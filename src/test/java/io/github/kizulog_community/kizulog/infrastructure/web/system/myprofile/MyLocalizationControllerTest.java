package io.github.kizulog_community.kizulog.infrastructure.web.system.myprofile;

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
import org.mockito.ArgumentCaptor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

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

/**
 * MyLocalizationControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class MyLocalizationControllerTest {

    private static final String ACCOUNT_ID = "account-001";
    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    private AccountLocalizationApplicationService accountLocalizationApplicationService;
    private SystemAccountLocalizationService systemAccountLocalizationService;
    private LocalizationSettingService localizationSettingService;
    private SessionLocaleResolver localeResolver;
    private MessageSource messageSource;
    private MyLocalizationController controller;

    private Model model;
    private RedirectAttributes redirectAttrs;
    private SystemUserPrincipal principal;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        accountLocalizationApplicationService =
                mock(AccountLocalizationApplicationService.class);
        systemAccountLocalizationService = mock(SystemAccountLocalizationService.class);
        localizationSettingService = mock(LocalizationSettingService.class);
        localeResolver = mock(SessionLocaleResolver.class);
        messageSource = mock(MessageSource.class);

        controller = new MyLocalizationController(
                accountLocalizationApplicationService,
                systemAccountLocalizationService,
                localizationSettingService,
                localeResolver,
                messageSource);

        model = new ConcurrentModel();
        redirectAttrs = new RedirectAttributesModelMap();
        principal = mock(SystemUserPrincipal.class);
        when(principal.getAccountId()).thenReturn(ACCOUNT_ID);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        when(messageSource.getMessage(any(String.class), any(), any(), any(Locale.class)))
                .thenReturn("dummy message");

        // 共通：システム側AVAILABLEモック
        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(new LanguageSetting(
                        SupportedLanguage.JA,
                        List.of(SupportedLanguage.JA, SupportedLanguage.EN))));
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(new TimezoneSetting(
                        SupportedTimezone.of("Asia/Tokyo"),
                        List.of(SupportedTimezone.of("Asia/Tokyo"),
                                SupportedTimezone.of("UTC")))));
    }

    private SystemAccountLocalization sampleLocalization(
            SupportedLanguage lang, SupportedTimezone tz) {
        return new SystemAccountLocalization(
                ACCOUNT_ID, BASE_TIME, lang, tz, BASE_TIME, "system:invite:inv-001");
    }

    @Test
    @DisplayName("detail: 既存設定がある場合、その値をViewに設定して返す")
    void detail_returnsViewWithExistingValues() {
        SystemAccountLocalization existing = sampleLocalization(
                SupportedLanguage.EN, SupportedTimezone.of("UTC"));
        when(systemAccountLocalizationService.getLocalization(ACCOUNT_ID))
                .thenReturn(Optional.of(existing));

        String view = controller.detail(principal, model);

        assertThat(view).isEqualTo("system/my-profile/localization/detail");
        MyLocalizationDetailView v = (MyLocalizationDetailView)
                model.getAttribute("localization");
        assertThat(v).isNotNull();
        assertThat(v.getLanguage()).isEqualTo(SupportedLanguage.EN);
        assertThat(v.getTimezone().getId()).isEqualTo("UTC");
        assertThat(v.getCreatedBy()).isEqualTo("system:invite:inv-001");
        assertThat(v.isConfigured()).isTrue();
        assertThat(model.getAttribute("activeMenu")).isEqualTo("my-profile");

        // 既存値があるので自動作成は呼ばれない
        verify(accountLocalizationApplicationService, never())
                .createDefaultLocalizationForAccount(any(), any());
    }

    @Test
    @DisplayName("detail: 未設定時はシステムデフォルトで自動作成し、その値で表示（Q5案C）")
    void detail_autoCreatesWhenNotConfigured() {
        SystemAccountLocalization created = sampleLocalization(
                SupportedLanguage.JA, SupportedTimezone.of("Asia/Tokyo"));
        // 1回目: 空、2回目（自動作成後の再取得）: ある
        when(systemAccountLocalizationService.getLocalization(ACCOUNT_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(created));

        String view = controller.detail(principal, model);

        assertThat(view).isEqualTo("system/my-profile/localization/detail");
        verify(accountLocalizationApplicationService)
                .createDefaultLocalizationForAccount(
                        eq(ACCOUNT_ID), eq("system:auto-recovery"));
        MyLocalizationDetailView v = (MyLocalizationDetailView)
                model.getAttribute("localization");
        assertThat(v.getLanguage()).isEqualTo(SupportedLanguage.JA);
        assertThat(v.getTimezone().getId()).isEqualTo("Asia/Tokyo");
    }

    @Test
    @DisplayName("editForm: 既存設定がある場合、その値でフォームを初期化し選択肢をモデルに設定")
    void editForm_initializedWithExistingValues() {
        SystemAccountLocalization existing = sampleLocalization(
                SupportedLanguage.EN, SupportedTimezone.of("UTC"));
        when(systemAccountLocalizationService.getLocalization(ACCOUNT_ID))
                .thenReturn(Optional.of(existing));

        String view = controller.editForm(principal, model);

        assertThat(view).isEqualTo("system/my-profile/localization/edit");
        MyLocalizationEditForm form = (MyLocalizationEditForm)
                model.getAttribute("myLocalizationEditForm");
        assertThat(form.getLanguage()).isEqualTo(SupportedLanguage.EN);
        assertThat(form.getTimezone().getId()).isEqualTo("UTC");

        // 選択肢
        assertThat(model.getAttribute("languageChoices")).isNotNull();
        assertThat(model.getAttribute("languageOptions")).isNotNull();
        assertThat(model.getAttribute("timezoneOptions")).isNotNull();
        assertThat(model.getAttribute("activeMenu")).isEqualTo("my-profile");
    }

    @Test
    @DisplayName("editForm: 未設定時は自動作成して初期化する")
    void editForm_autoCreatesWhenNotConfigured() {
        SystemAccountLocalization created = sampleLocalization(
                SupportedLanguage.JA, SupportedTimezone.of("Asia/Tokyo"));
        when(systemAccountLocalizationService.getLocalization(ACCOUNT_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(created));

        controller.editForm(principal, model);

        verify(accountLocalizationApplicationService)
                .createDefaultLocalizationForAccount(
                        eq(ACCOUNT_ID), eq("system:auto-recovery"));
        MyLocalizationEditForm form = (MyLocalizationEditForm)
                model.getAttribute("myLocalizationEditForm");
        assertThat(form.getLanguage()).isEqualTo(SupportedLanguage.JA);
    }

    @Test
    @DisplayName("editForm: モデルに既存formがある場合は上書きしない(flash復元)")
    void editForm_doesNotOverwriteExistingForm() {
        SystemAccountLocalization existing = sampleLocalization(
                SupportedLanguage.JA, SupportedTimezone.of("Asia/Tokyo"));
        when(systemAccountLocalizationService.getLocalization(ACCOUNT_ID))
                .thenReturn(Optional.of(existing));

        MyLocalizationEditForm existingForm = new MyLocalizationEditForm();
        existingForm.setLanguage(SupportedLanguage.EN);
        existingForm.setTimezone(SupportedTimezone.of("UTC"));
        model.addAttribute("myLocalizationEditForm", existingForm);

        controller.editForm(principal, model);

        MyLocalizationEditForm form = (MyLocalizationEditForm)
                model.getAttribute("myLocalizationEditForm");
        assertThat(form).isSameAs(existingForm);
        // 選択肢は設定される
        assertThat(model.getAttribute("languageOptions")).isNotNull();
    }

    @Test
    @DisplayName("save: 正常系で保存され詳細画面へリダイレクト、flashSuccessが設定される")
    void save_success_redirectsToDetailWithFlashSuccess() {
        MyLocalizationEditForm form = new MyLocalizationEditForm();
        form.setLanguage(SupportedLanguage.EN);
        form.setTimezone(SupportedTimezone.of("UTC"));
        BindingResult br = new BeanPropertyBindingResult(form, "myLocalizationEditForm");

        String view = controller.save(
                form, br, principal, Locale.ENGLISH, request, response, redirectAttrs, model);

        assertThat(view).isEqualTo("redirect:/system/my-profile/localization");
        assertThat(redirectAttrs.getFlashAttributes()).containsKey("flashSuccessKey");
        verify(accountLocalizationApplicationService).saveAccountLocalization(
                eq(ACCOUNT_ID),
                eq(SupportedLanguage.EN),
                eq(SupportedTimezone.of("UTC")),
                eq(ACCOUNT_ID));
    }

    @Test
    @DisplayName("save: 正常系でセッションにLocale/TimeZoneが即時反映される（Q6）")
    void save_success_appliesLocaleAndTimeZoneToSession() {
        MyLocalizationEditForm form = new MyLocalizationEditForm();
        form.setLanguage(SupportedLanguage.EN);
        form.setTimezone(SupportedTimezone.of("UTC"));
        BindingResult br = new BeanPropertyBindingResult(form, "myLocalizationEditForm");

        controller.save(form, br, principal, Locale.JAPANESE,
                request, response, redirectAttrs, model);

        ArgumentCaptor<LocaleContext> ctxCap = ArgumentCaptor.forClass(LocaleContext.class);
        verify(localeResolver).setLocaleContext(
                eq(request), eq(response), ctxCap.capture());

        LocaleContext ctx = ctxCap.getValue();
        assertThat(ctx).isInstanceOf(SimpleTimeZoneAwareLocaleContext.class);
        assertThat(ctx.getLocale()).isEqualTo(SupportedLanguage.EN.getLocale());

        TimeZoneAwareLocaleContext tzCtx = (TimeZoneAwareLocaleContext) ctx;
        assertThat(tzCtx.getTimeZone().getID()).isEqualTo("UTC");
    }

    @Test
    @DisplayName("save: BindingResultにエラーがある場合、編集画面を再表示しService呼ばれない")
    void save_bindingErrors_returnsEditView() {
        MyLocalizationEditForm form = new MyLocalizationEditForm();
        // language/timezoneがnull → @NotNullで弾かれるが、ここではBindingResultに直接エラー追加
        BindingResult br = new BeanPropertyBindingResult(form, "myLocalizationEditForm");
        br.reject("error", "test");

        String view = controller.save(
                form, br, principal, Locale.ENGLISH, request, response, redirectAttrs, model);

        assertThat(view).isEqualTo("system/my-profile/localization/edit");
        assertThat(model.getAttribute("languageOptions")).isNotNull();
        verify(accountLocalizationApplicationService, never())
                .saveAccountLocalization(any(), any(), any(), any());
        verify(localeResolver, never())
                .setLocaleContext(any(), any(), any());
    }

    @Test
    @DisplayName("save: LANGUAGE_NOT_AVAILABLE はlanguageフィールドエラー")
    void save_languageNotAvailable_addsLanguageFieldError() {
        MyLocalizationEditForm form = new MyLocalizationEditForm();
        form.setLanguage(SupportedLanguage.EN);
        form.setTimezone(SupportedTimezone.of("UTC"));
        BindingResult br = new BeanPropertyBindingResult(form, "myLocalizationEditForm");

        doThrow(new AccountLocalizationException(
                AccountLocalizationError.LANGUAGE_NOT_AVAILABLE))
                .when(accountLocalizationApplicationService)
                .saveAccountLocalization(any(), any(), any(), any());

        String view = controller.save(
                form, br, principal, Locale.ENGLISH, request, response, redirectAttrs, model);

        assertThat(view).isEqualTo("system/my-profile/localization/edit");
        assertThat(br.hasFieldErrors("language")).isTrue();
        verify(localeResolver, never())
                .setLocaleContext(any(), any(), any());
    }

    @Test
    @DisplayName("save: TIMEZONE_NOT_AVAILABLE はtimezoneフィールドエラー")
    void save_timezoneNotAvailable_addsTimezoneFieldError() {
        MyLocalizationEditForm form = new MyLocalizationEditForm();
        form.setLanguage(SupportedLanguage.JA);
        form.setTimezone(SupportedTimezone.of("Asia/Tokyo"));
        BindingResult br = new BeanPropertyBindingResult(form, "myLocalizationEditForm");

        doThrow(new AccountLocalizationException(
                AccountLocalizationError.TIMEZONE_NOT_AVAILABLE))
                .when(accountLocalizationApplicationService)
                .saveAccountLocalization(any(), any(), any(), any());

        controller.save(form, br, principal, Locale.ENGLISH,
                request, response, redirectAttrs, model);

        assertThat(br.hasFieldErrors("timezone")).isTrue();
    }

    @Test
    @DisplayName("save: LANGUAGE_REQUIRED はlanguageフィールドエラー")
    void save_languageRequired_addsLanguageFieldError() {
        MyLocalizationEditForm form = new MyLocalizationEditForm();
        form.setLanguage(SupportedLanguage.JA);
        form.setTimezone(SupportedTimezone.of("Asia/Tokyo"));
        BindingResult br = new BeanPropertyBindingResult(form, "myLocalizationEditForm");

        doThrow(new AccountLocalizationException(
                AccountLocalizationError.LANGUAGE_REQUIRED))
                .when(accountLocalizationApplicationService)
                .saveAccountLocalization(any(), any(), any(), any());

        controller.save(form, br, principal, Locale.ENGLISH,
                request, response, redirectAttrs, model);

        assertThat(br.hasFieldErrors("language")).isTrue();
    }

    @Test
    @DisplayName("save: SYSTEM_LOCALIZATION_NOT_CONFIGURED はグローバルエラー")
    void save_systemNotConfigured_addsGlobalError() {
        MyLocalizationEditForm form = new MyLocalizationEditForm();
        form.setLanguage(SupportedLanguage.JA);
        form.setTimezone(SupportedTimezone.of("Asia/Tokyo"));
        BindingResult br = new BeanPropertyBindingResult(form, "myLocalizationEditForm");

        doThrow(new AccountLocalizationException(
                AccountLocalizationError.SYSTEM_LOCALIZATION_NOT_CONFIGURED))
                .when(accountLocalizationApplicationService)
                .saveAccountLocalization(any(), any(), any(), any());

        controller.save(form, br, principal, Locale.ENGLISH,
                request, response, redirectAttrs, model);

        assertThat(br.hasFieldErrors()).isFalse();
        assertThat(br.hasGlobalErrors()).isTrue();
    }

}
