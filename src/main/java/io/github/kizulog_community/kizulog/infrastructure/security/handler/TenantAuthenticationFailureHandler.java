package io.github.kizulog_community.kizulog.infrastructure.security.handler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.exception.TenantInvitationError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * テナント利用者の認証失敗時のハンドラ
 *
 * <p>遷移先：
 * <ul>
 * <li>TenantInvitationError系（IDENTITY_EXISTS, TENANT_MISMATCH 等）→ /admin-invite/error?code={errorCode}</li>
 * <li>それ以外（TenantAuthenticationErrorType系・不明）→ /login?error={errorCode}</li>
 * <li>errorCode取得不能 → /login?error</li>
 * </ul>
 * </p>
 *
 * @author Jun Kobayashi
 */
@Component
public class TenantAuthenticationFailureHandler implements AuthenticationFailureHandler {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(TenantAuthenticationFailureHandler.class);

    /** デフォルトのリダイレクト先（エラーコード不明時） */
    private static final String DEFAULT_FAILURE_URL = "/login?error";

    /** ログインエラー時のリダイレクト先テンプレート */
    private static final String LOGIN_FAILURE_URL_WITH_CODE = "/login?error=";

    /** 招待受諾エラー時のリダイレクト先テンプレート */
    private static final String INVITE_FAILURE_URL_WITH_CODE = "/admin-invite/error?code=";

    /**
     * 認証失敗時の処理。
     *
     * @param request HTTPリクエスト
     * @param response HTTPレスポンス
     * @param exception 認証例外
     * @throws IOException IO例外
     * @throws ServletException Servlet例外
     */
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        String redirectUrl = DEFAULT_FAILURE_URL;
        if (exception instanceof OAuth2AuthenticationException oae) {
            String errorCode = oae.getError().getErrorCode();
            if (errorCode != null && !errorCode.isBlank()) {
                String encoded = URLEncoder.encode(errorCode, StandardCharsets.UTF_8);
                if (isInvitationError(errorCode)) {
                    redirectUrl = INVITE_FAILURE_URL_WITH_CODE + encoded;
                } else {
                    redirectUrl = LOGIN_FAILURE_URL_WITH_CODE + encoded;
                }
            }
        }
        log.info("テナント認証失敗: redirectTo={}", redirectUrl);
        response.sendRedirect(request.getContextPath() + redirectUrl);
    }

    /**
     * エラーコードが TenantInvitationError 由来か判定する。
     *
     * @param errorCode エラーコード
     * @return TenantInvitationError由来ならtrue
     */
    private boolean isInvitationError(String errorCode) {
        for (TenantInvitationError e : TenantInvitationError.values()) {
            if (e.name().equals(errorCode)) {
                return true;
            }
        }
        return false;
    }

}
