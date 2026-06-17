package io.github.kizulog_community.kizulog.infrastructure.web.tenant.header;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantRole;
import io.github.kizulog_community.kizulog.domain.tenantaccountprofile.model.TenantAccountProfile;
import io.github.kizulog_community.kizulog.domain.tenantaccountprofile.service.TenantAccountProfileService;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProvider;
import io.github.kizulog_community.kizulog.domain.tenantoidc.port.TenantOidcProviderRepository;
import io.github.kizulog_community.kizulog.domain.tenantoidc.service.ClaimsMappingResolver;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.TenantUserPrincipal;

/**
 * TenantUserDisplayResolverの単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantUserDisplayResolverTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);
    private static final String TENANT_ID = "tenant-A";
    private static final String OTHER_TENANT_ID = "tenant-B";
    private static final String IDENTITY_ID = "id-1";
    private static final String ISS = "https://auth.example/realms/main";
    private static final String AUD = "client-aud";
    private static final String SUB = "sub-1";

    private TenantAccountProfileService profileService;
    private TenantOidcProviderRepository providerRepository;
    private TenantUserDisplayResolver resolver;

    @BeforeEach
    void setUp() {
        profileService = mock(TenantAccountProfileService.class);
        providerRepository = mock(TenantOidcProviderRepository.class);
        // クレームマッピング解決は純ロジックのため実体を使用
        resolver = new TenantUserDisplayResolver(
                profileService, providerRepository, new ClaimsMappingResolver());
    }

    private TenantUserPrincipal principalOf(
            String tenantId, String identityId, String iss, String aud, String sub) {
        OidcIdToken idToken = new OidcIdToken(
                "token-value",
                Instant.parse("2026-05-01T12:00:00Z"),
                Instant.parse("2026-05-01T13:00:00Z"),
                Map.of("sub", sub));
        return TenantUserPrincipal.ofTenantUser(
                tenantId, "account-1", identityId, iss, aud, sub,
                idToken, Set.of(TenantRole.EMPLOYEE));
    }

    /**
     * familyName/givenName/organization/email にマッピングされたプロバイダと、
     * 対応するクレームを持つプロファイルから、表示Viewが正しく組み立てられる。
     */
    private TenantOidcProvider providerWithMapping(String tenantId, Map<String, String> mapping) {
        return new TenantOidcProvider(
                tenantId, "google", BASE_TIME,
                "Display", ISS, AUD, "client", "enc",
                mapping, BASE_TIME, "creator");
    }

    private TenantAccountProfile profileWithClaims(Map<String, Object> claims) {
        return new TenantAccountProfile(IDENTITY_ID, BASE_TIME, claims, BASE_TIME, SUB);
    }

    @Test
    @DisplayName("resolve: プロファイル・プロバイダーが揃う場合、マッピング適用済みのViewを返す")
    void resolve_returnsMappedView_whenProfileAndProviderPresent() {
        Map<String, String> mapping = Map.of(
                "familyName", "fn",
                "givenName", "gn",
                "organization", "org",
                "email", "em");
        when(profileService.getProfile(IDENTITY_ID)).thenReturn(
                Optional.of(profileWithClaims(Map.of(
                        "fn", "山田",
                        "gn", "太郎",
                        "org", "開発部",
                        "em", "taro@example.com"))));
        when(providerRepository.findAllLatestByIssAndAud(ISS, AUD))
                .thenReturn(List.of(providerWithMapping(TENANT_ID, mapping)));

        TenantUserDisplayView view = resolver.resolve(
                principalOf(TENANT_ID, IDENTITY_ID, ISS, AUD, SUB));

        assertThat(view.getFamilyName()).isEqualTo("山田");
        assertThat(view.getGivenName()).isEqualTo("太郎");
        assertThat(view.getMiddleName()).isNull();
        assertThat(view.getOrganization()).isEqualTo("開発部");
        assertThat(view.getEmail()).isEqualTo("taro@example.com");
        assertThat(view.isNameEmpty()).isFalse();
    }

    @Test
    @DisplayName("resolve: principalがnullの場合、空のViewを返す")
    void resolve_returnsEmptyView_whenPrincipalNull() {
        TenantUserDisplayView view = resolver.resolve(null);

        assertThat(view.getFamilyName()).isNull();
        assertThat(view.getGivenName()).isNull();
        assertThat(view.getOrganization()).isNull();
        assertThat(view.getEmail()).isNull();
        assertThat(view.isNameEmpty()).isTrue();
    }

    @Test
    @DisplayName("resolve: (iss,aud)一致でもtenantIdが異なる場合、空のViewを返す")
    void resolve_returnsEmptyView_whenProviderTenantMismatch() {
        when(profileService.getProfile(IDENTITY_ID)).thenReturn(
                Optional.of(profileWithClaims(Map.of("fn", "山田"))));
        // (iss,aud)では一致するが、別テナントのプロバイダーのみ存在
        when(providerRepository.findAllLatestByIssAndAud(ISS, AUD))
                .thenReturn(List.of(providerWithMapping(
                        OTHER_TENANT_ID, Map.of("familyName", "fn"))));

        TenantUserDisplayView view = resolver.resolve(
                principalOf(TENANT_ID, IDENTITY_ID, ISS, AUD, SUB));

        assertThat(view.isNameEmpty()).isTrue();
        assertThat(view.getOrganization()).isNull();
        assertThat(view.getEmail()).isNull();
    }

    @Test
    @DisplayName("resolve: プロファイル未キャッシュの場合、クレーム空として全項目nullのViewを返す")
    void resolve_allNull_whenProfileAbsent() {
        when(profileService.getProfile(IDENTITY_ID)).thenReturn(Optional.empty());
        when(providerRepository.findAllLatestByIssAndAud(ISS, AUD))
                .thenReturn(List.of(providerWithMapping(
                        TENANT_ID, Map.of("familyName", "fn", "email", "em"))));

        TenantUserDisplayView view = resolver.resolve(
                principalOf(TENANT_ID, IDENTITY_ID, ISS, AUD, SUB));

        assertThat(view.isNameEmpty()).isTrue();
        assertThat(view.getEmail()).isNull();
    }

    @Test
    @DisplayName("resolve: プロバイダー取得が例外でも、例外を伝播せず空のViewを返す（fail-open）")
    void resolve_failOpen_whenProviderRepositoryThrows() {
        when(profileService.getProfile(IDENTITY_ID)).thenReturn(
                Optional.of(profileWithClaims(Map.of("fn", "山田"))));
        when(providerRepository.findAllLatestByIssAndAud(ISS, AUD))
                .thenThrow(new RuntimeException("db error"));

        TenantUserDisplayView view = resolver.resolve(
                principalOf(TENANT_ID, IDENTITY_ID, ISS, AUD, SUB));

        assertThat(view.isNameEmpty()).isTrue();
        assertThat(view.getEmail()).isNull();
    }

}
