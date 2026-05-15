package io.github.kizulog_community.kizulog.infrastructure.web.system.myprofile;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.exception.AccountLocalizationError;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.exception.AccountLocalizationException;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.model.SystemAccountLocalization;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.service.AccountLocalizationApplicationService;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.service.SystemAccountLocalizationService;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.LanguageSetting;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.TimezoneSetting;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.LocalizationSettingService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import io.github.kizulog_community.kizulog.infrastructure.web.system.myprofile.dto.MyLocalizationDetailView;
import io.github.kizulog_community.kizulog.infrastructure.web.system.myprofile.dto.MyLocalizationEditForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * マイページ言語・タイムゾーン設定のコントローラー
 *
 * <p>URL設計：
 * <ul>
 * <li>GET  /system/my-profile/localization       - 詳細表示</li>
 * <li>GET  /system/my-profile/localization/edit  - 編集フォーム</li>
 * <li>POST /system/my-profile/localization       - 保存</li>
 * </ul>
 *
 * @author Jun Kobayashi
 */
@Controller
@RequestMapping("/system/my-profile/localization")
@RequiredArgsConstructor
public class MyLocalizationController {

    /** ロガー */
    private static final Logger log = LoggerFactory.getLogger(MyLocalizationController.class);

    /** 自動作成時の作成者識別子 */
    private static final String AUTO_RECOVERY_CREATED_BY = "system:auto-recovery";

    /** アカウント単位localizationサービス */
    private final AccountLocalizationApplicationService accountLocalizationApplicationService;

    /** localization読み取り用サービス（実値取得） */
    private final SystemAccountLocalizationService systemAccountLocalizationService;

    /** 選択肢生成用：システム側設定 */
    private final LocalizationSettingService localizationSettingService;

    /** ロケールリゾルバ（セッション即時反映用） */
    private final SessionLocaleResolver localeResolver;

    /** メッセージソース */
    private final MessageSource messageSource;

    /**
     * 言語・タイムゾーン設定の詳細画面を表示する。
     *
     * @param principal 認証済みプリンシパル
     * @param model モデル
     * @return ビュー名
     */
    @GetMapping
    public String detail(
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Model model) {

        String accountId = principal.getAccountId();
        SystemAccountLocalization localization = getOrCreateLocalization(accountId);

        MyLocalizationDetailView view = new MyLocalizationDetailView(
                localization.getLanguage(),
                localization.getTimezone(),
                localization.getVersion(),
                localization.getCreatedBy());

        model.addAttribute("activeMenu", "my-profile");
        model.addAttribute("localization", view);
        return "system/my-profile/localization/detail";
    }

    /**
     * 言語・タイムゾーン設定の編集フォームを表示する。
     *
     * @param principal 認証済みプリンシパル
     * @param model モデル
     * @return ビュー名
     */
    @GetMapping("/edit")
    public String editForm(
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Model model) {

        String accountId = principal.getAccountId();
        SystemAccountLocalization localization = getOrCreateLocalization(accountId);

        if (!model.containsAttribute("myLocalizationEditForm")) {
            MyLocalizationEditForm form = new MyLocalizationEditForm();
            form.setLanguage(localization.getLanguage());
            form.setTimezone(localization.getTimezone());
            model.addAttribute("myLocalizationEditForm", form);
        }
        addChoicesToModel(model);
        model.addAttribute("activeMenu", "my-profile");
        return "system/my-profile/localization/edit";
    }

    /**
     * 言語・タイムゾーン設定を保存する。
     *
     * @param form 編集フォーム
     * @param bindingResult バインディング結果
     * @param principal 認証済みプリンシパル
     * @param locale ロケール
     * @param request HTTPリクエスト（セッション反映用）
     * @param response HTTPレスポンス（セッション反映用）
     * @param redirectAttrs リダイレクト属性
     * @param model モデル
     * @return ビュー名（成功時はリダイレクト）
     */
    @PostMapping
    public String save(
            @Valid @ModelAttribute("myLocalizationEditForm") MyLocalizationEditForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttrs,
            Model model) {

        if (bindingResult.hasErrors()) {
            addChoicesToModel(model);
            model.addAttribute("activeMenu", "my-profile");
            return "system/my-profile/localization/edit";
        }

        String accountId = principal.getAccountId();
        try {
            accountLocalizationApplicationService.saveAccountLocalization(
                    accountId, form.getLanguage(), form.getTimezone(), accountId);
        } catch (AccountLocalizationException e) {
            handleLocalizationException(e, bindingResult, locale);
            addChoicesToModel(model);
            model.addAttribute("activeMenu", "my-profile");
            return "system/my-profile/localization/edit";
        }

        applyToSession(request, response, form.getLanguage(), form.getTimezone());

        log.info("アカウントの言語・タイムゾーン設定を更新しました: "
                        + "accountId={}, language={}, timezone={}",
                accountId, form.getLanguage().getCode(), form.getTimezone().getId());

        redirectAttrs.addFlashAttribute("flashSuccessKey",
                "system.my-profile.localization.form.success.updated");
        return "redirect:/system/my-profile/localization";
    }

