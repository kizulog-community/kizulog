package io.github.kizulog_community.kizulog.infrastructure.web.setup;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.SystemConfig;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.SystemConfigService;
import lombok.RequiredArgsConstructor;

/**
 * セットアップチェックフィルター
 *
 * <p>OIDC設定が未登録の場合はセットアップ画面にリダイレクトする。
 * セットアップ完了後はセットアップ画面へのアクセスを禁止する。</p>
 *
 * @author Jun Kobayashi
 */
@Component
@RequiredArgsConstructor
public class SetupCheckFilter implements Filter {

    /** セットアップ画面のパスプレフィックス */
    private static final String SETUP_PATH = "/setup";

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

        boolean isSetupPath = path.startsWith(SETUP_PATH);
        boolean isSetupCompleted = isSetupCompleted();

        // 未セットアップ かつ セットアップ画面以外 → セットアップ画面へリダイレクト
        if (!isSetupCompleted && !isSetupPath) {
            response.sendRedirect(SETUP_PATH + "/step1");
            return;
        }

        // セットアップ完了済み かつ セットアップ画面へのアクセス → 403
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