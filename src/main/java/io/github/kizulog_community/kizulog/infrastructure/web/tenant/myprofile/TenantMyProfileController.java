package io.github.kizulog_community.kizulog.infrastructure.web.tenant.myprofile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.kizulog_community.kizulog.infrastructure.security.principal.TenantUserPrincipal;

/**
 * テナント利用者マイプロフィールコントローラー
 *
 * <p>URL: GET /my-profile</p>
 *
 * @author Jun Kobayashi
 */
@Controller
@RequestMapping("/my-profile")
public class TenantMyProfileController {

    /**
     * マイプロフィール画面を表示する。
     *
     * @param principal 認証済みプリンシパル
     * @param model モデル
     * @return ビュー名
     */
    @GetMapping
    public String index(
            @AuthenticationPrincipal TenantUserPrincipal principal,
            Model model) {

        model.addAttribute("activeMenu", "my-profile");
        model.addAttribute("principal", principal);
        return "tenant/my-profile/index";
    }

}
