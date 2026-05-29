package io.github.kizulog_community.kizulog.infrastructure.security.handler;

import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * テナント利用者のOIDCログイン成功時のハンドラ
 *
 * <p>遷移先の決定:
 * <ul>
 * <li>認証を要求されて中断された元リクエスト（SavedRequest）があればそこへ戻す。</li>
 * <li>SavedRequest が無い場合はテナントダッシュボードへ。</li>
 * </ul>
 * </p>
 *
 * @author Jun Kobayashi
 */
@Component
public class TenantAuthenticationSuccessHandler
        extends SavedRequestAwareAuthenticationSuccessHandler {

    /** ログイン成功後のデフォルト遷移先（テナントダッシュボード） */
    private static final String DEFAULT_TARGET_URL = "/dashboard";

    /**
     * コンストラクタ
     */
    public TenantAuthenticationSuccessHandler() {
        setDefaultTargetUrl(DEFAULT_TARGET_URL);
    }

}
