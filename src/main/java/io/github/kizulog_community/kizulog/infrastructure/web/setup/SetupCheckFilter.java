package io.github.kizulog_community.kizulog.infrastructure.web.setup;

import java.io.IOException;

import io.github.kizulog_community.kizulog.domain.systemoidc.port.SystemOidcProviderRepository;
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
 * @author Jun Kobayashi
 */
@RequiredArgsConstructor
public class SetupCheckFilter implements Filter {

    /** セットアップ画面のパスプレフィックス */
    private static final String SETUP_PATH = "/setup";

    /** 完了画面のパス */
    private static final String COMPLETE_PATH = "/setup/complete";

    /** OIDCプロバイダーリポジトリ */
    private final SystemOidcProviderRepository systemOidcProviderRepository;

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

        // 完了画面はセットアップ完了直後に1回だけ表示する用途のため、
        // 完了状態でも未完了状態でも常にアクセス許可する。
        if (path.equals(COMPLETE_PATH)) {
            chain.doFilter(req, res);
            return;
        }

        boolean isSetupPath = path.startsWith(SETUP_PATH);
        boolean isSetupCompleted = isSetupCompleted();

        // 未完了かつセットアップ画面以外へのアクセスは、セットアップ画面にリダイレクト
        if (!isSetupCompleted && !isSetupPath) {
            response.sendRedirect(request.getContextPath() + SETUP_PATH + "/step0");
            return;
        }

        // 完了済みかつセットアップ画面へのアクセスは、403 Forbiddenで拒否
        if (isSetupCompleted && isSetupPath) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(req, res);
    }

    /**
     * セットアップの完了判定
     *
     * <p>{@code system_oidc_providers}テーブルに1件以上の
     * レコードが存在する場合にセットアップ完了と判断する。</p>
     *
     * @return セットアップ完了の場合はtrue、それ以外はfalse
     */
    private boolean isSetupCompleted() {
        return systemOidcProviderRepository.existsAny();
    }

}
