package io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.LocalizationConfigError;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.LocalizationConfigException;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.LanguageSetting;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.SystemConfig;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.TimezoneSetting;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.LocalizationSettingService;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.SystemConfigService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto.LocalizationDetailView;
import io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto.LocalizationEditForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * システム設定-言語・タイムゾーン設定のコントローラー
 *
 * <p>URL設計：
 * <ul>
 * <li>GET  /system/system-settings/localization      - 詳細表示</li>
 * <li>GET  /system/system-settings/localization/edit - 編集フォーム</li>
 * <li>POST /system/system-settings/localization      - 保存</li>
 * </ul>
 *
 * @author Jun Kobayashi
 */
@Controller
@RequestMapping("/system/system-settings/localization")
@RequiredArgsConstructor
public class LocalizationController {

    /** ロガー */
    private static final Logger log = LoggerFactory.getLogger(LocalizationController.class);

    /** 言語・タイムゾーン設定サービス */
    private final LocalizationSettingService localizationSettingService;

    /** システム設定サービス（監査情報取得用） */
    private final SystemConfigService systemConfigService;

    /** メッセージソース */
    private final MessageSource messageSource;

    /**
     * 言語・タイムゾーン設定の詳細画面を表示する。
     *
     * @param model モデル
     * @return ビュー名
     */
    @GetMapping
    public String detail(Model model) {
        LocalizationDetailView view = buildDetailView();
        model.addAttribute("activeMenu", "system-settings");
        model.addAttribute("localization", view);
        return "system/system-settings/localization/detail";
    }

    /**
     * 言語・タイムゾーン設定の編集フォームを表示する。
     *
     * @param model モデル
     * @return ビュー名
     */
    @GetMapping("/edit")
    public String editForm(Model model) {
        if (!model.containsAttribute("localizationEditForm")) {
            LocalizationEditForm form = new LocalizationEditForm();
            Optional<LanguageSetting> langOpt = safeGetLanguage();
            Optional<TimezoneSetting> tzOpt = safeGetTimezone();
            langOpt.ifPresent(lang -> {
                form.setDefaultLanguage(lang.getDefaultLanguage());
                form.setAvailableLanguages(
                        new java.util.ArrayList<>(lang.getAvailableLanguages()));
            });
            tzOpt.ifPresent(tz -> {
                form.setDefaultTimezone(tz.getDefaultTimezone());
                form.setAvailableTimezones(
                        new java.util.ArrayList<>(tz.getAvailableTimezones()));
            });
            model.addAttribute("localizationEditForm", form);
        }
        addTimezoneOptionsToModel(
                (LocalizationEditForm) model.getAttribute("localizationEditForm"),
                model);
        model.addAttribute("activeMenu", "system-settings");
        return "system/system-settings/localization/edit";
    }

    /**
     * 言語・タイムゾーン設定を保存する。
     *
     * <p>処理順序：
     * <ol>
     * <li>アノテーション検証（null/empty）</li>
     * <li>{@code LocalizationSettingService}でクロスフィールド検証＋保存</li>
     * <li>成功時：詳細画面へリダイレクト（PRGパターン）</li>
     * <li>失敗時：編集画面を再表示し、エラーメッセージを表示</li>
     * </ol>
     *
     * <p>言語とタイムゾーンを順次保存するため、片方が成功し片方が失敗する可能性がある。
     * バリデーションは保存前に両方実施し、両方OKであることを確認してから保存する。</p>
     *
     * @param form 編集フォーム
     * @param bindingResult バインディング結果
     * @param principal 認証済みプリンシパル
     * @param locale ロケール
     * @param redirectAttrs リダイレクト属性
     * @param model モデル
     * @return ビュー名（成功時はリダイレクト）
     */
    @PostMapping
    public String save(
            @Valid @ModelAttribute("localizationEditForm") LocalizationEditForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            RedirectAttributes redirectAttrs,
            Model model) {

        if (bindingResult.hasErrors()) {
            addTimezoneOptionsToModel(form, model);
            model.addAttribute("activeMenu", "system-settings");
            return "system/system-settings/localization/edit";
        }

        String updatedBy = (principal != null) ? principal.getAccountId() : "system";
        LanguageSetting language = new LanguageSetting(
                form.getDefaultLanguage(), form.getAvailableLanguages());
        TimezoneSetting timezone = new TimezoneSetting(
                form.getDefaultTimezone(), form.getAvailableTimezones());

        // 言語設定の保存
        try {
            localizationSettingService.saveLanguageSetting(language, updatedBy);
        } catch (LocalizationConfigException e) {
            handleConfigException(e, bindingResult, locale);
            addTimezoneOptionsToModel(form, model);
            model.addAttribute("activeMenu", "system-settings");
            return "system/system-settings/localization/edit";
        }

        // タイムゾーン設定の保存
        try {
            localizationSettingService.saveTimezoneSetting(timezone, updatedBy);
        } catch (LocalizationConfigException e) {
            handleConfigException(e, bindingResult, locale);
            addTimezoneOptionsToModel(form, model);
            model.addAttribute("activeMenu", "system-settings");
            return "system/system-settings/localization/edit";
        }

        log.info("言語・タイムゾーン設定を更新しました: updatedBy={}", updatedBy);
        redirectAttrs.addFlashAttribute("flashSuccessKey",
                "system.localization.form.success.updated");
        return "redirect:/system/system-settings/localization";
    }