    /**
     * アカウントlocalizationを取得する。未設定時はシステムデフォルトで自動作成する。
     *
     * @param accountId アカウントID
     * @return アカウントlocalization（必ず非null）
     * @throws AccountLocalizationException 自動作成も失敗した場合（システム側設定未登録等）
     */
    private SystemAccountLocalization getOrCreateLocalization(String accountId) {
        Optional<SystemAccountLocalization> opt =
                systemAccountLocalizationService.getLocalization(accountId);
        if (opt.isPresent()) {
            return opt.get();
        }

        log.warn("アカウントのlocalizationが未作成のためシステムデフォルトで自動作成します: "
                + "accountId={}", accountId);
        accountLocalizationApplicationService.createDefaultLocalizationForAccount(
                accountId, AUTO_RECOVERY_CREATED_BY);

        return systemAccountLocalizationService.getLocalization(accountId)
                .orElseThrow(() -> new IllegalStateException(
                        "自動作成後もlocalizationが取得できません: accountId=" + accountId));
    }

    /**
     * セッションのLocale/TimeZoneを反映する。
     *
     * @param request HTTPリクエスト
     * @param response HTTPレスポンス
     * @param language 言語
     * @param timezone タイムゾーン
     */
    private void applyToSession(
            HttpServletRequest request,
            HttpServletResponse response,
            SupportedLanguage language,
            SupportedTimezone timezone) {
        TimeZone tz = TimeZone.getTimeZone(timezone.getZoneId());
        localeResolver.setLocaleContext(
                request, response,
                new SimpleTimeZoneAwareLocaleContext(language.getLocale(), tz));
    }

    /**
     * 編集画面用の選択肢（AVAILABLE範囲）をモデルに追加する。
     *
     * @param model モデル
     */
    private void addChoicesToModel(Model model) {
        Optional<LanguageSetting> langOpt = localizationSettingService.getLanguageSetting();
        Optional<TimezoneSetting> tzOpt = localizationSettingService.getTimezoneSetting();

        List<SupportedLanguage> languageChoices = langOpt
                .map(LanguageSetting::getAvailableLanguages)
                .orElse(List.of());
        List<SupportedTimezone> timezoneChoices = tzOpt
                .map(TimezoneSetting::getAvailableTimezones)
                .orElse(List.of());

        model.addAttribute("languageChoices", languageChoices);

        // 各言語のdisplay用にMap化（id, displayName）
        List<Map<String, String>> langOptions = languageChoices.stream()
                .map(l -> Map.of("code", l.name(), "displayName", l.getDisplayName()))
                .toList();
        model.addAttribute("languageOptions", langOptions);

        // TZの選択肢
        List<Map<String, String>> tzOptions = timezoneChoices.stream()
                .map(tz -> Map.of("id", tz.getId(), "displayName", tz.getDisplayName()))
                .toList();
        model.addAttribute("timezoneOptions", tzOptions);
    }

    /**
     * AccountLocalizationException を BindingResult に反映する。
     *
     * @param e 例外
     * @param bindingResult バインディング結果
     * @param locale ロケール
     */
    private void handleLocalizationException(
            AccountLocalizationException e,
            BindingResult bindingResult,
            Locale locale) {
        String key = "system.my-profile.localization.error." + e.getError().name();
        String errorMessage = messageSource.getMessage(
                key, null, "Validation failed", locale);
        String fieldName = resolveFieldName(e.getError());
        if (fieldName != null) {
            bindingResult.rejectValue(fieldName, "myLocalizationError", errorMessage);
        } else {
            bindingResult.reject("myLocalizationError", errorMessage);
        }
    }

    /**
     * エラー種別から関連フィールド名を解決する。
     *
     * @param error エラー種別
     * @return フィールド名、または該当なしの場合null（グローバルエラー扱い）
     */
    private String resolveFieldName(AccountLocalizationError error) {
        return switch (error) {
            case LANGUAGE_REQUIRED,
                 LANGUAGE_NOT_AVAILABLE -> "language";
            case TIMEZONE_REQUIRED,
                 TIMEZONE_NOT_AVAILABLE -> "timezone";
            case SYSTEM_LOCALIZATION_NOT_CONFIGURED -> null;
        };
    }

}
