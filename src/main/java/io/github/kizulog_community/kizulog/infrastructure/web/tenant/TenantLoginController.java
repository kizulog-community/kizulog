package io.github.kizulog_community.kizulog.infrastructure.web.tenant;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import io.github.kizulog_community.kizulog.domain.tenant.model.Tenant;
import io.github.kizulog_community.kizulog.domain.tenant.service.TenantManagementService;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.EnabledTenantOidcProviderView;
import io.github.kizulog_community.kizulog.domain.tenantoidc.service.TenantOidcProviderService;
import lombok.RequiredArgsConstructor;

/**
 * テナント側ログイン画面コントローラー
 *
 * <p>URL: GET /t/{slug}/login</p>
 *
 * @author Jun Kobayashi
 */
@Controller
@RequiredArgsConstructor
public class TenantLoginController {

    private static final Logger log =
            LoggerFactory.getLogger(TenantLoginController.class);

    private final TenantManagementService tenantManagementService;
    private final TenantOidcProviderService tenantOidcProviderService;

    /**
     * テナントログイン画面を表示する。
     *
     * @param sg テナント slug
     * @param model モデル
     * @return ログインテンプレート、テナントが見つからない場合は 404 ビュー
     */
    @GetMapping("/t/{slug}/login")
    public String login(
            @PathVariable("slug") String sg,
            Model model) {

        // テナント解決（Filterで検証済みだが、念のため二重チェック）
        Tenant tenant = tenantManagementService.findTenantBySlug(sg)
                .orElse(null);
        if (tenant == null) {
            log.warn("テナントログイン画面: slugでテナントが見つかりません: slug={}", sg);
            return "error/404";
        }

        // ENABLEDプロバイダー一覧を取得（display_name 昇順）
        List<EnabledTenantOidcProviderView> enabledProviders =
                tenantOidcProviderService.findAllEnabledByTenantId(tenant.getTenantId());

        log.debug("テナントログイン画面表示: tenantId={}, slug={}, enabledProviderCount={}",
                tenant.getTenantId(), sg, enabledProviders.size());

        // Model 設定
        model.addAttribute("tenant", tenant);
        model.addAttribute("slug", sg);
        model.addAttribute("enabledProviders", enabledProviders);
        // 認証フロー未実装フラグ（テンプレート側でボタンを disabled にする判定に使用）
        model.addAttribute("authFlowImplemented", false);

        return "tenant/login";
    }

}
