package io.github.kizulog_community.kizulog.infrastructure.web.setup;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import io.github.kizulog_community.kizulog.domain.setup.SetupService;
import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.OidcConnectionException;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.OidcProviderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * セットアップウィザードコントローラー
 *
 * <p>セットアップウィザードの画面遷移とフォーム処理を担う。</p>
 *
 * @author Jun Kobayashi
 */
@Controller
@RequestMapping("/setup")
@RequiredArgsConstructor
public class SetupController {

	/** MessageSource */
	private final MessageSource messageSource;

	/** LocaleResolver */
	private final SessionLocaleResolver localeResolver;

    /** セットアップサービス */
    private final SetupService setupService;

    /** セットアップセッションデータ */
    private final SetupSessionData setupSessionData;

    /** OIDCプロバイダーサービス */
    private final OidcProviderService oidcProviderService;

    /**
     * Step0：言語選択画面を表示する。
     *
     * @param model モデル
     * @return Step0テンプレート
     */
    @GetMapping("/step0")
    public String step0(Model model) {
        model.addAttribute("languages", Arrays.asList(SupportedLanguage.values()));
        model.addAttribute("sessionData", setupSessionData);
        return "setup/step0";
    }

    /**
     * Step0：言語選択を受け取りStep1へ遷移する。
     *
     * @param language 選択された言語
     * @param request  HTTPリクエスト
     * @param response HTTPレスポンス
     * @return Step1へリダイレクト
     */
    @PostMapping("/step0")
    public String step0Submit(
            @RequestParam SupportedLanguage language,
            HttpServletRequest request,
            HttpServletResponse response) {
        setupSessionData.setSetupLanguage(language);
        localeResolver.setLocaleContext(
                request, response,
                new SimpleLocaleContext(language.getLocale()));
        return "redirect:/setup/step1";
    }

    /**
     * Step1：OIDC設定画面を表示する。
     *
     * @param model モデル
     * @return Step1テンプレート
     */
    @GetMapping("/step1")
    public String step1(Model model) {
        if (setupSessionData.getSetupLanguage() == null) {
            return "redirect:/setup/step0";
        }
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
    public String step1Submit(@ModelAttribute Step1FormData formData) {
        setupSessionData.setOidcSettings(formData.getOidcSettings());
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
    public String step2Submit(@ModelAttribute Step2FormData formData) {
        setupSessionData.setDefaultLanguage(formData.getDefaultLanguage());
        setupSessionData.setAvailableLanguages(formData.getAvailableLanguages());
        setupSessionData.setDefaultTimezone(formData.getDefaultTimezone());
        setupSessionData.setAvailableTimezones(formData.getAvailableTimezones());
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

    /**
     * OIDC接続確認
     *
     * @param request リクエストボディ（issuerUri）
     * @param locale  ロケール
     * @return 接続確認結果
     */
    @PostMapping("/check-oidc")
    @ResponseBody
    public ResponseEntity<OidcCheckResult> checkOidc(
    		@RequestBody Map<String, String> request, Locale locale) {
    	try {
    		oidcProviderService.verify(request.get("issuerUri"));
    		return ResponseEntity.ok(new OidcCheckResult(
    				true, null, messageSource.getMessage("oidc.success", null, locale)));

    	} catch (OidcConnectionException e) {
    		String messageKey = switch (e.getErrorType()) {
            	case INPUT_ERROR      -> "oidc.error.input";
            	case CONNECTION_ERROR -> "oidc.error.connection";
            	case INVALID_RESPONSE -> "oidc.error.invalid_response";
            	case UNEXPECTED_ERROR -> "oidc.error.unexpected";
    	};
    	
        return ResponseEntity.ok(new OidcCheckResult(
        		false, e.getErrorType().name(),
                messageSource.getMessage(messageKey, null, locale)));
    	}
    }

}