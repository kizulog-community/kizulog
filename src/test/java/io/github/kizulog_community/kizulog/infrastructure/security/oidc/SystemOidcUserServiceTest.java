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

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentity;
import io.github.kizulog_community.kizulog.domain.systemauth.exception.SystemAuthenticationErrorType;
import io.github.kizulog_community.kizulog.domain.systemauth.exception.SystemAuthenticationException;
import io.github.kizulog_community.kizulog.domain.systemauth.service.SystemAuthenticationService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;

/**
 * SystemOidcUserServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemOidcUserServiceTest {

    private static final String ISS = "https://auth.example/realms/master";
    private static final String AUD = "kizulog-master";
    private static final String SUB = "user-uuid-123";
    private static final String ACCOUNT_ID = "acc-1";
    private static final String IDENTITY_ID = "identity-1";
    private static final OffsetDateTime VERSION =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private SystemAuthenticationService authService;
    private OAuth2UserService<OidcUserRequest, OidcUser> delegate;
    private SystemOidcUserService sut;

    @BeforeEach
    void setUp() {
        authService = mock(SystemAuthenticationService.class);
        @SuppressWarnings("unchecked")
        OAuth2UserService<OidcUserRequest, OidcUser> mockDelegate =
                mock(OAuth2UserService.class);
        delegate = mockDelegate;
        sut = new SystemOidcUserService(authService, delegate);
    }

    /**
     * テスト用のOidcIdTokenを実体構築する。
     * iss/sub/audの3claimのみ設定。
     */
    private OidcIdToken buildIdToken() {
        return OidcIdToken.withTokenValue("fake-token-value")
                .issuer(ISS)
                .subject(SUB)
                .audience(List.of(AUD))
                .build();
    }

    /**
     * モック用のOidcUserRequestを生成する。
     * ClientRegistrationはclientId=AUDで構築する。
     */
    private OidcUserRequest buildUserRequest() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("master")
                .clientId(AUD)
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid")
                .authorizationUri("https://auth.example/oauth2/authorize")
                .tokenUri("https://auth.example/oauth2/token")
                .build();

        OidcUserRequest userRequest = mock(OidcUserRequest.class);
        when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
        return userRequest;
    }

    /**
     * モック用のOidcUserを生成する。
     * getIdToken()で実体構築したOidcIdTokenを返すよう設定。
     */
    private OidcUser buildOidcUser() {
        OidcUser oidcUser = mock(OidcUser.class);
        OidcIdToken idToken = buildIdToken();
        when(oidcUser.getIdToken()).thenReturn(idToken);
        return oidcUser;
    }

    private SystemAccountIdentity identity() {
        return new SystemAccountIdentity(
                IDENTITY_ID, VERSION, ACCOUNT_ID, ISS, AUD, SUB,
                VERSION, "system:setup");
    }

    @Test
    @DisplayName("loadUser: 認証成功時、SystemUserPrincipalが返る（accountId/identityId/iss/aud/sub）")
    void loadUser_succeeds_returnsSystemUserPrincipal() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(authService.authenticate(ISS, AUD, SUB)).thenReturn(identity());

        OidcUser result = sut.loadUser(userRequest);

        assertThat(result).isInstanceOf(SystemUserPrincipal.class);
        SystemUserPrincipal principal = (SystemUserPrincipal) result;
        assertThat(principal.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(principal.getIdentityId()).isEqualTo(IDENTITY_ID);
        assertThat(principal.getIss()).isEqualTo(ISS);
        assertThat(principal.getAud()).isEqualTo(AUD);
        assertThat(principal.getSub()).isEqualTo(SUB);
    }

    @Test
    @DisplayName("loadUser: 認証成功時、authoritiesにROLE_SYSTEM_ADMINが含まれる")
    void loadUser_succeeds_authoritiesContainsSystemAdminRole() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(authService.authenticate(ISS, AUD, SUB)).thenReturn(identity());

        OidcUser result = sut.loadUser(userRequest);

        assertThat(result.getAuthorities())
                .extracting("authority")
                .contains(SystemUserPrincipal.ROLE_SYSTEM_ADMIN);
    }

    @Test
    @DisplayName("loadUser: authServiceにiss/aud/subが正しく渡される")
    void loadUser_callsAuthServiceWithCorrectArguments() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(authService.authenticate(ISS, AUD, SUB)).thenReturn(identity());

        sut.loadUser(userRequest);

        verify(authService).authenticate(ISS, AUD, SUB);
    }

    @Test
    @DisplayName("loadUser: ACCOUNT_NOT_FOUND時、OAuth2AuthenticationExceptionに変換")
    void loadUser_throwsOAuth2AuthException_whenAccountNotFound() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(authService.authenticate(ISS, AUD, SUB))
                .thenThrow(new SystemAuthenticationException(
                        SystemAuthenticationErrorType.ACCOUNT_NOT_FOUND));

        assertThatThrownBy(() -> sut.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting("error.errorCode")
                .isEqualTo("ACCOUNT_NOT_FOUND");
    }

    @Test
    @DisplayName("loadUser: IDENTITY_INACTIVE時、OAuth2AuthenticationExceptionに変換")
    void loadUser_throwsOAuth2AuthException_whenIdentityInactive() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(authService.authenticate(ISS, AUD, SUB))
                .thenThrow(new SystemAuthenticationException(
                        SystemAuthenticationErrorType.IDENTITY_INACTIVE));

        assertThatThrownBy(() -> sut.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting("error.errorCode")
                .isEqualTo("IDENTITY_INACTIVE");
    }

    @Test
    @DisplayName("loadUser: ACCOUNT_INACTIVE時、OAuth2AuthenticationExceptionに変換")
    void loadUser_throwsOAuth2AuthException_whenAccountInactive() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(authService.authenticate(ISS, AUD, SUB))
                .thenThrow(new SystemAuthenticationException(
                        SystemAuthenticationErrorType.ACCOUNT_INACTIVE));

        assertThatThrownBy(() -> sut.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting("error.errorCode")
                .isEqualTo("ACCOUNT_INACTIVE");
    }

    @Test
    @DisplayName("loadUser: ROLE_NOT_GRANTED時、OAuth2AuthenticationExceptionに変換")
    void loadUser_throwsOAuth2AuthException_whenRoleNotGranted() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(authService.authenticate(ISS, AUD, SUB))
                .thenThrow(new SystemAuthenticationException(
                        SystemAuthenticationErrorType.ROLE_NOT_GRANTED));

        assertThatThrownBy(() -> sut.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting("error.errorCode")
                .isEqualTo("ROLE_NOT_GRANTED");
    }

    @Test
    @DisplayName("loadUser: delegate.loadUserがOAuth2AuthenticationExceptionを投げた場合、そのまま伝播")
    void loadUser_propagatesDelegateException() {
        OidcUserRequest userRequest = buildUserRequest();
        OAuth2AuthenticationException delegateException =
                new OAuth2AuthenticationException("invalid_token");
        when(delegate.loadUser(any())).thenThrow(delegateException);

        assertThatThrownBy(() -> sut.loadUser(userRequest))
                .isSameAs(delegateException);
    }

}
