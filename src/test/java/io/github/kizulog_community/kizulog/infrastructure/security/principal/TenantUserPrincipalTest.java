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

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantRole;

/**
 * TenantUserPrincipalの単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantUserPrincipalTest {

    private static final String TENANT_ID = "33333333-3333-3333-3333-333333333333";
    private static final String ACCOUNT_ID = "11111111-1111-1111-1111-111111111111";
    private static final String IDENTITY_ID = "22222222-2222-2222-2222-222222222222";
    private static final String ISS = "https://auth.dev.internal/realms/acme";
    private static final String AUD = "kizulog-acme";
    private static final String SUB = "user-sub-123";

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
                Set.of(new SimpleGrantedAuthority(TenantUserPrincipal.ROLE_TENANT_ADMIN));

        TenantUserPrincipal principal = new TenantUserPrincipal(
                TENANT_ID, ACCOUNT_ID, IDENTITY_ID, ISS, AUD, SUB,
                idToken, userInfo, authorities);

        assertThat(principal.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(principal.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(principal.getIdentityId()).isEqualTo(IDENTITY_ID);
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

        TenantUserPrincipal principal = new TenantUserPrincipal(
                TENANT_ID, ACCOUNT_ID, IDENTITY_ID, ISS, AUD, SUB, idToken, null, List.of());

        assertThat(principal.getUserInfo()).isNull();
    }

    @Test
    @DisplayName("コンストラクタ：tenantIdがnullならNullPointerException")
    void constructor_withNullTenantId_throwsNpe() {
        OidcIdToken idToken = sampleIdToken();

        assertThatNullPointerException().isThrownBy(() -> new TenantUserPrincipal(
                null, ACCOUNT_ID, IDENTITY_ID, ISS, AUD, SUB, idToken, null, List.of()))
                .withMessageContaining("tenantId");
    }

    @Test
    @DisplayName("コンストラクタ：accountIdがnullならNullPointerException")
    void constructor_withNullAccountId_throwsNpe() {
        OidcIdToken idToken = sampleIdToken();

        assertThatNullPointerException().isThrownBy(() -> new TenantUserPrincipal(
                TENANT_ID, null, IDENTITY_ID, ISS, AUD, SUB, idToken, null, List.of()))
                .withMessageContaining("accountId");
    }

    @Test
    @DisplayName("コンストラクタ：identityIdがnullならNullPointerException")
    void constructor_withNullIdentityId_throwsNpe() {
        OidcIdToken idToken = sampleIdToken();

        assertThatNullPointerException().isThrownBy(() -> new TenantUserPrincipal(
                TENANT_ID, ACCOUNT_ID, null, ISS, AUD, SUB, idToken, null, List.of()))
                .withMessageContaining("identityId");
    }

    @Test
    @DisplayName("コンストラクタ：issがnullならNullPointerException")
    void constructor_withNullIss_throwsNpe() {
        OidcIdToken idToken = sampleIdToken();

        assertThatNullPointerException().isThrownBy(() -> new TenantUserPrincipal(
                TENANT_ID, ACCOUNT_ID, IDENTITY_ID, null, AUD, SUB, idToken, null, List.of()))
                .withMessageContaining("iss");
    }

    @Test
    @DisplayName("コンストラクタ：audがnullならNullPointerException")
    void constructor_withNullAud_throwsNpe() {
        OidcIdToken idToken = sampleIdToken();

        assertThatNullPointerException().isThrownBy(() -> new TenantUserPrincipal(
                TENANT_ID, ACCOUNT_ID, IDENTITY_ID, ISS, null, SUB, idToken, null, List.of()))
                .withMessageContaining("aud");
    }

    @Test
    @DisplayName("コンストラクタ：subがnullならNullPointerException")
    void constructor_withNullSub_throwsNpe() {
        OidcIdToken idToken = sampleIdToken();

        assertThatNullPointerException().isThrownBy(() -> new TenantUserPrincipal(
                TENANT_ID, ACCOUNT_ID, IDENTITY_ID, ISS, AUD, null, idToken, null, List.of()))
                .withMessageContaining("sub");
    }

    @Test
    @DisplayName("コンストラクタ：idTokenがnullならNullPointerException")
    void constructor_withNullIdToken_throwsNpe() {
        assertThatNullPointerException().isThrownBy(() -> new TenantUserPrincipal(
                TENANT_ID, ACCOUNT_ID, IDENTITY_ID, ISS, AUD, SUB, null, null, List.of()))
                .withMessageContaining("idToken");
    }

    @Test
    @DisplayName("コンストラクタ：authoritiesがnullならNullPointerException")
    void constructor_withNullAuthorities_throwsNpe() {
        OidcIdToken idToken = sampleIdToken();

        assertThatNullPointerException().isThrownBy(() -> new TenantUserPrincipal(
                TENANT_ID, ACCOUNT_ID, IDENTITY_ID, ISS, AUD, SUB, idToken, null, null))
                .withMessageContaining("authorities");
    }

    @Test
    @DisplayName("getAuthorities：返されるCollectionは不変")
    void getAuthorities_returnsUnmodifiableCollection() {
        OidcIdToken idToken = sampleIdToken();
        TenantUserPrincipal principal = new TenantUserPrincipal(
                TENANT_ID, ACCOUNT_ID, IDENTITY_ID, ISS, AUD, SUB, idToken, null,
                List.of(new SimpleGrantedAuthority(TenantUserPrincipal.ROLE_TENANT_ADMIN)));

        Collection<GrantedAuthority> authorities =
                (Collection<GrantedAuthority>) principal.getAuthorities();

        assertThat(authorities).isUnmodifiable();
    }

    @Test
    @DisplayName("ofTenantUser：TENANT_ADMIN単独でROLE_TENANT_ADMINを生成する")
    void ofTenantUser_withTenantAdmin_buildsRoleTenantAdmin() {
        OidcIdToken idToken = sampleIdToken();

        TenantUserPrincipal principal = TenantUserPrincipal.ofTenantUser(
                TENANT_ID, ACCOUNT_ID, IDENTITY_ID, ISS, AUD, SUB, idToken,
                Set.of(TenantRole.TENANT_ADMIN));

        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(TenantUserPrincipal.ROLE_TENANT_ADMIN);
        assertThat(principal.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(principal.getUserInfo()).isNull();
    }

    @Test
    @DisplayName("ofTenantUser：EMPLOYEE単独でROLE_EMPLOYEEを生成する")
    void ofTenantUser_withEmployee_buildsRoleEmployee() {
        OidcIdToken idToken = sampleIdToken();

        TenantUserPrincipal principal = TenantUserPrincipal.ofTenantUser(
                TENANT_ID, ACCOUNT_ID, IDENTITY_ID, ISS, AUD, SUB, idToken,
                Set.of(TenantRole.EMPLOYEE));

        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(TenantUserPrincipal.ROLE_EMPLOYEE);
    }

    @Test
    @DisplayName("ofTenantUser：両ロールでROLE_TENANT_ADMINとROLE_EMPLOYEEを生成する")
    void ofTenantUser_withBothRoles_buildsBothAuthorities() {
        OidcIdToken idToken = sampleIdToken();

        TenantUserPrincipal principal = TenantUserPrincipal.ofTenantUser(
                TENANT_ID, ACCOUNT_ID, IDENTITY_ID, ISS, AUD, SUB, idToken,
                Set.of(TenantRole.TENANT_ADMIN, TenantRole.EMPLOYEE));

        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(
                        TenantUserPrincipal.ROLE_TENANT_ADMIN,
                        TenantUserPrincipal.ROLE_EMPLOYEE);
    }

    @Test
    @DisplayName("ofTenantUser：activeRolesがnullならNullPointerException")
    void ofTenantUser_withNullRoles_throwsNpe() {
        OidcIdToken idToken = sampleIdToken();

        assertThatNullPointerException().isThrownBy(() -> TenantUserPrincipal.ofTenantUser(
                TENANT_ID, ACCOUNT_ID, IDENTITY_ID, ISS, AUD, SUB, idToken, null))
                .withMessageContaining("activeRoles");
    }

    @Test
    @DisplayName("getClaims：ID Tokenのクレームを返す")
    void getClaims_returnsIdTokenClaims() {
        OidcIdToken idToken = sampleIdToken();
        TenantUserPrincipal principal = TenantUserPrincipal.ofTenantUser(
                TENANT_ID, ACCOUNT_ID, IDENTITY_ID, ISS, AUD, SUB, idToken,
                Set.of(TenantRole.EMPLOYEE));

        assertThat(principal.getClaims()).isEqualTo(idToken.getClaims());
    }

    @Test
    @DisplayName("getAttributes：ID Tokenのクレームを返す")
    void getAttributes_returnsIdTokenClaims() {
        OidcIdToken idToken = sampleIdToken();
        TenantUserPrincipal principal = TenantUserPrincipal.ofTenantUser(
                TENANT_ID, ACCOUNT_ID, IDENTITY_ID, ISS, AUD, SUB, idToken,
                Set.of(TenantRole.EMPLOYEE));

        assertThat(principal.getAttributes()).isEqualTo(idToken.getClaims());
    }

    @Test
    @DisplayName("getName：accountIdを返す")
    void getName_returnsAccountId() {
        OidcIdToken idToken = sampleIdToken();
        TenantUserPrincipal principal = TenantUserPrincipal.ofTenantUser(
                TENANT_ID, ACCOUNT_ID, IDENTITY_ID, ISS, AUD, SUB, idToken,
                Set.of(TenantRole.EMPLOYEE));

        assertThat(principal.getName()).isEqualTo(ACCOUNT_ID);
    }

    @Test
    @DisplayName("ロール定数：想定の値である")
    void roleConstants_haveExpectedValues() {
        assertThat(TenantUserPrincipal.ROLE_TENANT_ADMIN).isEqualTo("ROLE_TENANT_ADMIN");
        assertThat(TenantUserPrincipal.ROLE_EMPLOYEE).isEqualTo("ROLE_EMPLOYEE");
    }

}
