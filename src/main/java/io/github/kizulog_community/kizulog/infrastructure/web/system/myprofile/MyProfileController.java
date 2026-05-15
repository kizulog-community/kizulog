package io.github.kizulog_community.kizulog.infrastructure.web.system.myprofile;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * マイプロフィールランチャーコントローラー
 *
 * <p>URL: GET /system/my-profile</p>
 *
 * @author Jun Kobayashi
 */
@Controller
@RequestMapping("/system/my-profile")
public class MyProfileController {

    /**
     * マイプロフィールメニュー画面を表示する。
     *
     * @param model モデル
     * @return ビュー名
     */
    @GetMapping
    public String index(Model model) {
        model.addAttribute("activeMenu", "my-profile");
        return "system/my-profile/index";
    }

}
