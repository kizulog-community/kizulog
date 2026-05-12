package io.github.kizulog_community.kizulog.infrastructure.web.setup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import com.nimbusds.jwt.JWTClaimsSet;

import io.github.kizulog_community.kizulog.domain.setup.SetupService;
import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.OidcConnectionException;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.OidcProviderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * セットアップウィザードコントローラー
 *
 * @author Jun Kobayashi
 */
@Controller
@RequestMapping("/setup")
@RequiredArgsConstructor
public class SetupController {

    /** MessageSource */
    private final MessageSource messageSource;

    /** LocaleResolve */
    private final SessionLocaleResolver localeResolver;

    /** セットアップサービス */
    private final SetupService setupService;

    /** セットアップセッションデータ */
    private final SetupSessionData setupSessionData;

    /** OIDCプロバイダーサービス */
    private final OidcProviderService oidcProviderService;

    /** ロガー */
    private static final Logger log = LoggerFactory.getLogger(SetupController.class);

    /**
     * Step0：言語選択画面を表示
     *
     * @param model モデル
     * @return Step0テンプレート
     */
    @GetMapping("/step0")
    public String step0(Model model) {
        model.addAttribute("languages", Arrays.asList(SupportedLanguage.values()));
        model.addAttribute("sessionData", setupSessionData);
        if (!model.containsAttribute("step0FormData")) {
            Step0FormData formData = new Step0FormData();
            if (setupSessionData.getSetupLanguage() != null) {
                formData.setLanguage(setupSessionData.getSetupLanguage());
            }
            model.addAttribute("step0FormData", formData);
        }
        return "setup/step0";
    }

