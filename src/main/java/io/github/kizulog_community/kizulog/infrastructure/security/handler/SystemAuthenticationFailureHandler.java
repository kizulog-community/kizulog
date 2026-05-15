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

import io.github.kizulog_community.kizulog.domain.systemadmininvitation.exception.InvitationError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * システム管理画面の認証失敗時のハンドラ
 *
 * <p>遷移先：
 * <ul>
 * <li>InvitationError系（IDENTITY_EXISTS, INVITATION_NOT_FOUND等）→ /system/invite/error?code={errorCode}</li>
 * <li>それ以外 → /system/login?error={errorCode}</li>
 * <li>errorCode取得不能 → /system/login?error</li>
 * </ul>
 * </p>
 *
 * @author Jun Kobayashi
 */
@Component
public class SystemAuthenticationFailureHandler implements AuthenticationFailureHandler {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(SystemAuthenticationFailureHandler.class);

    /** デフォルトのリダイレクト先（エラーコード不明時） */
    private static final String DEFAULT_FAILURE_URL = "/system/login?error";

    /** ログインエラー時のリダイレクト先テンプレート */
    private static final String LOGIN_FAILURE_URL_WITH_CODE = "/system/login?error=";

    /** 招待エラー時のリダイレクト先テンプレート */
    private static final String INVITE_FAILURE_URL_WITH_CODE = "/system/invite/error?code=";

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
        log.info("認証失敗: redirectTo={}", redirectUrl);
        response.sendRedirect(request.getContextPath() + redirectUrl);
    }

    /**
     * エラーコードが InvitationError 由来か判定する。
     *
     * @param errorCode エラーコード
     * @return InvitationError由来ならtrue
     */
    private boolean isInvitationError(String errorCode) {
        for (InvitationError e : InvitationError.values()) {
            if (e.name().equals(errorCode)) {
                return true;
            }
        }
        return false;
    }

}
