package io.github.kizulog_community.kizulog.infrastructure.web.system;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * アカウント管理画面のコントローラー
 *
 * @author Jun Kobayashi
 */
@Controller
public class SystemAccountsController {

    /**
     * アカウント管理画面（TOP）を表示
     *
     * @param model Model
     * @return view name
     */
    @GetMapping("/system/accounts")
    public String accounts(Model model) {
        model.addAttribute("activeMenu", "accounts");
        return "system/accounts";
    }

}
