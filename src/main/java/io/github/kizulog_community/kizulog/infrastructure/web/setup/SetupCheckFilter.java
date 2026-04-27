package io.github.kizulog_community.kizulog.infrastructure.web.setup;

import java.io.IOException;

import io.github.kizulog_community.kizulog.domain.systemconfig.model.SystemConfig;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.SystemConfigService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * セットアップチェックフィルター
 *
 * <p>OIDC設定が未登録の場合はセットアップ画面にリダイレクトする。
 * セットアップ完了後はセットアップ画面へのアクセスを禁止する。</p>
 *
 * @author Jun Kobayashi
 */
@RequiredArgsConstructor
public class SetupCheckFilter implements Filter {

    /** セットアップ画面のパスプレフィックス */
    private static final String SETUP_PATH = "/setup";

    /** 完了画面のパス */
    private static final String COMPLETE_PATH = "/setup/complete";

    /** システム設定サービス */
    private final SystemConfigService systemConfigService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI();

        // 静的リソースはフィルター対象外
        if (path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.startsWith("/favicon")) {
            chain.doFilter(req, res);
            return;
        }

        // 完了画面は常にアクセス許可
        if (path.equals(COMPLETE_PATH)) {
            chain.doFilter(req, res);
            return;
        }

        boolean isSetupPath = path.startsWith(SETUP_PATH);
        boolean isSetupCompleted = isSetupCompleted();

        if (!isSetupCompleted && !isSetupPath) {
            response.sendRedirect(request.getContextPath() + SETUP_PATH + "/step0");
            return;
        }

        if (isSetupCompleted && isSetupPath) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(req, res);
    }

    /**
     * セットアップの完了判定
     *
     * <p>OIDCキーが存在しかつ1件以上の設定がある場合にセットアップ完了と判断する。</p>
     *
     * @return セットアップ完了の場合はtrue、それ以外はfalse
     */
    private boolean isSetupCompleted() {
        return systemConfigService.findLatestByKey("OIDC")
                .map(SystemConfig::getValue)
                .map(value -> !value.isEmpty() && !value.equals("[]"))
                .orElse(false);
    }

}