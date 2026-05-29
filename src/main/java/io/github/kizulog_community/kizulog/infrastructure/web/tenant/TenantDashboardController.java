package io.github.kizulog_community.kizulog.infrastructure.web.tenant;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import io.github.kizulog_community.kizulog.domain.tenant.model.Tenant;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.TenantUserPrincipal;

/**
 * テナント利用者ダッシュボード画面のコントローラー
 *
 * <p>/dashboardでログイン後のダッシュボードを表示する。
 * テナントホスト配下で認証済みの利用者のみアクセス可能。</p>
 *
 * @author Jun Kobayashi
 */
@Controller
public class TenantDashboardController {

    /**
     * ダッシュボード画面を表示する。
     *
     * @param principal ログイン中のテナント利用者
     * @param model モデル
     * @return ビュー名
     */
    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal TenantUserPrincipal principal,
            Model model) {
        // 受諾アクセス元ホストから解決したテナント（TenantResolverFilter が設定済み）
        Tenant tenant = TenantContext.current();
        model.addAttribute("principal", principal);
        model.addAttribute("tenant", tenant);
        return "tenant/dashboard";
    }

}
