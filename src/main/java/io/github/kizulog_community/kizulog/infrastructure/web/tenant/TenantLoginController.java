package io.github.kizulog_community.kizulog.infrastructure.web.tenant;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import io.github.kizulog_community.kizulog.domain.tenant.model.Tenant;

/**
 * テナント側ログイン画面コントローラ
 *
 * <p>URL: /t/{slug}/login</p>
 *
 * @author Jun Kobayashi
 */
@Controller
public class TenantLoginController {

    /**
     * テナント側ログイン画面
     *
     * @param sg URL用slug
     * @param model モデル
     * @return ログインテンプレート
     */
    @GetMapping("/t/{slug}/login")
    public String login(
            @PathVariable("slug") String sg,
            Model model) {

        Tenant tenant = TenantContext.current();
        // TenantResolverFilterが先に動作しているため、ここでtenantはnullになり得ない
        // ただし防御的に分岐
        if (tenant != null) {
            model.addAttribute("tenantName", tenant.getName());
            model.addAttribute("tenantSlug", tenant.getSlug());
        } else {
            model.addAttribute("tenantName", "");
            model.addAttribute("tenantSlug", sg);
        }
        return "tenant/login";
    }

}