    /**
     * 詳細表示用のViewを組み立てる。
     *
     * @return LocalizationDetailView
     */
    private LocalizationDetailView buildDetailView() {
        Optional<LanguageSetting> langOpt = safeGetLanguage();
        Optional<TimezoneSetting> tzOpt = safeGetTimezone();
        Optional<SystemConfig> langConfigOpt =
                systemConfigService.findLatestByKey(LocalizationSettingService.KEY_LANGUAGE);
        Optional<SystemConfig> tzConfigOpt =
                systemConfigService.findLatestByKey(LocalizationSettingService.KEY_TIMEZONE);

        return new LocalizationDetailView(
                langOpt.map(LanguageSetting::getDefaultLanguage).orElse(null),
                langOpt.map(LanguageSetting::getAvailableLanguages)
                        .orElse(java.util.Collections.emptyList()),
                tzOpt.map(TimezoneSetting::getDefaultTimezone).orElse(null),
                tzOpt.map(TimezoneSetting::getAvailableTimezones)
                        .orElse(java.util.Collections.emptyList()),
                langConfigOpt.map(SystemConfig::getVersion).orElse(null),
                langConfigOpt.map(SystemConfig::getCreatedBy).orElse(null),
                tzConfigOpt.map(SystemConfig::getVersion).orElse(null),
                tzConfigOpt.map(SystemConfig::getCreatedBy).orElse(null));
    }

    /**
     * 編集画面のタイムゾーン選択UI用データをモデルに追加する。
     *
     * @param form 編集フォーム（選択済み一覧の抽出元）
     * @param model モデル
     */
    private void addTimezoneOptionsToModel(LocalizationEditForm form, Model model) {
        List<Map<String, String>> tzList = SupportedTimezone.values().stream()
                .map(tz -> Map.of("id", tz.getId(), "displayName", tz.getDisplayName()))
                .toList();
        model.addAttribute("timezonesJson", tzList);

        List<SupportedTimezone> currentTz = (form != null && form.getAvailableTimezones() != null)
                ? form.getAvailableTimezones()
                : List.of();
        List<Map<String, String>> selectedTzList = currentTz.stream()
                .map(tz -> Map.of("id", tz.getId(), "displayName", tz.getDisplayName()))
                .toList();
        model.addAttribute("selectedTimezones", selectedTzList);
    }

    /**
     * 言語設定を安全に取得する（デシリアライズ失敗時は空Optionalを返す）
     *
     * @return 言語設定（取得失敗時は空）
     */
    private Optional<LanguageSetting> safeGetLanguage() {
        try {
            return localizationSettingService.getLanguageSetting();
        } catch (LocalizationConfigException e) {
            log.warn("言語設定の取得に失敗しました: error={}", e.getError(), e);
            return Optional.empty();
        }
    }

    /**
     * タイムゾーン設定を安全に取得する（デシリアライズ失敗時は空Optionalを返す）
     *
     * @return タイムゾーン設定（取得失敗時は空）
     */
    private Optional<TimezoneSetting> safeGetTimezone() {
        try {
            return localizationSettingService.getTimezoneSetting();
        } catch (LocalizationConfigException e) {
            log.warn("タイムゾーン設定の取得に失敗しました: error={}", e.getError(), e);
            return Optional.empty();
        }
    }

    /**
     * LocalizationConfigException を BindingResult に反映する。
     *
     * @param e 例外
     * @param bindingResult バインディング結果
     * @param locale ロケール
     */
    private void handleConfigException(
            LocalizationConfigException e,
            BindingResult bindingResult,
            Locale locale) {
        String errorMessage = messageSource.getMessage(
                "system.localization.error." + e.getError().name(),
                null,
                "Validation failed",
                locale);
        String fieldName = resolveFieldName(e.getError());
        if (fieldName != null) {
            bindingResult.rejectValue(fieldName, "localizationError", errorMessage);
        } else {
            bindingResult.reject("localizationError", errorMessage);
        }
    }

    /**
     * エラー種別から関連フィールド名を解決する。
     *
     * @param error エラー種別
     * @return フィールド名、または該当なしの場合null
     */
    private String resolveFieldName(LocalizationConfigError error) {
        return switch (error) {
            case DEFAULT_LANGUAGE_REQUIRED,
                 DEFAULT_LANGUAGE_NOT_IN_AVAILABLE -> "defaultLanguage";
            case AVAILABLE_LANGUAGES_EMPTY -> "availableLanguages";
            case DEFAULT_TIMEZONE_REQUIRED,
                 DEFAULT_TIMEZONE_NOT_IN_AVAILABLE -> "defaultTimezone";
            case AVAILABLE_TIMEZONES_EMPTY -> "availableTimezones";
            case SERIALIZATION_FAILED,
                 DESERIALIZATION_FAILED -> null;
        };
    }

}