    /**
     * Step0：言語選択を受け取りStep1へ遷移
     */
    @PostMapping("/step0")
    public String step0Submit(
            @Valid @ModelAttribute("step0FormData") Step0FormData formData,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("languages", Arrays.asList(SupportedLanguage.values()));
            model.addAttribute("sessionData", setupSessionData);
            return "setup/step0";
        }
        SupportedLanguage language = formData.getLanguage();
        setupSessionData.setSetupLanguage(language);
        localeResolver.setLocaleContext(
                request, response,
                new SimpleLocaleContext(language.getLocale()));
        return "redirect:/setup/step1";
    }

    /**
     * Step1：デフォルト設定画面を表示
     */
    @GetMapping("/step1")
    public String step1(Model model) {
        if (setupSessionData.getSetupLanguage() == null) {
            return "redirect:/setup/step0";
        }
        model.addAttribute("sessionData", setupSessionData);
        model.addAttribute("languages", Arrays.asList(SupportedLanguage.values()));

        List<Map<String, String>> tzList = SupportedTimezone.values().stream()
                .map(tz -> Map.of("id", tz.getId(), "displayName", tz.getDisplayName()))
                .toList();
        model.addAttribute("timezonesJson", tzList);

        List<Map<String, String>> selectedTzList = setupSessionData.getAvailableTimezones().stream()
                .map(tz -> Map.of("id", tz.getId(), "displayName", tz.getDisplayName()))
                .toList();
        model.addAttribute("selectedTimezones", selectedTzList);

        if (!model.containsAttribute("step1FormData")) {
            Step1FormData formData = new Step1FormData();
            if (setupSessionData.getDefaultLanguage() != null) {
                formData.setDefaultLanguage(setupSessionData.getDefaultLanguage());
            }
            if (setupSessionData.getAvailableLanguages() != null) {
                formData.setAvailableLanguages(
                        new ArrayList<>(setupSessionData.getAvailableLanguages()));
            }
            if (setupSessionData.getDefaultTimezone() != null) {
                formData.setDefaultTimezone(setupSessionData.getDefaultTimezone());
            }
            if (setupSessionData.getAvailableTimezones() != null) {
                formData.setAvailableTimezones(
                        new ArrayList<>(setupSessionData.getAvailableTimezones()));
            }
            model.addAttribute("step1FormData", formData);
        }

        return "setup/step1";
    }

    /**
     * Step1：デフォルト設定を受け取りStep2へ遷移
     */
    @PostMapping("/step1")
    public String step1Submit(
            @Valid @ModelAttribute("step1FormData") Step1FormData formData,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("sessionData", setupSessionData);
            model.addAttribute("languages", Arrays.asList(SupportedLanguage.values()));
            List<Map<String, String>> tzList = SupportedTimezone.values().stream()
                    .map(tz -> Map.of("id", tz.getId(), "displayName", tz.getDisplayName()))
                    .toList();
            model.addAttribute("timezonesJson", tzList);
            List<SupportedTimezone> currentTz = formData.getAvailableTimezones() != null
                    ? formData.getAvailableTimezones()
                    : List.of();
            List<Map<String, String>> selectedTzList = currentTz.stream()
                    .map(tz -> Map.of("id", tz.getId(), "displayName", tz.getDisplayName()))
                    .toList();
            model.addAttribute("selectedTimezones", selectedTzList);
            return "setup/step1";
        }

        setupSessionData.setDefaultLanguage(formData.getDefaultLanguage());
        setupSessionData.setAvailableLanguages(formData.getAvailableLanguages());
        setupSessionData.setDefaultTimezone(formData.getDefaultTimezone());
        setupSessionData.setAvailableTimezones(formData.getAvailableTimezones());
        return "redirect:/setup/step2";
    }

    /**
     * Step2：システム管理OIDC設定画面を表示
     */
    @GetMapping("/step2")
    public String step2(Model model, HttpServletRequest request) {
        if (setupSessionData.getHost() == null) {
            setupSessionData.setHost(request.getServerName());
        }
        if (setupSessionData.getOidcSettings().isEmpty()) {
            setupSessionData.getOidcSettings().add(new OidcSetting());
        }
        model.addAttribute("sessionData", setupSessionData);

        if (!model.containsAttribute("step2FormData")) {
            Step2FormData formData = new Step2FormData();
            formData.setHost(setupSessionData.getHost());
            // セッションから既存OIDC設定を復元
            OidcSetting existing = setupSessionData.getOidcSettings().get(0);
            OidcSetting form = new OidcSetting();
            form.setUri(existing.getUri());
            form.setClientId(existing.getClientId());
            form.setClientSecret(existing.getClientSecret());
            formData.setOidcSetting(form);
            model.addAttribute("step2FormData", formData);
        }

        return "setup/step2";
    }

    /**
     * Step2：OIDC設定を受け取りStep3へ遷移
     */
    @PostMapping("/step2")
    public String step2Submit(
            @Valid @ModelAttribute("step2FormData") Step2FormData formData,
            BindingResult bindingResult,
            Locale locale,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("sessionData", setupSessionData);
            return "setup/step2";
        }

        // サーバー側でもOIDC接続確認を実施
        try {
            oidcProviderService.verify(formData.getOidcSetting().getUri());
        } catch (OidcConnectionException e) {
            log.warn("Step2 OIDC接続確認失敗: uri={}, errorType={}",
                    formData.getOidcSetting().getUri(), e.getErrorType());
            String messageKey = switch (e.getErrorType()) {
                case INPUT_ERROR -> "oidc.error.input";
                case CONNECTION_ERROR -> "oidc.error.connection";
                case INVALID_RESPONSE -> "oidc.error.invalid_response";
                case UNEXPECTED_ERROR -> "oidc.error.unexpected";
            };
            String errMsg = messageSource.getMessage(messageKey, null, locale);
            bindingResult.reject("oidc.connection.failed", errMsg);
            model.addAttribute("sessionData", setupSessionData);
            return "setup/step2";
        }

        setupSessionData.setHost(formData.getHost());

        OidcSetting oidcSetting = formData.getOidcSetting();
        oidcSetting.setId("master");

        setupSessionData.getOidcSettings().clear();
        setupSessionData.getOidcSettings().add(oidcSetting);
        return "redirect:/setup/step3";
    }

    /**
     * Step3：管理者OIDCログイン画面を表示
     */
    @GetMapping("/step3")
    public String step3(Model model) {
        model.addAttribute("sessionData", setupSessionData);
        return "setup/step3";
    }

    /**
     * Step4：確認画面を表示
     */
    @GetMapping("/step4")
    public String step4(Model model) {
        model.addAttribute("sessionData", setupSessionData);
        return "setup/step4";
    }

    /**
     * Step4：設定を保存して完了画面へ遷移
     */
    @PostMapping("/step4")
    public String step4Submit() {
        setupService.save(setupSessionData);
        return "redirect:/setup/complete";
    }

    /**
     * 完了画面を表示
     */
    @GetMapping("/complete")
    public String complete() {
        return "setup/complete";
    }

    /**
     * OIDC接続確認
     */
    @PostMapping("/check-oidc")
    @ResponseBody
    public ResponseEntity<OidcCheckResult> checkOidc(
            @RequestBody Map<String, String> request, Locale locale) {
        try {
            oidcProviderService.verify(request.get("issuerUri"));
            return ResponseEntity.ok(new OidcCheckResult(
                    true, null,
                    messageSource.getMessage("oidc.success", null, locale)));
        } catch (OidcConnectionException e) {
            String messageKey = switch (e.getErrorType()) {
                case INPUT_ERROR -> "oidc.error.input";
                case CONNECTION_ERROR -> "oidc.error.connection";
                case INVALID_RESPONSE -> "oidc.error.invalid_response";
                case UNEXPECTED_ERROR -> "oidc.error.unexpected";
            };
            return ResponseEntity.ok(new OidcCheckResult(
                    false, e.getErrorType().name(),
                    messageSource.getMessage(messageKey, null, locale)));
        }
    }

    /**
     * OIDCログイン
     */
    @GetMapping("/oidc-login")
    public String oidcLogin(HttpServletRequest request, HttpSession session) {
        OidcSetting oidcSetting = setupSessionData.getOidcSettings().get(0);
        Map<String, Object> metadata = oidcProviderService.getMetadata(oidcSetting.getUri());

        String authorizationEndpoint = (String) metadata.get("authorization_endpoint");
        String state = oidcProviderService.generateState();
        session.setAttribute("oidc_state", state);

        String redirectUri = request.getScheme() + "://"
                + request.getServerName()
                + (request.getServerPort() != 80 && request.getServerPort() != 443
                        ? ":" + request.getServerPort() : "")
                + "/setup/callback";
        session.setAttribute("oidc_redirect_uri", redirectUri);

        String authUrl = oidcProviderService.buildAuthorizationUrl(
                authorizationEndpoint, oidcSetting.getClientId()
                , redirectUri, state);

        return "redirect:" + authUrl;
    }

    /**
     * OIDCコールバック
     */
    @GetMapping("/callback")
    public String oidcCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpSession session) {

        String savedState = (String) session.getAttribute("oidc_state");
        if (!state.equals(savedState)) {
            log.warn("state不一致: expected={}, actual={}", savedState, state);
            return "redirect:/setup/step3?error=state_mismatch";
        }

        OidcSetting oidcSetting = setupSessionData.getOidcSettings().get(0);
        Map<String, Object> metadata = oidcProviderService.getMetadata(
                oidcSetting.getUri());
        String tokenEndpoint = (String) metadata.get("token_endpoint");
        String redirectUri = (String) session.getAttribute("oidc_redirect_uri");

        JWTClaimsSet claims = oidcProviderService.exchangeCodeForClaims(
                tokenEndpoint,
                code,
                oidcSetting.getClientId(),
                oidcSetting.getClientSecret(),
                redirectUri);

        setupSessionData.setAdminIss(claims.getIssuer());
        setupSessionData.setAdminAud(oidcSetting.getClientId());
        setupSessionData.setAdminSub(claims.getSubject());

        session.removeAttribute("oidc_state");
        session.removeAttribute("oidc_redirect_uri");

        return "redirect:/setup/step3";
    }

}