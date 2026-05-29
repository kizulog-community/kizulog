package io.github.kizulog_community.kizulog.infrastructure.security.handler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.exception.TenantInvitationError;
import io.github.kizulog_community.kizulog.domain.tenantauth.exception.TenantAuthenticationErrorType;

/**
 * TenantAuthenticationFailureHandlerの単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantAuthenticationFailureHandlerTest {

    private TenantAuthenticationFailureHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        handler = new TenantAuthenticationFailureHandler();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    private OAuth2AuthenticationException oauthExceptionWithCode(String code) {
        return new OAuth2AuthenticationException(new OAuth2Error(code));
    }

    @Test
    @DisplayName("onAuthenticationFailure: ACCOUNT_NOT_FOUND は /login?error=ACCOUNT_NOT_FOUND へ")
    void onFailure_accountNotFound_redirectsToLoginError() throws Exception {
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode(TenantAuthenticationErrorType.ACCOUNT_NOT_FOUND.name()));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/login?error=ACCOUNT_NOT_FOUND");
    }

    @Test
    @DisplayName("onAuthenticationFailure: ROLE_NOT_GRANTED は /login?error=ROLE_NOT_GRANTED へ")
    void onFailure_roleNotGranted_redirectsToLoginError() throws Exception {
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode(TenantAuthenticationErrorType.ROLE_NOT_GRANTED.name()));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/login?error=ROLE_NOT_GRANTED");
    }

    @Test
    @DisplayName("onAuthenticationFailure: ACCOUNT_INACTIVE は /login?error=ACCOUNT_INACTIVE へ")
    void onFailure_accountInactive_redirectsToLoginError() throws Exception {
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode(TenantAuthenticationErrorType.ACCOUNT_INACTIVE.name()));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/login?error=ACCOUNT_INACTIVE");
    }

    @Test
    @DisplayName("onAuthenticationFailure: IDENTITY_INACTIVE は /login?error=IDENTITY_INACTIVE へ")
    void onFailure_identityInactive_redirectsToLoginError() throws Exception {
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode(TenantAuthenticationErrorType.IDENTITY_INACTIVE.name()));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/login?error=IDENTITY_INACTIVE");
    }

    @Test
    @DisplayName("onAuthenticationFailure: OAuth2以外の例外はデフォルト /login?error へ")
    void onFailure_nonOAuth2_redirectsToDefault() throws Exception {
        handler.onAuthenticationFailure(request, response,
                new BadCredentialsException("bad"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error");
    }

    @Test
    @DisplayName("onAuthenticationFailure: contextPathがある場合はprefixされる")
    void onFailure_withContextPath_prefixesPath() throws Exception {
        request.setContextPath("/app");

        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode(TenantAuthenticationErrorType.ACCOUNT_NOT_FOUND.name()));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/app/login?error=ACCOUNT_NOT_FOUND");
    }

    @Test
    @DisplayName("onAuthenticationFailure: errorCodeはURLエンコードされる")
    void onFailure_errorCodeIsUrlEncoded() throws Exception {
        // 通常のエラーコードは英大文字とアンダースコアのみだが、念のため特殊文字を検証
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode("CODE WITH SPACE"));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/login?error=CODE+WITH+SPACE");
    }

    @Test
    @DisplayName("onAuthenticationFailure: IDENTITY_EXISTS は /admin-invite/error?code=IDENTITY_EXISTS へ")
    void onFailure_identityExists_redirectsToInviteError() throws Exception {
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode(TenantInvitationError.IDENTITY_EXISTS.name()));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/admin-invite/error?code=IDENTITY_EXISTS");
    }

    @Test
    @DisplayName("onAuthenticationFailure: TENANT_MISMATCH は /admin-invite/error?code=TENANT_MISMATCH へ")
    void onFailure_tenantMismatch_redirectsToInviteError() throws Exception {
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode(TenantInvitationError.TENANT_MISMATCH.name()));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/admin-invite/error?code=TENANT_MISMATCH");
    }

    @Test
    @DisplayName("onAuthenticationFailure: INVITATION_NOT_FOUND は /admin-invite/error?code=INVITATION_NOT_FOUND へ")
    void onFailure_invitationNotFound_redirectsToInviteError() throws Exception {
        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode(TenantInvitationError.INVITATION_NOT_FOUND.name()));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/admin-invite/error?code=INVITATION_NOT_FOUND");
    }

    @Test
    @DisplayName("onAuthenticationFailure: 受諾エラーもcontextPathがprefixされる")
    void onFailure_inviteError_withContextPath_prefixesPath() throws Exception {
        request.setContextPath("/app");

        handler.onAuthenticationFailure(request, response,
                oauthExceptionWithCode(TenantInvitationError.IDENTITY_EXISTS.name()));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/app/admin-invite/error?code=IDENTITY_EXISTS");
    }

}
