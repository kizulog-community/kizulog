package io.github.kizulog_community.kizulog.infrastructure.security.principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

/**
 * SystemUserPrincipalの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemUserPrincipalTest {

    private static final String ACCOUNT_ID = "11111111-1111-1111-1111-111111111111";
    private static final String ISS = "https://auth.dev.internal/realms/master";
    private static final String AUD = "kizulog-master";
    private static final String SUB = "user-sub-123";

    /**
     * テスト用のOidcIdTokenを生成する
     */
    private OidcIdToken sampleIdToken() {
        return new OidcIdToken(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of(
                        IdTokenClaimNames.ISS, ISS,
                        IdTokenClaimNames.AUD, List.of(AUD),
                        IdTokenClaimNames.SUB, SUB));
    }

    @Test
    @DisplayName("コンストラクタ：全フィールド指定で各getterが値を返す")
    void constructor_withAllFields_returnsAllValues() {
        OidcIdToken idToken = sampleIdToken();
        OidcUserInfo userInfo = new OidcUserInfo(Map.of("sub", SUB));
        Set<GrantedAuthority> authorities =
                Set.of(new SimpleGrantedAuthority(SystemUserPrincipal.ROLE_SYSTEM_ADMIN));

        SystemUserPrincipal principal = new SystemUserPrincipal(
                ACCOUNT_ID, ISS, AUD, SUB, idToken, userInfo, authorities);

        assertThat(principal.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(principal.getIss()).isEqualTo(ISS);
        assertThat(principal.getAud()).isEqualTo(AUD);
        assertThat(principal.getSub()).isEqualTo(SUB);
        assertThat(principal.getIdToken()).isSameAs(idToken);
        assertThat(principal.getUserInfo()).isSameAs(userInfo);
        assertThat(principal.getAuthorities()).containsExactlyElementsOf(authorities);
    }

    @Test
    @DisplayName("コンストラクタ：userInfoがnullの場合も生成できる")
    void constructor_withNullUserInfo_isAllowed() {
        OidcIdToken idToken = sampleIdToken();

        SystemUserPrincipal principal = new SystemUserPrincipal(
                ACCOUNT_ID, ISS, AUD, SUB, idToken, null, List.of());

        assertThat(principal.getUserInfo()).isNull();
    }

    @Test
    @DisplayName("コンストラクタ：accountIdがnullならNullPointerException")
    void constructor_withNullAccountId_throwsNpe() {
        OidcIdToken idToken = sampleIdToken();

        assertThatNullPointerException().isThrownBy(() -> new SystemUserPrincipal(
                null, ISS, AUD, SUB, idToken, null, List.of()))
                .withMessageContaining("accountId");
    }

    @Test
    @DisplayName("コンストラクタ：issがnullならNullPointerException")
    void constructor_withNullIss_throwsNpe() {
        OidcIdToken idToken = sampleIdToken();

        assertThatNullPointerException().isThrownBy(() -> new SystemUserPrincipal(
                ACCOUNT_ID, null, AUD, SUB, idToken, null, List.of()))
                .withMessageContaining("iss");
    }

    @Test
    @DisplayName("コンストラクタ：audがnullならNullPointerException")
    void constructor_withNullAud_throwsNpe() {
        OidcIdToken idToken = sampleIdToken();

        assertThatNullPointerException().isThrownBy(() -> new SystemUserPrincipal(
                ACCOUNT_ID, ISS, null, SUB, idToken, null, List.of()))
                .withMessageContaining("aud");
    }

    @Test
    @DisplayName("コンストラクタ：subがnullならNullPointerException")
    void constructor_withNullSub_throwsNpe() {
        OidcIdToken idToken = sampleIdToken();

        assertThatNullPointerException().isThrownBy(() -> new SystemUserPrincipal(
                ACCOUNT_ID, ISS, AUD, null, idToken, null, List.of()))
                .withMessageContaining("sub");
    }

    @Test
    @DisplayName("コンストラクタ：idTokenがnullならNullPointerException")
    void constructor_withNullIdToken_throwsNpe() {
        assertThatNullPointerException().isThrownBy(() -> new SystemUserPrincipal(
                ACCOUNT_ID, ISS, AUD, SUB, null, null, List.of()))
                .withMessageContaining("idToken");
    }

    @Test
    @DisplayName("コンストラクタ：authoritiesがnullならNullPointerException")
    void constructor_withNullAuthorities_throwsNpe() {
        OidcIdToken idToken = sampleIdToken();

        assertThatNullPointerException().isThrownBy(() -> new SystemUserPrincipal(
                ACCOUNT_ID, ISS, AUD, SUB, idToken, null, null))
                .withMessageContaining("authorities");
    }

    @Test
    @DisplayName("getAuthorities：返されるCollectionは不変")
    void getAuthorities_returnsUnmodifiableCollection() {
        OidcIdToken idToken = sampleIdToken();
        SystemUserPrincipal principal = new SystemUserPrincipal(
                ACCOUNT_ID, ISS, AUD, SUB, idToken, null,
                List.of(new SimpleGrantedAuthority(SystemUserPrincipal.ROLE_SYSTEM_ADMIN)));

        Collection<GrantedAuthority> authorities =
                (Collection<GrantedAuthority>) principal.getAuthorities();

        assertThat(authorities).isUnmodifiable();
    }

    @Test
    @DisplayName("ofSystemAdmin：SYSTEM_ADMINロール付きPrincipalを生成する")
    void ofSystemAdmin_buildsPrincipalWithSystemAdminRole() {
        OidcIdToken idToken = sampleIdToken();

        SystemUserPrincipal principal = SystemUserPrincipal.ofSystemAdmin(
                ACCOUNT_ID, ISS, AUD, SUB, idToken);

        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(SystemUserPrincipal.ROLE_SYSTEM_ADMIN);
        assertThat(principal.getUserInfo()).isNull();
    }

    @Test
    @DisplayName("getClaims：ID Tokenのクレームを返す")
    void getClaims_returnsIdTokenClaims() {
        OidcIdToken idToken = sampleIdToken();
        SystemUserPrincipal principal = SystemUserPrincipal.ofSystemAdmin(
                ACCOUNT_ID, ISS, AUD, SUB, idToken);

        assertThat(principal.getClaims()).isEqualTo(idToken.getClaims());
    }

    @Test
    @DisplayName("getAttributes：ID Tokenのクレームを返す")
    void getAttributes_returnsIdTokenClaims() {
        OidcIdToken idToken = sampleIdToken();
        SystemUserPrincipal principal = SystemUserPrincipal.ofSystemAdmin(
                ACCOUNT_ID, ISS, AUD, SUB, idToken);

        assertThat(principal.getAttributes()).isEqualTo(idToken.getClaims());
    }

    @Test
    @DisplayName("getName：accountIdを返す")
    void getName_returnsAccountId() {
        OidcIdToken idToken = sampleIdToken();
        SystemUserPrincipal principal = SystemUserPrincipal.ofSystemAdmin(
                ACCOUNT_ID, ISS, AUD, SUB, idToken);

        assertThat(principal.getName()).isEqualTo(ACCOUNT_ID);
    }

    @Test
    @DisplayName("ROLE_SYSTEM_ADMIN定数：想定の値である")
    void roleSystemAdmin_hasExpectedValue() {
        assertThat(SystemUserPrincipal.ROLE_SYSTEM_ADMIN).isEqualTo("ROLE_SYSTEM_ADMIN");
    }

}
