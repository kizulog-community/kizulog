package io.github.kizulog_community.kizulog.infrastructure.security.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

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

import io.github.kizulog_community.kizulog.domain.systemaccount.exception.IdentityLinkError;
import io.github.kizulog_community.kizulog.domain.systemaccount.exception.IdentityLinkException;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentity;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.service.SystemAccountIdentityLinkService;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.exception.InvitationError;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.exception.InvitationException;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.service.InvitationAcceptanceService;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.service.SystemAdminInvitationService;
import io.github.kizulog_community.kizulog.domain.systemauth.exception.SystemAuthenticationErrorType;
import io.github.kizulog_community.kizulog.domain.systemauth.exception.SystemAuthenticationException;
import io.github.kizulog_community.kizulog.domain.systemauth.service.SystemAuthenticationService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import io.github.kizulog_community.kizulog.infrastructure.web.system.invite.InvitationAcceptanceSession;
import io.github.kizulog_community.kizulog.infrastructure.web.system.myprofile.IdentityLinkSession;

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
    private static final String INVITATION_ID = "invite-1";
    private static final String PROVIDER_ID = "master";
    private static final OffsetDateTime VERSION =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private SystemAuthenticationService authService;
    private SystemAccountIdentityRepository identityRepository;
    private InvitationAcceptanceService invitationAcceptanceService;
    private SystemAdminInvitationService invitationService;
    private SystemAccountIdentityLinkService identityLinkService;
    private InvitationAcceptanceSession invitationSession;
    private IdentityLinkSession identityLinkSession;
    private OAuth2UserService<OidcUserRequest, OidcUser> delegate;
    private SystemOidcUserService sut;

    @BeforeEach
    void setUp() {
        authService = mock(SystemAuthenticationService.class);
        identityRepository = mock(SystemAccountIdentityRepository.class);
        invitationAcceptanceService = mock(InvitationAcceptanceService.class);
        invitationService = mock(SystemAdminInvitationService.class);
        identityLinkService = mock(SystemAccountIdentityLinkService.class);
        invitationSession = mock(InvitationAcceptanceSession.class);
        identityLinkSession = mock(IdentityLinkSession.class);
        @SuppressWarnings("unchecked")
        OAuth2UserService<OidcUserRequest, OidcUser> mockDelegate =
                mock(OAuth2UserService.class);
        delegate = mockDelegate;

        // 既定: pending無し（通常ログインフロー）
        when(invitationSession.isPending()).thenReturn(false);
        when(identityLinkSession.isPending()).thenReturn(false);

        sut = new SystemOidcUserService(
                authService,
                identityRepository,
                invitationAcceptanceService,
                invitationService,
                identityLinkService,
                invitationSession,
                identityLinkSession,
                delegate);
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

    private SystemAccountIdentity newlyCreatedIdentity() {
        return new SystemAccountIdentity(
                "new-identity-id", VERSION, "new-account-id", ISS, AUD, SUB,
                VERSION, "system:invite:" + INVITATION_ID);
    }

    private SystemAccountIdentity newlyLinkedIdentity() {
        return new SystemAccountIdentity(
                "linked-identity-id", VERSION, ACCOUNT_ID, ISS, AUD, SUB,
                VERSION, "system:identity-link:" + ACCOUNT_ID);
    }

    @Test
    @DisplayName("loadUser: 通常ログインフロー - 認証成功時、SystemUserPrincipalが返る（accountId/identityId/iss/aud/sub）")
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
    @DisplayName("loadUser: 通常ログインフロー - 認証成功時、authoritiesにROLE_SYSTEM_ADMINが含まれる")
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
    @DisplayName("loadUser: 通常ログインフロー - authServiceにiss/aud/subが正しく渡される")
    void loadUser_callsAuthServiceWithCorrectArguments() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(authService.authenticate(ISS, AUD, SUB)).thenReturn(identity());

        sut.loadUser(userRequest);

        verify(authService).authenticate(ISS, AUD, SUB);
    }

    @Test
    @DisplayName("loadUser: 通常ログインフロー - ACCOUNT_NOT_FOUND時、OAuth2AuthenticationExceptionに変換")
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
    @DisplayName("loadUser: 通常ログインフロー - IDENTITY_INACTIVE時、OAuth2AuthenticationExceptionに変換")
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
    @DisplayName("loadUser: 通常ログインフロー - ACCOUNT_INACTIVE時、OAuth2AuthenticationExceptionに変換")
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
    @DisplayName("loadUser: 通常ログインフロー - ROLE_NOT_GRANTED時、OAuth2AuthenticationExceptionに変換")
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

    @Test
    @DisplayName("loadUser: 通常ログインフロー - 招待関連サービスは呼ばれない")
    void loadUser_doesNotCallInvitationServices_whenStandardLogin() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(authService.authenticate(ISS, AUD, SUB)).thenReturn(identity());

        sut.loadUser(userRequest);

        verify(invitationAcceptanceService, never())
                .acceptInvitation(anyString(), anyString(), anyString(), anyString());
        verify(invitationService, never())
                .cancelInvitation(anyString(), anyString(), anyString());
        verify(identityRepository, never())
                .findLatestByIssAndAudAndSub(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("loadUser: 通常ログインフロー - identityLinkServiceは呼ばれない")
    void loadUser_doesNotCallIdentityLinkService_whenStandardLogin() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(authService.authenticate(ISS, AUD, SUB)).thenReturn(identity());

        sut.loadUser(userRequest);

        verify(identityLinkService, never()).linkIdentity(
                anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("loadUser: 招待受諾フロー - identity未存在の新規受諾者、acceptInvitationが呼ばれsession.clear()される")
    void loadUser_invitationFlow_createsNewAccount_whenIdentityNotExists() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(invitationSession.isPending()).thenReturn(true);
        when(invitationSession.getInvitationId()).thenReturn(INVITATION_ID);
        when(identityRepository.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.empty());
        when(invitationAcceptanceService.acceptInvitation(INVITATION_ID, ISS, AUD, SUB))
                .thenReturn(newlyCreatedIdentity());

        OidcUser result = sut.loadUser(userRequest);

        // SystemUserPrincipal返却
        assertThat(result).isInstanceOf(SystemUserPrincipal.class);
        SystemUserPrincipal principal = (SystemUserPrincipal) result;
        assertThat(principal.getAccountId()).isEqualTo("new-account-id");
        assertThat(principal.getIdentityId()).isEqualTo("new-identity-id");
        assertThat(principal.getIss()).isEqualTo(ISS);

        // 認証Serviceは呼ばれない
        verify(authService, never()).authenticate(anyString(), anyString(), anyString());
        // 受諾Serviceは呼ばれる
        verify(invitationAcceptanceService).acceptInvitation(INVITATION_ID, ISS, AUD, SUB);
        // セッションはクリアされる
        verify(invitationSession).clear();
    }

    @Test
    @DisplayName("loadUser: 招待受諾フロー - identity既存ユーザーの招待は自動取消されIDENTITY_EXISTSエラー")
    void loadUser_invitationFlow_throwsIdentityExists_whenIdentityAlreadyExists() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(invitationSession.isPending()).thenReturn(true);
        when(invitationSession.getInvitationId()).thenReturn(INVITATION_ID);
        when(identityRepository.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));

        assertThatThrownBy(() -> sut.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting("error.errorCode")
                .isEqualTo(InvitationError.IDENTITY_EXISTS.name());

        // 招待は自動取消される
        verify(invitationService).cancelInvitation(
                eq(INVITATION_ID), anyString(), eq("system:auto-cancel"));
        // アカウント作成は呼ばれない
        verify(invitationAcceptanceService, never())
                .acceptInvitation(anyString(), anyString(), anyString(), anyString());
        // セッションはクリアされる
        verify(invitationSession).clear();
    }

    @Test
    @DisplayName("loadUser: 招待受諾フロー - 自動取消が失敗してもIDENTITY_EXISTSエラーは継続(同時アクセス耐性)")
    void loadUser_invitationFlow_continuesWhenAutoCancelFails() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(invitationSession.isPending()).thenReturn(true);
        when(invitationSession.getInvitationId()).thenReturn(INVITATION_ID);
        when(identityRepository.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        // 自動取消失敗(既にCANCELLED/USED等)
        doThrow(new InvitationException(InvitationError.ALREADY_CANCELLED))
                .when(invitationService).cancelInvitation(
                        anyString(), anyString(), anyString());

        // それでもIDENTITY_EXISTSはthrowされる
        assertThatThrownBy(() -> sut.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting("error.errorCode")
                .isEqualTo(InvitationError.IDENTITY_EXISTS.name());

        // セッションはクリアされる
        verify(invitationSession).clear();
    }

    @Test
    @DisplayName("loadUser: 招待受諾フロー - acceptInvitationが失敗時、INVITATION_NOT_FOUNDで変換されsession.clear()される")
    void loadUser_invitationFlow_throwsInvitationNotFound_whenAcceptanceFails() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(invitationSession.isPending()).thenReturn(true);
        when(invitationSession.getInvitationId()).thenReturn(INVITATION_ID);
        when(identityRepository.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.empty());
        // 受諾処理で例外発生(例: 同時アクセスで既にUSED化された)
        when(invitationAcceptanceService.acceptInvitation(
                INVITATION_ID, ISS, AUD, SUB))
                .thenThrow(new InvitationException(InvitationError.ALREADY_USED));

        assertThatThrownBy(() -> sut.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting("error.errorCode")
                .isEqualTo(InvitationError.INVITATION_NOT_FOUND.name());

        // セッションはクリアされる
        verify(invitationSession).clear();
    }

    @Test
    @DisplayName("loadUser: 招待受諾フロー - delegate.loadUserがOAuth2AuthExceptionを投げた場合はそのまま伝播(セッションは触らない)")
    void loadUser_invitationFlow_propagatesDelegateException() {
        OidcUserRequest userRequest = buildUserRequest();
        OAuth2AuthenticationException delegateException =
                new OAuth2AuthenticationException("invalid_token");
        when(delegate.loadUser(any())).thenThrow(delegateException);
        // session.isPending()はdelegate呼び出し後の処理で評価されるため、ここでは
        // delegate例外が先に発生してsession判定にすら到達しない。
        when(invitationSession.isPending()).thenReturn(true);

        assertThatThrownBy(() -> sut.loadUser(userRequest))
                .isSameAs(delegateException);

        // session判定にすら到達していないため、clearは呼ばれない
        verify(invitationSession, never()).clear();
        // 受諾Serviceも呼ばれない
        verify(invitationAcceptanceService, never())
                .acceptInvitation(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("loadUser: 招待受諾フロー - 認証Service(authenticate)は呼ばれない")
    void loadUser_invitationFlow_doesNotCallStandardAuthService() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(invitationSession.isPending()).thenReturn(true);
        when(invitationSession.getInvitationId()).thenReturn(INVITATION_ID);
        when(identityRepository.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.empty());
        when(invitationAcceptanceService.acceptInvitation(INVITATION_ID, ISS, AUD, SUB))
                .thenReturn(newlyCreatedIdentity());

        sut.loadUser(userRequest);

        verify(authService, never()).authenticate(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("loadUser: identityリンクフロー - 成功時、新規identityでPrincipalを返却・session.clear()される")
    void loadUser_identityLinkFlow_succeeds() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(identityLinkSession.isPending()).thenReturn(true);
        when(identityLinkSession.getTargetAccountId()).thenReturn(ACCOUNT_ID);
        when(identityLinkSession.getProviderId()).thenReturn(PROVIDER_ID);
        when(identityLinkService.linkIdentity(ACCOUNT_ID, PROVIDER_ID, ISS, AUD, SUB))
                .thenReturn(newlyLinkedIdentity());

        OidcUser result = sut.loadUser(userRequest);

        assertThat(result).isInstanceOf(SystemUserPrincipal.class);
        SystemUserPrincipal principal = (SystemUserPrincipal) result;
        assertThat(principal.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(principal.getIdentityId()).isEqualTo("linked-identity-id");

        verify(identityLinkService).linkIdentity(ACCOUNT_ID, PROVIDER_ID, ISS, AUD, SUB);
        verify(identityLinkSession).clear();
        // 通常認証・招待は呼ばれない
        verify(authService, never()).authenticate(anyString(), anyString(), anyString());
        verify(invitationAcceptanceService, never())
                .acceptInvitation(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("loadUser: identityリンクフロー - IdentityLinkException→errorCode付きOAuth2例外＋session.clear()")
    void loadUser_identityLinkFlow_convertsDomainException() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(identityLinkSession.isPending()).thenReturn(true);
        when(identityLinkSession.getTargetAccountId()).thenReturn(ACCOUNT_ID);
        when(identityLinkSession.getProviderId()).thenReturn(PROVIDER_ID);
        when(identityLinkService.linkIdentity(ACCOUNT_ID, PROVIDER_ID, ISS, AUD, SUB))
                .thenThrow(new IdentityLinkException(
                        IdentityLinkError.IDENTITY_ALREADY_LINKED));

        assertThatThrownBy(() -> sut.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting("error.errorCode")
                .isEqualTo(IdentityLinkError.IDENTITY_ALREADY_LINKED.name());

        verify(identityLinkSession).clear();
    }

    @Test
    @DisplayName("loadUser: identityリンクフロー - PROVIDER_ALREADY_LINKED→errorCode付きOAuth2例外")
    void loadUser_identityLinkFlow_providerAlreadyLinked() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(identityLinkSession.isPending()).thenReturn(true);
        when(identityLinkSession.getTargetAccountId()).thenReturn(ACCOUNT_ID);
        when(identityLinkSession.getProviderId()).thenReturn(PROVIDER_ID);
        when(identityLinkService.linkIdentity(ACCOUNT_ID, PROVIDER_ID, ISS, AUD, SUB))
                .thenThrow(new IdentityLinkException(
                        IdentityLinkError.PROVIDER_ALREADY_LINKED));

        assertThatThrownBy(() -> sut.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting("error.errorCode")
                .isEqualTo(IdentityLinkError.PROVIDER_ALREADY_LINKED.name());

        verify(identityLinkSession).clear();
    }

    @Test
    @DisplayName("loadUser: identityリンクフロー - 予期せぬRuntimeException→PROVIDER_NOT_FOUND相当に変換")
    void loadUser_identityLinkFlow_unexpectedExceptionMapsToProviderNotFound() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(identityLinkSession.isPending()).thenReturn(true);
        when(identityLinkSession.getTargetAccountId()).thenReturn(ACCOUNT_ID);
        when(identityLinkSession.getProviderId()).thenReturn(PROVIDER_ID);
        when(identityLinkService.linkIdentity(ACCOUNT_ID, PROVIDER_ID, ISS, AUD, SUB))
                .thenThrow(new RuntimeException("DB connection refused"));

        assertThatThrownBy(() -> sut.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting("error.errorCode")
                .isEqualTo(IdentityLinkError.PROVIDER_NOT_FOUND.name());

        verify(identityLinkSession).clear();
    }

    @Test
    @DisplayName("loadUser: 招待pendingとidentityリンクpendingが両方ONなら、招待を優先（フェイルセーフ）")
    void loadUser_invitationTakesPriorityOverIdentityLink() {
        OidcUser oidcUser = buildOidcUser();
        OidcUserRequest userRequest = buildUserRequest();
        when(delegate.loadUser(any())).thenReturn(oidcUser);
        when(invitationSession.isPending()).thenReturn(true);
        when(invitationSession.getInvitationId()).thenReturn(INVITATION_ID);
        when(identityLinkSession.isPending()).thenReturn(true);
        when(identityRepository.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.empty());
        when(invitationAcceptanceService.acceptInvitation(INVITATION_ID, ISS, AUD, SUB))
                .thenReturn(newlyCreatedIdentity());

        sut.loadUser(userRequest);

        verify(invitationAcceptanceService).acceptInvitation(INVITATION_ID, ISS, AUD, SUB);
        verify(identityLinkService, never()).linkIdentity(
                anyString(), anyString(), anyString(), anyString(), anyString());
    }

}
