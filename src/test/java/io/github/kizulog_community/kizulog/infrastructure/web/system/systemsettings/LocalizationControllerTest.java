package io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.service.AccountLocalizationApplicationService;
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

/**
 * LocalizationControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class LocalizationControllerTest {

    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    private LocalizationSettingService localizationSettingService;
    private SystemConfigService systemConfigService;
    private AccountLocalizationApplicationService accountLocalizationApplicationService;
    private MessageSource messageSource;
    private LocalizationController controller;

    private Model model;
    private RedirectAttributes redirectAttrs;
    private SystemUserPrincipal principal;

    @BeforeEach
    void setUp() {
        localizationSettingService = mock(LocalizationSettingService.class);
        systemConfigService = mock(SystemConfigService.class);
        accountLocalizationApplicationService =
                mock(AccountLocalizationApplicationService.class);
        messageSource = mock(MessageSource.class);
        controller = new LocalizationController(
                localizationSettingService,
                systemConfigService,
                accountLocalizationApplicationService,
                messageSource);

        model = new ConcurrentModel();
        redirectAttrs = new RedirectAttributesModelMap();
        principal = mock(SystemUserPrincipal.class);
        when(principal.getAccountId()).thenReturn("admin-account-id");
        when(messageSource.getMessage(any(String.class), any(), any(), any(Locale.class)))
                .thenReturn("dummy message");
    }

    private LanguageSetting languageSettingJa() {
        return new LanguageSetting(
                SupportedLanguage.JA,
                List.of(SupportedLanguage.JA, SupportedLanguage.EN));
    }

    private TimezoneSetting timezoneSettingTokyo() {
        return new TimezoneSetting(
                SupportedTimezone.of("Asia/Tokyo"),
                List.of(SupportedTimezone.of("Asia/Tokyo")));
    }

    private SystemConfig languageConfig() {
        return new SystemConfig("LANGUAGE", BASE_TIME,
                "{\"DEFAULT\":\"ja\",\"AVAILABLE\":[\"ja\",\"en\"]}",
                BASE_TIME, "system:setup-wizard");
    }

    private SystemConfig timezoneConfig() {
        return new SystemConfig("TIMEZONE", BASE_TIME,
                "{\"DEFAULT\":\"Asia/Tokyo\",\"AVAILABLE\":[\"Asia/Tokyo\"]}",
                BASE_TIME, "system:setup-wizard");
    }

    @Test
    @DisplayName("detail: 言語・タイムゾーン共に保存済の場合、両方の値がmodelに設定される")
    void detail_returnsBothConfigured() {
        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(languageSettingJa()));
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(timezoneSettingTokyo()));
        when(systemConfigService.findLatestByKey("LANGUAGE"))
                .thenReturn(Optional.of(languageConfig()));
        when(systemConfigService.findLatestByKey("TIMEZONE"))
                .thenReturn(Optional.of(timezoneConfig()));

        String view = controller.detail(model);

        assertThat(view).isEqualTo("system/system-settings/localization/detail");
        LocalizationDetailView v = (LocalizationDetailView) model.getAttribute("localization");
        assertThat(v).isNotNull();
        assertThat(v.isLanguageConfigured()).isTrue();
        assertThat(v.isTimezoneConfigured()).isTrue();
        assertThat(v.getDefaultLanguage()).isEqualTo(SupportedLanguage.JA);
        assertThat(v.getDefaultTimezone().getId()).isEqualTo("Asia/Tokyo");
        assertThat(v.getLanguageCreatedBy()).isEqualTo("system:setup-wizard");
        assertThat(v.getTimezoneCreatedBy()).isEqualTo("system:setup-wizard");
        assertThat(model.getAttribute("activeMenu")).isEqualTo("system-settings");
    }

    @Test
    @DisplayName("detail: 未保存の場合、未設定のViewが返る")
    void detail_returnsEmptyView_whenNotConfigured() {
        when(localizationSettingService.getLanguageSetting()).thenReturn(Optional.empty());
        when(localizationSettingService.getTimezoneSetting()).thenReturn(Optional.empty());
        when(systemConfigService.findLatestByKey("LANGUAGE")).thenReturn(Optional.empty());
        when(systemConfigService.findLatestByKey("TIMEZONE")).thenReturn(Optional.empty());

        controller.detail(model);

        LocalizationDetailView v = (LocalizationDetailView) model.getAttribute("localization");
        assertThat(v.isLanguageConfigured()).isFalse();
        assertThat(v.isTimezoneConfigured()).isFalse();
        assertThat(v.getDefaultLanguage()).isNull();
        assertThat(v.getDefaultTimezone()).isNull();
    }

    @Test
    @DisplayName("detail: 言語のみ保存済の場合、TZ側は未設定で表示")
    void detail_languageOnlyConfigured() {
        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(languageSettingJa()));
        when(localizationSettingService.getTimezoneSetting()).thenReturn(Optional.empty());
        when(systemConfigService.findLatestByKey("LANGUAGE"))
                .thenReturn(Optional.of(languageConfig()));
        when(systemConfigService.findLatestByKey("TIMEZONE")).thenReturn(Optional.empty());

        controller.detail(model);

        LocalizationDetailView v = (LocalizationDetailView) model.getAttribute("localization");
        assertThat(v.isLanguageConfigured()).isTrue();
        assertThat(v.isTimezoneConfigured()).isFalse();
    }

    @Test
    @DisplayName("detail: 言語デシリアライズ失敗時は未設定扱い(防御的)")
    void detail_languageDeserializationFailedTreatedAsEmpty() {
        when(localizationSettingService.getLanguageSetting())
                .thenThrow(new LocalizationConfigException(
                        LocalizationConfigError.DESERIALIZATION_FAILED));
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(timezoneSettingTokyo()));
        when(systemConfigService.findLatestByKey("LANGUAGE"))
                .thenReturn(Optional.of(languageConfig()));
        when(systemConfigService.findLatestByKey("TIMEZONE"))
                .thenReturn(Optional.of(timezoneConfig()));

        controller.detail(model);

        LocalizationDetailView v = (LocalizationDetailView) model.getAttribute("localization");
        assertThat(v.isLanguageConfigured()).isFalse();
        assertThat(v.isTimezoneConfigured()).isTrue();
    }

    @Test
    @DisplayName("editForm: 既存値がない場合、空フォームと timezonesJson/selectedTimezones を設定")
    void editForm_emptyForm_whenNothingConfigured() {
        when(localizationSettingService.getLanguageSetting()).thenReturn(Optional.empty());
        when(localizationSettingService.getTimezoneSetting()).thenReturn(Optional.empty());

        String view = controller.editForm(model);

        assertThat(view).isEqualTo("system/system-settings/localization/edit");
        LocalizationEditForm form =
                (LocalizationEditForm) model.getAttribute("localizationEditForm");
        assertThat(form).isNotNull();
        assertThat(form.getDefaultLanguage()).isNull();
        assertThat(form.getAvailableLanguages()).isEmpty();
        assertThat(form.getDefaultTimezone()).isNull();
        assertThat(form.getAvailableTimezones()).isEmpty();

        assertThat(model.getAttribute("timezonesJson")).isNotNull();
        assertThat(model.getAttribute("selectedTimezones")).isNotNull();
        assertThat(model.getAttribute("activeMenu")).isEqualTo("system-settings");
    }

    @Test
    @DisplayName("editForm: 既存値がある場合、その値で初期化される")
    void editForm_prefilledForm_whenConfigured() {
        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(languageSettingJa()));
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(timezoneSettingTokyo()));

        controller.editForm(model);

        LocalizationEditForm form =
                (LocalizationEditForm) model.getAttribute("localizationEditForm");
        assertThat(form.getDefaultLanguage()).isEqualTo(SupportedLanguage.JA);
        assertThat(form.getAvailableLanguages())
                .containsExactly(SupportedLanguage.JA, SupportedLanguage.EN);
        assertThat(form.getDefaultTimezone().getId()).isEqualTo("Asia/Tokyo");
        assertThat(form.getAvailableTimezones())
                .extracting(SupportedTimezone::getId)
                .containsExactly("Asia/Tokyo");
    }

    @Test
    @DisplayName("editForm: timezonesJson は List<Map> 形式")
    void editForm_timezonesJsonIsListOfMaps() {
        when(localizationSettingService.getLanguageSetting()).thenReturn(Optional.empty());
        when(localizationSettingService.getTimezoneSetting()).thenReturn(Optional.empty());

        controller.editForm(model);

        @SuppressWarnings("unchecked")
        List<Object> tzList = (List<Object>) model.getAttribute("timezonesJson");
        assertThat(tzList).isNotEmpty();
        // 各要素は Map<String, String> (id, displayName)
        Object firstTz = tzList.get(0);
        assertThat(firstTz).isInstanceOf(java.util.Map.class);
    }

    @Test
    @DisplayName("editForm: モデルに既存formがある場合は上書きしない(flash復元)")
    void editForm_doesNotOverwriteExistingForm() {
        LocalizationEditForm existingForm = new LocalizationEditForm();
        existingForm.setDefaultLanguage(SupportedLanguage.EN);
        model.addAttribute("localizationEditForm", existingForm);

        controller.editForm(model);

        LocalizationEditForm form =
                (LocalizationEditForm) model.getAttribute("localizationEditForm");
        assertThat(form).isSameAs(existingForm);
        // 既存formでも timezonesJson は設定される
        assertThat(model.getAttribute("timezonesJson")).isNotNull();
    }

    @Test
    @DisplayName("save: 正常系で言語・TZが保存され詳細画面へリダイレクト")
    void save_success_redirectsToDetail() {
        LocalizationEditForm form = new LocalizationEditForm();
        form.setDefaultLanguage(SupportedLanguage.JA);
        form.setAvailableLanguages(List.of(SupportedLanguage.JA));
        form.setDefaultTimezone(SupportedTimezone.of("Asia/Tokyo"));
        form.setAvailableTimezones(List.of(SupportedTimezone.of("Asia/Tokyo")));
        BindingResult br = new BeanPropertyBindingResult(form, "localizationEditForm");

        // 現在の設定をモック（除外対象なしのため使用中チェックは通過）
        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(new LanguageSetting(
                        SupportedLanguage.JA, List.of(SupportedLanguage.JA))));
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(timezoneSettingTokyo()));

        String view = controller.save(
                form, br, principal, Locale.ENGLISH, redirectAttrs, model);

        assertThat(view).isEqualTo("redirect:/system/system-settings/localization");
        assertThat(redirectAttrs.getFlashAttributes())
                .containsKey("flashSuccessKey");
        verify(localizationSettingService).saveLanguageSetting(
                any(LanguageSetting.class), any(String.class));
        verify(localizationSettingService).saveTimezoneSetting(
                any(TimezoneSetting.class), any(String.class));
    }

    @Test
    @DisplayName("save: principalがnullの場合、createdBy='system' で保存")
    void save_principalNull_usesSystemAsCreatedBy() {
        LocalizationEditForm form = new LocalizationEditForm();
        form.setDefaultLanguage(SupportedLanguage.JA);
        form.setAvailableLanguages(List.of(SupportedLanguage.JA));
        form.setDefaultTimezone(SupportedTimezone.of("Asia/Tokyo"));
        form.setAvailableTimezones(List.of(SupportedTimezone.of("Asia/Tokyo")));
        BindingResult br = new BeanPropertyBindingResult(form, "localizationEditForm");

        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(new LanguageSetting(
                        SupportedLanguage.JA, List.of(SupportedLanguage.JA))));
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(timezoneSettingTokyo()));

        org.mockito.ArgumentCaptor<String> captor =
                org.mockito.ArgumentCaptor.forClass(String.class);

        controller.save(form, br, null, Locale.ENGLISH, redirectAttrs, model);

        verify(localizationSettingService).saveLanguageSetting(
                any(LanguageSetting.class), captor.capture());
        assertThat(captor.getValue()).isEqualTo("system");
    }

    @Test
    @DisplayName("save: 現在の設定が未登録（新規時）は使用中チェックスキップで保存される")
    void save_currentSettingNotConfigured_skipsInUseCheckAndSaves() {
        LocalizationEditForm form = new LocalizationEditForm();
        form.setDefaultLanguage(SupportedLanguage.JA);
        form.setAvailableLanguages(List.of(SupportedLanguage.JA));
        form.setDefaultTimezone(SupportedTimezone.of("Asia/Tokyo"));
        form.setAvailableTimezones(List.of(SupportedTimezone.of("Asia/Tokyo")));
        BindingResult br = new BeanPropertyBindingResult(form, "localizationEditForm");

        when(localizationSettingService.getLanguageSetting()).thenReturn(Optional.empty());
        when(localizationSettingService.getTimezoneSetting()).thenReturn(Optional.empty());

        String view = controller.save(
                form, br, principal, Locale.ENGLISH, redirectAttrs, model);

        assertThat(view).isEqualTo("redirect:/system/system-settings/localization");
        // 現在の設定が無い → ACTIVE件数取得は呼ばれない
        verify(accountLocalizationApplicationService, never())
                .countActiveAccountsUsingLanguage(any());
        verify(accountLocalizationApplicationService, never())
                .countActiveAccountsUsingTimezone(any());
    }

    @Test
    @DisplayName("save: BindingResultにエラーがある場合、編集画面を再表示")
    void save_bindingErrors_returnsEditView() {
        LocalizationEditForm form = new LocalizationEditForm();
        BindingResult br = new BeanPropertyBindingResult(form, "localizationEditForm");
        br.reject("error", "test error");

        String view = controller.save(
                form, br, principal, Locale.ENGLISH, redirectAttrs, model);

        assertThat(view).isEqualTo("system/system-settings/localization/edit");
        assertThat(model.getAttribute("timezonesJson")).isNotNull();
        verify(localizationSettingService, never())
                .saveLanguageSetting(any(LanguageSetting.class), any(String.class));
    }

    @Test
    @DisplayName("save: 言語バリデーション失敗時はフィールドエラー登録＋編集画面に戻る")
    void save_languageValidationFails_addsFieldErrorAndReturnsEdit() {
        LocalizationEditForm form = new LocalizationEditForm();
        form.setDefaultLanguage(SupportedLanguage.JA);
        form.setAvailableLanguages(List.of(SupportedLanguage.EN)); // 不整合（クロスフィールド）
        form.setDefaultTimezone(SupportedTimezone.of("Asia/Tokyo"));
        form.setAvailableTimezones(List.of(SupportedTimezone.of("Asia/Tokyo")));
        BindingResult br = new BeanPropertyBindingResult(form, "localizationEditForm");

        when(localizationSettingService.getLanguageSetting()).thenReturn(Optional.empty());
        when(localizationSettingService.getTimezoneSetting()).thenReturn(Optional.empty());

        doThrow(new LocalizationConfigException(
                LocalizationConfigError.DEFAULT_LANGUAGE_NOT_IN_AVAILABLE))
                .when(localizationSettingService)
                .saveLanguageSetting(any(LanguageSetting.class), any(String.class));

        String view = controller.save(
                form, br, principal, Locale.ENGLISH, redirectAttrs, model);

        assertThat(view).isEqualTo("system/system-settings/localization/edit");
        assertThat(br.hasFieldErrors("defaultLanguage")).isTrue();
        assertThat(model.getAttribute("timezonesJson")).isNotNull();
        verify(localizationSettingService, never())
                .saveTimezoneSetting(any(TimezoneSetting.class), any(String.class));
    }

    @Test
    @DisplayName("save: TZバリデーション失敗時はフィールドエラー登録＋編集画面に戻る")
    void save_timezoneValidationFails_addsFieldErrorAndReturnsEdit() {
        LocalizationEditForm form = new LocalizationEditForm();
        form.setDefaultLanguage(SupportedLanguage.JA);
        form.setAvailableLanguages(List.of(SupportedLanguage.JA));
        form.setDefaultTimezone(SupportedTimezone.of("Asia/Tokyo"));
        form.setAvailableTimezones(List.of(SupportedTimezone.of("UTC"))); // 不整合
        BindingResult br = new BeanPropertyBindingResult(form, "localizationEditForm");

        when(localizationSettingService.getLanguageSetting()).thenReturn(Optional.empty());
        when(localizationSettingService.getTimezoneSetting()).thenReturn(Optional.empty());

        // 言語保存は成功させる
        doThrow(new LocalizationConfigException(
                LocalizationConfigError.DEFAULT_TIMEZONE_NOT_IN_AVAILABLE))
                .when(localizationSettingService)
                .saveTimezoneSetting(any(TimezoneSetting.class), any(String.class));

        String view = controller.save(
                form, br, principal, Locale.ENGLISH, redirectAttrs, model);

        assertThat(view).isEqualTo("system/system-settings/localization/edit");
        assertThat(br.hasFieldErrors("defaultTimezone")).isTrue();
        // 言語側は呼ばれている
        verify(localizationSettingService)
                .saveLanguageSetting(any(LanguageSetting.class), any(String.class));
    }

    @Test
    @DisplayName("save: 言語側エラー AVAILABLE_LANGUAGES_EMPTY は availableLanguages フィールドエラー")
    void save_availableLanguagesEmpty_addsAvailableLanguagesFieldError() {
        LocalizationEditForm form = new LocalizationEditForm();
        form.setDefaultLanguage(SupportedLanguage.JA);
        form.setAvailableLanguages(List.of(SupportedLanguage.JA));
        form.setDefaultTimezone(SupportedTimezone.of("Asia/Tokyo"));
        form.setAvailableTimezones(List.of(SupportedTimezone.of("Asia/Tokyo")));
        BindingResult br = new BeanPropertyBindingResult(form, "localizationEditForm");

        when(localizationSettingService.getLanguageSetting()).thenReturn(Optional.empty());
        when(localizationSettingService.getTimezoneSetting()).thenReturn(Optional.empty());

        doThrow(new LocalizationConfigException(
                LocalizationConfigError.AVAILABLE_LANGUAGES_EMPTY))
                .when(localizationSettingService)
                .saveLanguageSetting(any(LanguageSetting.class), any(String.class));

        controller.save(form, br, principal, Locale.ENGLISH, redirectAttrs, model);

        assertThat(br.hasFieldErrors("availableLanguages")).isTrue();
    }

    @Test
    @DisplayName("save: TZ側エラー AVAILABLE_TIMEZONES_EMPTY は availableTimezones フィールドエラー")
    void save_availableTimezonesEmpty_addsAvailableTimezonesFieldError() {
        LocalizationEditForm form = new LocalizationEditForm();
        form.setDefaultLanguage(SupportedLanguage.JA);
        form.setAvailableLanguages(List.of(SupportedLanguage.JA));
        form.setDefaultTimezone(SupportedTimezone.of("Asia/Tokyo"));
        form.setAvailableTimezones(List.of(SupportedTimezone.of("Asia/Tokyo")));
        BindingResult br = new BeanPropertyBindingResult(form, "localizationEditForm");

        when(localizationSettingService.getLanguageSetting()).thenReturn(Optional.empty());
        when(localizationSettingService.getTimezoneSetting()).thenReturn(Optional.empty());

        doThrow(new LocalizationConfigException(
                LocalizationConfigError.AVAILABLE_TIMEZONES_EMPTY))
                .when(localizationSettingService)
                .saveTimezoneSetting(any(TimezoneSetting.class), any(String.class));

        controller.save(form, br, principal, Locale.ENGLISH, redirectAttrs, model);

        assertThat(br.hasFieldErrors("availableTimezones")).isTrue();
    }

    @Test
    @DisplayName("save: 言語側エラー DEFAULT_LANGUAGE_REQUIRED は defaultLanguage フィールドエラー")
    void save_defaultLanguageRequired_addsDefaultLanguageFieldError() {
        LocalizationEditForm form = new LocalizationEditForm();
        form.setDefaultLanguage(SupportedLanguage.JA);
        form.setAvailableLanguages(List.of(SupportedLanguage.JA));
        form.setDefaultTimezone(SupportedTimezone.of("Asia/Tokyo"));
        form.setAvailableTimezones(List.of(SupportedTimezone.of("Asia/Tokyo")));
        BindingResult br = new BeanPropertyBindingResult(form, "localizationEditForm");

        when(localizationSettingService.getLanguageSetting()).thenReturn(Optional.empty());
        when(localizationSettingService.getTimezoneSetting()).thenReturn(Optional.empty());

        doThrow(new LocalizationConfigException(
                LocalizationConfigError.DEFAULT_LANGUAGE_REQUIRED))
                .when(localizationSettingService)
                .saveLanguageSetting(any(LanguageSetting.class), any(String.class));

        controller.save(form, br, principal, Locale.ENGLISH, redirectAttrs, model);

        assertThat(br.hasFieldErrors("defaultLanguage")).isTrue();
    }

    @Test
    @DisplayName("save: TZ側エラー DEFAULT_TIMEZONE_REQUIRED は defaultTimezone フィールドエラー")
    void save_defaultTimezoneRequired_addsDefaultTimezoneFieldError() {
        LocalizationEditForm form = new LocalizationEditForm();
        form.setDefaultLanguage(SupportedLanguage.JA);
        form.setAvailableLanguages(List.of(SupportedLanguage.JA));
        form.setDefaultTimezone(SupportedTimezone.of("Asia/Tokyo"));
        form.setAvailableTimezones(List.of(SupportedTimezone.of("Asia/Tokyo")));
        BindingResult br = new BeanPropertyBindingResult(form, "localizationEditForm");

        when(localizationSettingService.getLanguageSetting()).thenReturn(Optional.empty());
        when(localizationSettingService.getTimezoneSetting()).thenReturn(Optional.empty());

        doThrow(new LocalizationConfigException(
                LocalizationConfigError.DEFAULT_TIMEZONE_REQUIRED))
                .when(localizationSettingService)
                .saveTimezoneSetting(any(TimezoneSetting.class), any(String.class));

        controller.save(form, br, principal, Locale.ENGLISH, redirectAttrs, model);

        assertThat(br.hasFieldErrors("defaultTimezone")).isTrue();
    }

    @Test
    @DisplayName("save: SERIALIZATION_FAILED はグローバルエラー扱い")
    void save_serializationFailed_addsGlobalError() {
        LocalizationEditForm form = new LocalizationEditForm();
        form.setDefaultLanguage(SupportedLanguage.JA);
        form.setAvailableLanguages(List.of(SupportedLanguage.JA));
        form.setDefaultTimezone(SupportedTimezone.of("Asia/Tokyo"));
        form.setAvailableTimezones(List.of(SupportedTimezone.of("Asia/Tokyo")));
        BindingResult br = new BeanPropertyBindingResult(form, "localizationEditForm");

        when(localizationSettingService.getLanguageSetting()).thenReturn(Optional.empty());
        when(localizationSettingService.getTimezoneSetting()).thenReturn(Optional.empty());

        doThrow(new LocalizationConfigException(
                LocalizationConfigError.SERIALIZATION_FAILED))
                .when(localizationSettingService)
                .saveLanguageSetting(any(LanguageSetting.class), any(String.class));

        controller.save(form, br, principal, Locale.ENGLISH, redirectAttrs, model);

        assertThat(br.hasFieldErrors()).isFalse();
        assertThat(br.hasGlobalErrors()).isTrue();
    }

    @Test
    @DisplayName("save: 使用中の言語が新AVAILABLEから除外される場合、availableLanguagesフィールドエラー")
    void save_languageInUseRemoved_addsAvailableLanguagesFieldError() {
        // 現在のシステム側AVAILABLE: [JA, EN]
        // 新AVAILABLE: [JA]  ← ENを除外
        // ACTIVEなアカウントの中で EN を使用中: 2件
        LocalizationEditForm form = new LocalizationEditForm();
        form.setDefaultLanguage(SupportedLanguage.JA);
        form.setAvailableLanguages(List.of(SupportedLanguage.JA));
        form.setDefaultTimezone(SupportedTimezone.of("Asia/Tokyo"));
        form.setAvailableTimezones(List.of(SupportedTimezone.of("Asia/Tokyo")));
        BindingResult br = new BeanPropertyBindingResult(form, "localizationEditForm");

        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(languageSettingJa()));
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(timezoneSettingTokyo()));
        when(accountLocalizationApplicationService
                .countActiveAccountsUsingLanguage(SupportedLanguage.EN))
                .thenReturn(2);
        // JAは新AVAILABLEに含まれているのでチェック不要だが、安全側で0返す
        when(accountLocalizationApplicationService
                .countActiveAccountsUsingLanguage(SupportedLanguage.JA))
                .thenReturn(0);

        String view = controller.save(
                form, br, principal, Locale.ENGLISH, redirectAttrs, model);

        assertThat(view).isEqualTo("system/system-settings/localization/edit");
        assertThat(br.hasFieldErrors("availableLanguages")).isTrue();
        // 保存は呼ばれていない
        verify(localizationSettingService, never())
                .saveLanguageSetting(any(LanguageSetting.class), any(String.class));
        verify(localizationSettingService, never())
                .saveTimezoneSetting(any(TimezoneSetting.class), any(String.class));
    }

    @Test
    @DisplayName("save: 使用中のTZが新AVAILABLEから除外される場合、availableTimezonesフィールドエラー")
    void save_timezoneInUseRemoved_addsAvailableTimezonesFieldError() {
        // 現在: [Asia/Tokyo]、新: [America/New_York] → Asia/Tokyoを除外
        // Asia/Tokyo を ACTIVE で使用中: 3件
        LocalizationEditForm form = new LocalizationEditForm();
        form.setDefaultLanguage(SupportedLanguage.JA);
        form.setAvailableLanguages(List.of(SupportedLanguage.JA));
        form.setDefaultTimezone(SupportedTimezone.of("America/New_York"));
        form.setAvailableTimezones(List.of(SupportedTimezone.of("America/New_York")));
        BindingResult br = new BeanPropertyBindingResult(form, "localizationEditForm");

        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(languageSettingJa()));
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(timezoneSettingTokyo()));
        // 言語側は変更なし（JA in new AVAILABLE, EN除外もMockで0返す）
        when(accountLocalizationApplicationService
                .countActiveAccountsUsingLanguage(SupportedLanguage.EN))
                .thenReturn(0);
        when(accountLocalizationApplicationService
                .countActiveAccountsUsingTimezone(SupportedTimezone.of("Asia/Tokyo")))
                .thenReturn(3);

        String view = controller.save(
                form, br, principal, Locale.ENGLISH, redirectAttrs, model);

        assertThat(view).isEqualTo("system/system-settings/localization/edit");
        assertThat(br.hasFieldErrors("availableTimezones")).isTrue();
        verify(localizationSettingService, never())
                .saveLanguageSetting(any(LanguageSetting.class), any(String.class));
    }

    @Test
    @DisplayName("save: 複数の使用中言語が除外される場合、件数は合算される")
    void save_multipleLanguagesInUseRemoved_sumsCount() {
        // 現在AVAILABLE: [JA, EN] → 新: [] （両方除外）
        // ※ availableLanguagesEmptyはService側で別エラーになるが、
        //   ここでは使用中チェックが先に走ることを確認するため、
        //   新AVAILABLE=[XX]のような形でJA/EN両方除外する状況を作る
        // ただし2言語しかenum値が無いため、AVAILABLEを空にして
        // 「両方使用中で除外される」状況をシミュレートする
        LocalizationEditForm form = new LocalizationEditForm();
        form.setDefaultLanguage(SupportedLanguage.JA);
        form.setAvailableLanguages(List.of()); // 両方除外
        form.setDefaultTimezone(SupportedTimezone.of("Asia/Tokyo"));
        form.setAvailableTimezones(List.of(SupportedTimezone.of("Asia/Tokyo")));
        BindingResult br = new BeanPropertyBindingResult(form, "localizationEditForm");

        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(languageSettingJa()));
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(timezoneSettingTokyo()));
        when(accountLocalizationApplicationService
                .countActiveAccountsUsingLanguage(SupportedLanguage.JA))
                .thenReturn(3);
        when(accountLocalizationApplicationService
                .countActiveAccountsUsingLanguage(SupportedLanguage.EN))
                .thenReturn(2);

        controller.save(form, br, principal, Locale.ENGLISH, redirectAttrs, model);

        assertThat(br.hasFieldErrors("availableLanguages")).isTrue();
        // メッセージ取得時に件数 5 が渡されていることを確認
        verify(messageSource).getMessage(
                eq("system.localization.error.LANGUAGE_IN_USE_BY_ACCOUNT"),
                eq(new Object[] { 5 }),
                any(),
                any(Locale.class));
    }

    @Test
    @DisplayName("save: 使用中件数が0なら通常通り保存される")
    void save_inUseCountIsZero_proceedsToSave() {
        // 現在: [JA, EN] → 新: [JA]  ENは使用中件数0
        LocalizationEditForm form = new LocalizationEditForm();
        form.setDefaultLanguage(SupportedLanguage.JA);
        form.setAvailableLanguages(List.of(SupportedLanguage.JA));
        form.setDefaultTimezone(SupportedTimezone.of("Asia/Tokyo"));
        form.setAvailableTimezones(List.of(SupportedTimezone.of("Asia/Tokyo")));
        BindingResult br = new BeanPropertyBindingResult(form, "localizationEditForm");

        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(languageSettingJa()));
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(timezoneSettingTokyo()));
        when(accountLocalizationApplicationService
                .countActiveAccountsUsingLanguage(SupportedLanguage.EN))
                .thenReturn(0);

        String view = controller.save(
                form, br, principal, Locale.ENGLISH, redirectAttrs, model);

        assertThat(view).isEqualTo("redirect:/system/system-settings/localization");
        verify(localizationSettingService).saveLanguageSetting(
                any(LanguageSetting.class), any(String.class));
    }

    @Test
    @DisplayName("save: 言語・TZの両方で使用中エラー → 両フィールドにエラー登録")
    void save_bothLanguageAndTimezoneInUse_addsBothFieldErrors() {
        // 言語: EN除外で2件、TZ: Asia/Tokyo除外で3件
        LocalizationEditForm form = new LocalizationEditForm();
        form.setDefaultLanguage(SupportedLanguage.JA);
        form.setAvailableLanguages(List.of(SupportedLanguage.JA));
        form.setDefaultTimezone(SupportedTimezone.of("America/New_York"));
        form.setAvailableTimezones(List.of(SupportedTimezone.of("America/New_York")));
        BindingResult br = new BeanPropertyBindingResult(form, "localizationEditForm");

        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(languageSettingJa()));
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(timezoneSettingTokyo()));
        when(accountLocalizationApplicationService
                .countActiveAccountsUsingLanguage(SupportedLanguage.EN))
                .thenReturn(2);
        when(accountLocalizationApplicationService
                .countActiveAccountsUsingTimezone(SupportedTimezone.of("Asia/Tokyo")))
                .thenReturn(3);

        controller.save(form, br, principal, Locale.ENGLISH, redirectAttrs, model);

        assertThat(br.hasFieldErrors("availableLanguages")).isTrue();
        assertThat(br.hasFieldErrors("availableTimezones")).isTrue();
        verify(localizationSettingService, never())
                .saveLanguageSetting(any(LanguageSetting.class), any(String.class));
    }

    @Test
    @DisplayName("save: 使用中チェックは現在の言語設定取得失敗時にスキップされる（フォールバック）")
    void save_languageSettingDeserializationFails_skipsInUseCheckForLanguage() {
        LocalizationEditForm form = new LocalizationEditForm();
        form.setDefaultLanguage(SupportedLanguage.JA);
        form.setAvailableLanguages(List.of(SupportedLanguage.JA));
        form.setDefaultTimezone(SupportedTimezone.of("Asia/Tokyo"));
        form.setAvailableTimezones(List.of(SupportedTimezone.of("Asia/Tokyo")));
        BindingResult br = new BeanPropertyBindingResult(form, "localizationEditForm");

        // 言語設定取得が例外（デシリアライズ失敗）
        when(localizationSettingService.getLanguageSetting())
                .thenThrow(new LocalizationConfigException(
                        LocalizationConfigError.DESERIALIZATION_FAILED));
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(timezoneSettingTokyo()));

        controller.save(form, br, principal, Locale.ENGLISH, redirectAttrs, model);

        // 言語側の使用中件数取得は呼ばれない
        verify(accountLocalizationApplicationService, never())
                .countActiveAccountsUsingLanguage(any());
        // 保存処理は実行された
        verify(localizationSettingService).saveLanguageSetting(
                any(LanguageSetting.class), any(String.class));
    }

}
