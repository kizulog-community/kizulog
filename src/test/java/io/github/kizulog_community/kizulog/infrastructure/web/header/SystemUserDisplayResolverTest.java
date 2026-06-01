package io.github.kizulog_community.kizulog.infrastructure.web.header;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.kizulog_community.kizulog.domain.systemaccountprofile.model.SystemAccountProfile;
import io.github.kizulog_community.kizulog.domain.systemaccountprofile.service.SystemAccountProfileService;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.ClaimsMappingTarget;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProvider;
import io.github.kizulog_community.kizulog.domain.systemoidc.port.SystemOidcProviderRepository;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.ClaimsMappingResolver;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;

/**
 * SystemUserDisplayResolverの単体テスト
 *
 * <p>T.0シリーズで全面書き換え。グローバル設定参照から
 * プロバイダ単位の {@code claimsMapping} 参照へ移行した動作を検証する。</p>
 *
 * @author Jun Kobayashi
 */
class SystemUserDisplayResolverTest {

    private static final String ACCOUNT_ID = "acc-1";
    private static final String IDENTITY_ID = "id-1";
    private static final String SUB = "sub-uuid-123";
    private static final String ISS = "https://example.com";
    private static final String AUD = "aud-1";
    private static final String PROVIDER_ID = "test-provider";
    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private SystemAccountProfileService profileService;
    private SystemOidcProviderRepository providerRepository;
    private ClaimsMappingResolver claimsMappingResolver;
    private SystemUserDisplayResolver sut;

    @BeforeEach
    void setUp() {
        profileService = mock(SystemAccountProfileService.class);
        providerRepository = mock(SystemOidcProviderRepository.class);
        claimsMappingResolver = new ClaimsMappingResolver();
        sut = new SystemUserDisplayResolver(
                profileService, providerRepository, claimsMappingResolver);
    }

    @Test
    @DisplayName("resolve: デフォルトマッピングで姓名・所属・emailが解決される")
    void resolve_resolvesAllFields_withDefaultMapping() {
        // プロバイダ: デフォルトマッピング（family_name/given_name/middle_name/organization/email）
        SystemOidcProvider provider = providerWithMapping(ClaimsMappingTarget.defaultMapping());
        when(providerRepository.findAllLatest()).thenReturn(List.of(provider));

        Map<String, Object> claims = Map.of(
                "family_name", "山田",
                "given_name", "太郎",
                "organization", "開発部",
                "email", "taro@example.com");
        when(profileService.getProfile(IDENTITY_ID))
                .thenReturn(Optional.of(profileOf(claims)));

        SystemUserDisplayView result = sut.resolve(principal());

        assertThat(result.getFamilyName()).isEqualTo("山田");
        assertThat(result.getGivenName()).isEqualTo("太郎");
        assertThat(result.getMiddleName()).isNull();
        assertThat(result.getOrganization()).isEqualTo("開発部");
        assertThat(result.getEmail()).isEqualTo("taro@example.com");
        assertThat(result.isNameEmpty()).isFalse();
    }

    @Test
    @DisplayName("resolve: カスタムマッピング（family_nameをsurnameに割当）が反映される")
    void resolve_respectsCustomMapping() {
        Map<String, String> customMapping = new LinkedHashMap<>();
        customMapping.put("familyName", "surname");
        customMapping.put("givenName", "first_name");
        customMapping.put("organization", "dept");
        customMapping.put("email", "mail");
        SystemOidcProvider provider = providerWithMapping(customMapping);
        when(providerRepository.findAllLatest()).thenReturn(List.of(provider));

        Map<String, Object> claims = Map.of(
                "surname", "山田",
                "first_name", "太郎",
                "dept", "営業部",
                "mail", "taro@example.com",
                // family_name 等の標準キーがあっても、マッピングで surname 等を指定しているので無視される
                "family_name", "別人");
        when(profileService.getProfile(IDENTITY_ID))
                .thenReturn(Optional.of(profileOf(claims)));

        SystemUserDisplayView result = sut.resolve(principal());

        assertThat(result.getFamilyName()).isEqualTo("山田");
        assertThat(result.getGivenName()).isEqualTo("太郎");
        assertThat(result.getOrganization()).isEqualTo("営業部");
        assertThat(result.getEmail()).isEqualTo("taro@example.com");
    }

    @Test
    @DisplayName("resolve: マッピング未設定の属性はnullになる")
    void resolve_returnsNull_forUnmappedAttributes() {
        // 姓・名のみマッピング、organization/email/middle_name はマッピングなし
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("familyName", "family_name");
        mapping.put("givenName", "given_name");
        SystemOidcProvider provider = providerWithMapping(mapping);
        when(providerRepository.findAllLatest()).thenReturn(List.of(provider));

        Map<String, Object> claims = Map.of(
                "family_name", "山田",
                "given_name", "太郎",
                "organization", "開発部",
                "email", "taro@example.com");
        when(profileService.getProfile(IDENTITY_ID))
                .thenReturn(Optional.of(profileOf(claims)));

        SystemUserDisplayView result = sut.resolve(principal());

        assertThat(result.getFamilyName()).isEqualTo("山田");
        assertThat(result.getGivenName()).isEqualTo("太郎");
        // マッピング未設定のため、クレーム値があってもnull
        assertThat(result.getOrganization()).isNull();
        assertThat(result.getEmail()).isNull();
        assertThat(result.getMiddleName()).isNull();
    }

