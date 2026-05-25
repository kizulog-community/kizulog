package io.github.kizulog_community.kizulog.infrastructure.security.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountIdentity;
import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantRole;
import io.github.kizulog_community.kizulog.domain.tenantauth.exception.TenantAuthenticationErrorType;
import io.github.kizulog_community.kizulog.domain.tenantauth.exception.TenantAuthenticationException;
import io.github.kizulog_community.kizulog.domain.tenantauth.model.TenantAuthenticationResult;
import io.github.kizulog_community.kizulog.domain.tenantauth.service.TenantAuthenticationService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.TenantUserPrincipal;

/**
 * TenantOidcUserServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantOidcUserServiceTest {

    private static final String TENANT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String PROVIDER_ID = "keycloak";
    private static final String REG_ID = "tenant-" + TENANT_ID + "-" + PROVIDER_ID;
    private static final String ISS = "https://auth.acme.example/realms/acme";
    private static final String AUD = "kizulog-acme";
    private static final String SUB = "user-uuid-123";
    private static final String ACCOUNT_ID = "acc-1";
    private static final String IDENTITY_ID = "identity-1";
    private static final OffsetDateTime VERSION =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private TenantAuthenticationService authService;
    private OAuth2UserService<OidcUserRequest, OidcUser> delegate;
    private TenantOidcUserService sut;

    @BeforeEach
    void setUp() {
        authService = mock(TenantAuthenticationService.class);
        @SuppressWarnings("unchecked")
        OAuth2UserService<OidcUserRequest, OidcUser> mockDelegate =
                mock(OAuth2UserService.class);
        delegate = mockDelegate;
        sut = new TenantOidcUserService(authService, delegate);
    }

    private OidcIdToken buildIdToken() {
        return OidcIdToken.withTokenValue("fake-token-value")
                .issuer(ISS)
                .subject(SUB)
                .audience(List.of(AUD))
                .build();
    }

    private OidcUserRequest buildUserRequest(String registrationId) {
        ClientRegistration clientRegistration =
                ClientRegistration.withRegistrationId(registrationId)
                        .clientId(AUD)
                        .clientSecret("secret")
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                        .scope("openid")
                        .authorizationUri("https://auth.acme.example/oauth2/authorize")
                        .tokenUri("https://auth.acme.example/oauth2/token")
                        .build();

        OidcUserRequest userRequest = mock(OidcUserRequest.class);
        when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
        return userRequest;
    }

    private OidcUser buildOidcUser() {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getIdToken()).thenReturn(buildIdToken());
        return oidcUser;
    }

    private TenantAccountIdentity identity() {
        return new TenantAccountIdentity(
                IDENTITY_ID, VERSION, ACCOUNT_ID, TENANT_ID, ISS, AUD, SUB,
                VERSION, "tenant:invite:x");
    }

    private TenantAuthenticationResult resultWith(Set<TenantRole> roles) {
        return new TenantAuthenticationResult(identity(), roles);
    }

    @Test
    @DisplayName("loadUser: 認証成功時、TenantUserPrincipalが返る（tenantId/accountId/identityId/iss/aud/sub）")
    void loadUser_succeeds_returnsTenantUserPrincipal() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest(REG_ID);
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(authService.authenticate(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(resultWith(Set.of(TenantRole.TENANT_ADMIN)));

        OidcUser result = sut.loadUser(userRequest);

        assertThat(result).isInstanceOf(TenantUserPrincipal.class);
        TenantUserPrincipal principal = (TenantUserPrincipal) result;
        assertThat(principal.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(principal.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(principal.getIdentityId()).isEqualTo(IDENTITY_ID);
        assertThat(principal.getIss()).isEqualTo(ISS);
        assertThat(principal.getAud()).isEqualTo(AUD);
        assertThat(principal.getSub()).isEqualTo(SUB);
    }

    @Test
    @DisplayName("loadUser: TENANT_ADMINロールでauthoritiesにROLE_TENANT_ADMINが含まれる")
    void loadUser_succeeds_authoritiesContainsTenantAdminRole() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest(REG_ID);
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(authService.authenticate(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(resultWith(Set.of(TenantRole.TENANT_ADMIN)));

        OidcUser result = sut.loadUser(userRequest);

        assertThat(result.getAuthorities())
                .extracting("authority")
                .contains(TenantUserPrincipal.ROLE_TENANT_ADMIN);
    }

    @Test
    @DisplayName("loadUser: 両ロールでauthoritiesにROLE_TENANT_ADMINとROLE_EMPLOYEEが含まれる")
    void loadUser_succeeds_authoritiesContainsBothRoles() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest(REG_ID);
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(authService.authenticate(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(resultWith(Set.of(TenantRole.TENANT_ADMIN, TenantRole.EMPLOYEE)));

        OidcUser result = sut.loadUser(userRequest);

        assertThat(result.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder(
                        TenantUserPrincipal.ROLE_TENANT_ADMIN,
                        TenantUserPrincipal.ROLE_EMPLOYEE);
    }

    @Test
    @DisplayName("loadUser: authServiceにregistrationIdから抽出したtenantIdとiss/aud/subが渡される")
    void loadUser_callsAuthServiceWithParsedTenantId() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest(REG_ID);
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(authService.authenticate(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(resultWith(Set.of(TenantRole.EMPLOYEE)));

        sut.loadUser(userRequest);

        verify(authService).authenticate(TENANT_ID, ISS, AUD, SUB);
    }

    @Test
    @DisplayName("loadUser: registrationIdが不正形式の場合、OAuth2AuthenticationExceptionで認証失敗")
    void loadUser_throwsException_whenInvalidRegistrationId() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest("invalid-format");
        when(delegate.loadUser(any())).thenReturn(oidcUser);

        assertThatThrownBy(() -> sut.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    @DisplayName("loadUser: ドメイン認証失敗時、errorType付きOAuth2AuthenticationExceptionに変換される")
    void loadUser_convertsAuthExceptionToOAuth2() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest(REG_ID);
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(authService.authenticate(TENANT_ID, ISS, AUD, SUB))
                .thenThrow(new TenantAuthenticationException(
                        TenantAuthenticationErrorType.ROLE_NOT_GRANTED));

        assertThatThrownBy(() -> sut.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
                .isEqualTo(TenantAuthenticationErrorType.ROLE_NOT_GRANTED.name());
    }

    @Test
    @DisplayName("loadUser: ACCOUNT_NOT_FOUNDも正しくerrorCodeに反映される")
    void loadUser_convertsAccountNotFound() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest(REG_ID);
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(authService.authenticate(TENANT_ID, ISS, AUD, SUB))
                .thenThrow(new TenantAuthenticationException(
                        TenantAuthenticationErrorType.ACCOUNT_NOT_FOUND));

        assertThatThrownBy(() -> sut.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
                .isEqualTo(TenantAuthenticationErrorType.ACCOUNT_NOT_FOUND.name());
    }

    @Test
    @DisplayName("loadUser: delegateが例外を投げた場合はそのまま伝播する")
    void loadUser_propagatesDelegateException() {
        OidcUserRequest userRequest = buildUserRequest(REG_ID);
        when(delegate.loadUser(any()))
                .thenThrow(new OAuth2AuthenticationException("delegate failed"));

        assertThatThrownBy(() -> sut.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

}
