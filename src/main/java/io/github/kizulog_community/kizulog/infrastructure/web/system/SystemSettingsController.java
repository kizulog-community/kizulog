package io.github.kizulog_community.kizulog.infrastructure.web.system;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * システム管理画面のコントローラー
 *
 * @author Jun Kobayashi
 */
@Controller
public class SystemSettingsController {

    /**
     * システム管理画面（TOP）を表示
     *
     * @param model Model
     * @return view name
     */
    @GetMapping("/system/system-settings")
    public String systemSettings(Model model) {
        model.addAttribute("activeMenu", "system-settings");
        return "system/system-settings";
    }

}
