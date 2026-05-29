package io.github.kizulog_community.kizulog.infrastructure.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import io.github.kizulog_community.kizulog.infrastructure.security.matcher.SystemHostMatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * ルートパスのリダイレクトコントローラー
 *
 * <p>ルートへのアクセスをリクエストのホストに応じて適切なダッシュボードへ振り分ける。
 * システム・テナント双方のFilterChainで共有されるハンドラ。</p>
 *
 * <p>振り分け:
 * <ul>
 * <li>システムホスト → /system/dashboard</li>
 * <li>テナントホスト → /dashboard</li>
 * </ul>
 * </p>
 *
 * @author Jun Kobayashi
 */
@Controller
@RequiredArgsConstructor
public class RootRedirectController {

    /** システムダッシュボードへのリダイレクト */
    private static final String REDIRECT_SYSTEM_DASHBOARD = "redirect:/system/dashboard";

    /** テナントダッシュボードへのリダイレクト */
    private static final String REDIRECT_TENANT_DASHBOARD = "redirect:/dashboard";

    /** システムホスト判定マッチャ */
    private final SystemHostMatcher systemHostMatcher;

    /**
     * ルートパスを、ホストに応じたダッシュボードへリダイレクトする。
     *
     * @param request HTTPリクエスト
     * @return ホストに対応するダッシュボードへのリダイレクト
     */
    @GetMapping("/")
    public String root(HttpServletRequest request) {
        if (systemHostMatcher.matches(request)) {
            return REDIRECT_SYSTEM_DASHBOARD;
        }
        return REDIRECT_TENANT_DASHBOARD;
    }

}
