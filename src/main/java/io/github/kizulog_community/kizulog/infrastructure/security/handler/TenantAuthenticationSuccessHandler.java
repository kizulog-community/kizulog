package io.github.kizulog_community.kizulog.infrastructure.security.handler;

import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * テナント利用者のOIDCログイン成功時のハンドラ
 *
 * @author Jun Kobayashi
 */
@Component
public class TenantAuthenticationSuccessHandler
        extends SavedRequestAwareAuthenticationSuccessHandler {

    /** ログイン成功後のデフォルト遷移先（テナントホストのトップ） */
    private static final String DEFAULT_TARGET_URL = "/";

    /**
     * コンストラクタ
     */
    public TenantAuthenticationSuccessHandler() {
        setDefaultTargetUrl(DEFAULT_TARGET_URL);
    }

}
