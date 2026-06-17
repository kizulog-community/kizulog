package io.github.kizulog_community.kizulog.infrastructure.web.header;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.kizulog_community.kizulog.domain.systemaccountprofile.model.SystemAccountProfile;
import io.github.kizulog_community.kizulog.domain.systemaccountprofile.service.SystemAccountProfileService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;

/**
 * SystemUserDisplayResolverの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemUserDisplayResolverTest {

    private static final String ACCOUNT_ID = "acc-1";
    private static final String IDENTITY_ID = "id-1";
    private static final String SUB = "sub-uuid-123";
    private static final String ISS = "https://example.com";
    private static final String AUD = "aud-1";
    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private SystemAccountProfileService profileService;
    private SystemUserDisplayResolver sut;

    @BeforeEach
    void setUp() {
        profileService = mock(SystemAccountProfileService.class);
        sut = new SystemUserDisplayResolver(profileService);
    }

    @Test
    @DisplayName("resolve: 解決済みプロファイル（ターゲットキー）から全項目を読み出す")
    void resolve_readsAllResolvedFields() {
        Map<String, Object> resolved = Map.of(
                "familyName", "山田",
                "givenName", "太郎",
                "organization", "開発部",
                "email", "taro@example.com");
        when(profileService.getProfile(IDENTITY_ID))
                .thenReturn(Optional.of(profileOf(resolved)));

        SystemUserDisplayView result = sut.resolve(principal());

        assertThat(result.getFamilyName()).isEqualTo("山田");
        assertThat(result.getGivenName()).isEqualTo("太郎");
        assertThat(result.getMiddleName()).isNull();
        assertThat(result.getOrganization()).isEqualTo("開発部");
        assertThat(result.getEmail()).isEqualTo("taro@example.com");
        assertThat(result.isNameEmpty()).isFalse();
    }

    @Test
    @DisplayName("resolve: 保存値に無い項目はnull")
    void resolve_returnsNull_forAbsentKeys() {
        when(profileService.getProfile(IDENTITY_ID))
                .thenReturn(Optional.of(profileOf(Map.of("familyName", "山田"))));

        SystemUserDisplayView result = sut.resolve(principal());

        assertThat(result.getFamilyName()).isEqualTo("山田");
        assertThat(result.getGivenName()).isNull();
        assertThat(result.getOrganization()).isNull();
        assertThat(result.getEmail()).isNull();
        assertThat(result.getMiddleName()).isNull();
        assertThat(result.isNameEmpty()).isFalse();
    }

    @Test
    @DisplayName("resolve: プロファイル未取得の場合は全フィールドがnull (fail-open)")
    void resolve_returnsEmptyView_whenProfileNotFound() {
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
    @DisplayName("resolve: プロファイル取得が例外でも伝播せず空View (fail-open)")
    void resolve_failOpen_whenProfileServiceThrows() {
        when(profileService.getProfile(IDENTITY_ID))
                .thenThrow(new RuntimeException("db error"));

        SystemUserDisplayView result = sut.resolve(principal());

        assertThat(result.isNameEmpty()).isTrue();
        assertThat(result.getEmail()).isNull();
    }

    @Test
    @DisplayName("resolve: principalがnullの場合は空View (fail-open)")
    void resolve_returnsEmptyView_whenPrincipalIsNull() {
        SystemUserDisplayView result = sut.resolve(null);

        assertThat(result.getFamilyName()).isNull();
        assertThat(result.getGivenName()).isNull();
        assertThat(result.getOrganization()).isNull();
        assertThat(result.getEmail()).isNull();
        assertThat(result.isNameEmpty()).isTrue();
    }

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

    private SystemAccountProfile profileOf(Map<String, Object> claims) {
        return new SystemAccountProfile(
                IDENTITY_ID, BASE_TIME, claims, BASE_TIME, SUB);
    }

}
