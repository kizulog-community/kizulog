package io.github.kizulog_community.kizulog.infrastructure.web.system.myprofile;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.kizulog_community.kizulog.domain.systemoidc.service.IdentityClaimsViewService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import lombok.RequiredArgsConstructor;

/**
 * マイプロフィールランチャーコントローラー
 *
 * <p>URL: GET /system/my-profile</p>
 *
 * @author Jun Kobayashi
 */
@Controller
@RequestMapping("/system/my-profile")
@RequiredArgsConstructor
public class MyProfileController {

    /** Identityクレームビューサービス */
    private final IdentityClaimsViewService identityClaimsViewService;

    /**
     * マイプロフィールメニュー画面を表示する。
     *
     * @param principal 認証済みプリンシパル
     * @param locale ロケール（i18nラベル用）
     * @param model モデル
     * @return ビュー名
     */
    @GetMapping
    public String index(
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            Model model) {

        model.addAttribute("activeMenu", "my-profile");

        if (principal != null) {
            List<Map<String, String>> myClaimsDisplay =
                    identityClaimsViewService.resolveClaimsDisplay(
                            principal.getIdentityId(), locale);
            model.addAttribute("myClaimsDisplay", myClaimsDisplay);
            model.addAttribute("myIss", principal.getIss());
            model.addAttribute("myAud", principal.getAud());
            model.addAttribute("mySub", principal.getSub());
        } else {
            model.addAttribute("myClaimsDisplay", List.of());
            model.addAttribute("myIss", null);
            model.addAttribute("myAud", null);
            model.addAttribute("mySub", null);
        }

        return "system/my-profile/index";
    }

}
