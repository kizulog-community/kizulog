package io.github.kizulog_community.kizulog.infrastructure.security.handler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import io.github.kizulog_community.kizulog.domain.systemadmininvitation.exception.InvitationError;

/**
 * SystemAuthenticationFailureHandlerの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemAuthenticationFailureHandlerTest {

    private SystemAuthenticationFailureHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        handler = new SystemAuthenticationFailureHandler();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    private OAuth2AuthenticationException oauthExceptionWithCode(String code) {
        return new OAuth2AuthenticationException(new OAuth2Error(code));
    }

    @Test
    @DisplayName("onAuthenticationFailure: IDENTITY_EXISTS は /system/invite/error?code=IDENTITY_EXISTS へ")
    void onFailure_identityExists_redirectsToInviteError() throws Exception {
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode(InvitationError.IDENTITY_EXISTS.name()));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/system/invite/error?code=IDENTITY_EXISTS");
    }

    @Test
    @DisplayName("onAuthenticationFailure: INVITATION_NOT_FOUND は /system/invite/error?code=INVITATION_NOT_FOUND へ")
    void onFailure_invitationNotFound_redirectsToInviteError() throws Exception {
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode(InvitationError.INVITATION_NOT_FOUND.name()));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/system/invite/error?code=INVITATION_NOT_FOUND");
    }

    @Test
    @DisplayName("onAuthenticationFailure: EXPIRED は /system/invite/error?code=EXPIRED へ")
    void onFailure_expired_redirectsToInviteError() throws Exception {
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode(InvitationError.EXPIRED.name()));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/system/invite/error?code=EXPIRED");
    }

    @Test
    @DisplayName("onAuthenticationFailure: ALREADY_USED は /system/invite/error?code=ALREADY_USED へ")
    void onFailure_alreadyUsed_redirectsToInviteError() throws Exception {
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode(InvitationError.ALREADY_USED.name()));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/system/invite/error?code=ALREADY_USED");
    }

    @Test
    @DisplayName("onAuthenticationFailure: CANCELLED は /system/invite/error?code=CANCELLED へ")
    void onFailure_cancelled_redirectsToInviteError() throws Exception {
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode(InvitationError.CANCELLED.name()));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/system/invite/error?code=CANCELLED");
    }

    @Test
    @DisplayName("onAuthenticationFailure: INVALID_TOKEN は /system/invite/error?code=INVALID_TOKEN へ")
    void onFailure_invalidToken_redirectsToInviteError() throws Exception {
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode(InvitationError.INVALID_TOKEN.name()));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/system/invite/error?code=INVALID_TOKEN");
    }

    @Test
    @DisplayName("onAuthenticationFailure: OIDC_FAILED は /system/invite/error?code=OIDC_FAILED へ")
    void onFailure_oidcFailed_redirectsToInviteError() throws Exception {
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode(InvitationError.OIDC_FAILED.name()));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/system/invite/error?code=OIDC_FAILED");
    }

    @Test
    @DisplayName("onAuthenticationFailure: ACCOUNT_NOT_FOUND は /system/login?error=ACCOUNT_NOT_FOUND へ")
    void onFailure_accountNotFound_redirectsToLogin() throws Exception {
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode("ACCOUNT_NOT_FOUND"));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/system/login?error=ACCOUNT_NOT_FOUND");
    }

    @Test
    @DisplayName("onAuthenticationFailure: IDENTITY_INACTIVE は /system/login?error=IDENTITY_INACTIVE へ")
    void onFailure_identityInactive_redirectsToLogin() throws Exception {
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode("IDENTITY_INACTIVE"));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/system/login?error=IDENTITY_INACTIVE");
    }

    @Test
    @DisplayName("onAuthenticationFailure: ACCOUNT_INACTIVE は /system/login?error=ACCOUNT_INACTIVE へ")
    void onFailure_accountInactive_redirectsToLogin() throws Exception {
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode("ACCOUNT_INACTIVE"));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/system/login?error=ACCOUNT_INACTIVE");
    }

    @Test
    @DisplayName("onAuthenticationFailure: ROLE_NOT_GRANTED は /system/login?error=ROLE_NOT_GRANTED へ")
    void onFailure_roleNotGranted_redirectsToLogin() throws Exception {
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode("ROLE_NOT_GRANTED"));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/system/login?error=ROLE_NOT_GRANTED");
    }

    @Test
    @DisplayName("onAuthenticationFailure: OAuth2例外以外のAuthenticationException → /system/login?error")
    void onFailure_nonOauthException_redirectsToLoginErrorDefault() throws Exception {
        AuthenticationException nonOauth = new BadCredentialsException("bad creds");

        handler.onAuthenticationFailure(request, response, nonOauth);

        assertThat(response.getRedirectedUrl()).isEqualTo("/system/login?error");
    }

    @Test
    @DisplayName("onAuthenticationFailure: contextPath が設定されている場合、前置される")
    void onFailure_withContextPath_redirectsWithContextPath() throws Exception {
        request.setContextPath("/app");

        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode(InvitationError.IDENTITY_EXISTS.name()));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/app/system/invite/error?code=IDENTITY_EXISTS");
    }

    @Test
    @DisplayName("onAuthenticationFailure: errorCodeに特殊文字が含まれる場合、URLエンコードされる")
    void onFailure_specialCharInErrorCode_isUrlEncoded() throws Exception {
        // 非InvitationErrorで特殊文字あり
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode("error with spaces"));

        // " " → "+" (URLEncoder.encode のデフォルト挙動)
        assertThat(response.getRedirectedUrl())
                .isEqualTo("/system/login?error=error+with+spaces");
    }

}