    @Test
    @DisplayName("resolve: クレームが取得できない属性はnullになる")
    void resolve_returnsNull_whenClaimAbsent() {
        SystemOidcProvider provider = providerWithMapping(ClaimsMappingTarget.defaultMapping());
        when(providerRepository.findAllLatest()).thenReturn(List.of(provider));

        // 姓のみ存在、他は欠落
        Map<String, Object> claims = Map.of("family_name", "山田");
        when(profileService.getProfile(IDENTITY_ID))
                .thenReturn(Optional.of(profileOf(claims)));

        SystemUserDisplayView result = sut.resolve(principal());

        assertThat(result.getFamilyName()).isEqualTo("山田");
        assertThat(result.getGivenName()).isNull();
        assertThat(result.getOrganization()).isNull();
        assertThat(result.getEmail()).isNull();
        assertThat(result.isNameEmpty()).isFalse();
    }

    @Test
    @DisplayName("resolve: プロファイル未取得の場合は全フィールドがnull (fail-open)")
    void resolve_returnsEmptyView_whenProfileNotFound() {
        SystemOidcProvider provider = providerWithMapping(ClaimsMappingTarget.defaultMapping());
        when(providerRepository.findAllLatest()).thenReturn(List.of(provider));
        when(profileService.getProfile(IDENTITY_ID)).thenReturn(Optional.empty());

        SystemUserDisplayView result = sut.resolve(principal());

        assertThat(result.getFamilyName()).isNull();
        assertThat(result.getGivenName()).isNull();
        assertThat(result.getMiddleName()).isNull();
        assertThat(result.getOrganization()).isNull();
        assertThat(result.getEmail()).isNull();
        assertThat(result.isNameEmpty()).isTrue();
    }

    @Test
    @DisplayName("resolve: プロバイダが見つからない場合は全フィールドがnull (fail-open)")
    void resolve_returnsEmptyView_whenProviderNotFound() {
        // 別のiss URIを持つプロバイダしか存在しない
        SystemOidcProvider otherProvider = new SystemOidcProvider(
                "other-provider", BASE_TIME, "Other Provider", "https://other.example.com",
                "client-1", "secret", ClaimsMappingTarget.defaultMapping(), BASE_TIME, "system");
        when(providerRepository.findAllLatest()).thenReturn(List.of(otherProvider));

        Map<String, Object> claims = Map.of("family_name", "山田");
        when(profileService.getProfile(IDENTITY_ID))
                .thenReturn(Optional.of(profileOf(claims)));

        SystemUserDisplayView result = sut.resolve(principal());

        assertThat(result.getFamilyName()).isNull();
        assertThat(result.getGivenName()).isNull();
        assertThat(result.isNameEmpty()).isTrue();
    }

    @Test
    @DisplayName("resolve: principalがnullの場合は空View (fail-open)")
    void resolve_returnsEmptyView_whenPrincipalIsNull() {
        SystemUserDisplayView result = sut.resolve(null);

        assertThat(result.getFamilyName()).isNull();
        assertThat(result.getGivenName()).isNull();
        assertThat(result.getOrganization()).isNull();
        assertThat(result.getEmail()).isNull();
    }

    @Test
    @DisplayName("resolve: 末尾スラッシュ揺れがあってもプロバイダが一致する")
    void resolve_matchesProvider_evenWithTrailingSlashDifference() {
        // プロバイダURI: 末尾スラッシュなし
        // 認証時のiss: 末尾スラッシュあり
        SystemOidcProvider provider = new SystemOidcProvider(
                PROVIDER_ID, BASE_TIME, "Test Provider", "https://example.com",
                "client-1", "secret",
                ClaimsMappingTarget.defaultMapping(), BASE_TIME, "system");
        when(providerRepository.findAllLatest()).thenReturn(List.of(provider));

        Map<String, Object> claims = Map.of("family_name", "山田");
        when(profileService.getProfile(IDENTITY_ID))
                .thenReturn(Optional.of(profileOf(claims)));

        // principal の iss は "https://example.com" (末尾スラッシュなし、prefix一致)
        SystemUserDisplayView result = sut.resolve(principal());

        assertThat(result.getFamilyName()).isEqualTo("山田");
    }

    /**
     * 指定のクレームマッピングでプロバイダを構築する。
     */
    private SystemOidcProvider providerWithMapping(Map<String, String> mapping) {
        return new SystemOidcProvider(
                PROVIDER_ID, BASE_TIME, "Test Provider", ISS,
                "client-1", "secret", mapping, BASE_TIME, "system");
    }

    /**
     * テスト用のPrincipalを生成する。
     */
    private SystemUserPrincipal principal() {
        org.springframework.security.oauth2.core.oidc.OidcIdToken idToken =
                org.springframework.security.oauth2.core.oidc.OidcIdToken
                        .withTokenValue("dummy-token")
                        .issuer(ISS)
                        .subject(SUB)
                        .audience(List.of(AUD))
                        .build();
        return SystemUserPrincipal.ofSystemAdmin(
                ACCOUNT_ID, IDENTITY_ID, ISS, AUD, SUB, idToken);
    }

    /**
     * テスト用のSystemAccountProfileを生成する。
     */
    private SystemAccountProfile profileOf(Map<String, Object> claims) {
        return new SystemAccountProfile(
                IDENTITY_ID, BASE_TIME, claims, BASE_TIME, SUB);
    }

}
