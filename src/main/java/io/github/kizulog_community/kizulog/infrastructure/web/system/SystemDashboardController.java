package io.github.kizulog_community.kizulog.infrastructure.web.system;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;

/**
 * システム管理ダッシュボード画面のコントローラー
 *
 * <p>/system/dashboardでダッシュボードを表示する。
 * SYSTEM_ADMINロールが付与されたユーザーのみアクセス可能（SystemSecurityConfigで制御）。</p>
 *
 * @author Jun Kobayashi
 */
@Controller
public class SystemDashboardController {

    /**
     * ダッシュボード画面を表示
     *
     * @param principal ログイン中のシステム管理者
     * @param model Model
     * @return view name
     */
    @GetMapping("/system/dashboard")
    public String dashboard(
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Model model) {
        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("principal", principal);
        return "system/dashboard";
    }

}
