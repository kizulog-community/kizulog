package io.github.kizulog_community.kizulog.infrastructure.web.setup;

import java.util.Arrays;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import io.github.kizulog_community.kizulog.domain.setup.SetupService;
import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import lombok.RequiredArgsConstructor;

/**
 * セットアップウィザードコントローラー。
 *
 * <p>セットアップウィザードの画面遷移とフォーム処理を担う。</p>
 *
 * @author Jun Kobayashi
 */
@Controller
@RequestMapping("/setup")
@RequiredArgsConstructor
public class SetupController {

    /** セットアップサービス */
    private final SetupService setupService;

    /** セットアップセッションデータ */
    private final SetupSessionData setupSessionData;

    /**
     * Step1：OIDC設定画面を表示する。
     *
     * @param model モデル
     * @return Step1テンプレート
     */
    @GetMapping("/step1")
    public String step1(Model model) {
        if (setupSessionData.getOidcSettings().isEmpty()) {
            setupSessionData.getOidcSettings().add(new OidcSetting());
        }
        model.addAttribute("sessionData", setupSessionData);
        return "setup/step1";
    }

    /**
     * Step1：OIDC設定を受け取りStep2へ遷移する。
     *
     * @param sessionData フォームデータ
     * @return Step2へリダイレクト
     */
    @PostMapping("/step1")
    public String step1Submit(
            @ModelAttribute SetupSessionData sessionData) {
        setupSessionData.setOidcSettings(sessionData.getOidcSettings());
        return "redirect:/setup/step2";
    }

    /**
     * Step2：基本設定画面を表示する。
     *
     * @param model モデル
     * @return Step2テンプレート
     */
    @GetMapping("/step2")
    public String step2(Model model) {
        model.addAttribute("sessionData", setupSessionData);
        model.addAttribute("languages", Arrays.asList(SupportedLanguage.values()));
        model.addAttribute("timezones", Arrays.asList(SupportedTimezone.values()));
        return "setup/step2";
    }

    /**
     * Step2：基本設定を受け取りStep3へ遷移する。
     *
     * @param sessionData フォームデータ
     * @return Step3へリダイレクト
     */
    @PostMapping("/step2")
    public String step2Submit(
            @ModelAttribute SetupSessionData sessionData) {
        setupSessionData.setDefaultLanguage(sessionData.getDefaultLanguage());
        setupSessionData.setAvailableLanguages(sessionData.getAvailableLanguages());
        setupSessionData.setDefaultTimezone(sessionData.getDefaultTimezone());
        setupSessionData.setAvailableTimezones(sessionData.getAvailableTimezones());
        return "redirect:/setup/step3";
    }

    /**
     * Step3：確認画面を表示する。
     *
     * @param model モデル
     * @return Step3テンプレート
     */
    @GetMapping("/step3")
    public String step3(Model model) {
        model.addAttribute("sessionData", setupSessionData);
        return "setup/step3";
    }

    /**
     * Step3：設定を保存して完了画面へ遷移する。
     *
     * @return 完了画面へリダイレクト
     */
    @PostMapping("/step3")
    public String step3Submit() {
        setupService.save(setupSessionData);
        return "redirect:/setup/complete";
    }

    /**
     * 完了画面を表示する。
     *
     * @return 完了テンプレート
     */
    @GetMapping("/complete")
    public String complete() {
        return "setup/complete";
    }

}