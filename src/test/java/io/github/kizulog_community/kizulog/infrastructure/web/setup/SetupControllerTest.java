package io.github.kizulog_community.kizulog.infrastructure.web.setup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.nimbusds.jwt.JWTClaimsSet;

import io.github.kizulog_community.kizulog.domain.setup.SetupService;
import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.OidcConnectionError;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.OidcConnectionException;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.OidcProviderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * SetupControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class SetupControllerTest {

    private MessageSource messageSource;
    private SessionLocaleResolver localeResolver;
    private SetupService setupService;
    private SetupSessionData setupSessionData;
    private OidcProviderService oidcProviderService;
    private SetupController controller;

    @BeforeEach
    void setUp() {
        messageSource = mock(MessageSource.class);
        localeResolver = mock(SessionLocaleResolver.class);
        setupService = mock(SetupService.class);
        setupSessionData = new SetupSessionData();
        setupSessionData.setOidcSettings(new ArrayList<>());
        oidcProviderService = mock(OidcProviderService.class);

        controller = new SetupController(
                messageSource,
                localeResolver,
                setupService,
                setupSessionData,
                oidcProviderService);
    }

    // ============================================================
    // Step0
    // ============================================================
    @Test
    @DisplayName("step0()はステップ0テンプレートを返す")
    void step0_returnsTemplate() {
        Model model = mock(Model.class);
        String view = controller.step0(model);
        assertThat(view).isEqualTo("setup/step0");
    }

    @Test
    @DisplayName("step0()はモデルにlanguagesとsessionDataを設定する")
    void step0_setsModelAttributes() {
        Model model = mock(Model.class);
        controller.step0(model);
        verify(model).addAttribute(eq("languages"), any());
        verify(model).addAttribute(eq("sessionData"), eq(setupSessionData));
    }

    @Test
    @DisplayName("step0Submit()はsetupLanguageを設定してstep1へリダイレクト")
    void step0Submit_setsLanguageAndRedirects() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        String view = controller.step0Submit(SupportedLanguage.JA, request, response);

        assertThat(view).isEqualTo("redirect:/setup/step1");
        assertThat(setupSessionData.getSetupLanguage()).isEqualTo(SupportedLanguage.JA);
        verify(localeResolver).setLocaleContext(eq(request), eq(response), any());
    }

    // ============================================================
    // Step1
    // ============================================================
    @Test
    @DisplayName("step1()でsetupLanguageが未設定ならstep0へリダイレクト")
    void step1_languageNotSet_redirectsToStep0() {
        Model model = mock(Model.class);
        String view = controller.step1(model);
        assertThat(view).isEqualTo("redirect:/setup/step0");
    }

    @Test
    @DisplayName("step1()でsetupLanguageがあればstep1テンプレートを返す")
    void step1_returnsTemplate() {
        setupSessionData.setSetupLanguage(SupportedLanguage.JA);
        setupSessionData.setAvailableTimezones(new ArrayList<>());
        Model model = mock(Model.class);

        String view = controller.step1(model);

        assertThat(view).isEqualTo("setup/step1");
        verify(model).addAttribute(eq("sessionData"), eq(setupSessionData));
        verify(model).addAttribute(eq("languages"), any());
        verify(model).addAttribute(eq("timezonesJson"), any());
        verify(model).addAttribute(eq("selectedTimezones"), any());
    }

    @Test
    @DisplayName("step1Submit()で利用可能言語が空ならstep1にリダイレクト")
    void step1Submit_emptyAvailableLanguages_redirectsToStep1() {
        Step1FormData formData = new Step1FormData();
        formData.setAvailableLanguages(List.of());
        formData.setAvailableTimezones(List.of(SupportedTimezone.of("Asia/Tokyo")));
        formData.setDefaultLanguage(SupportedLanguage.JA);
        formData.setDefaultTimezone(SupportedTimezone.of("Asia/Tokyo"));
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        String view = controller.step1Submit(formData, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/setup/step1");
        verify(redirectAttributes).addFlashAttribute(eq("error"),
                eq("validation.availableLanguages.empty"));
    }

    @Test
    @DisplayName("step1Submit()で利用可能タイムゾーンが空ならstep1にリダイレクト")
    void step1Submit_emptyAvailableTimezones_redirectsToStep1() {
        Step1FormData formData = new Step1FormData();
        formData.setAvailableLanguages(List.of(SupportedLanguage.JA));
        formData.setAvailableTimezones(List.of());
        formData.setDefaultLanguage(SupportedLanguage.JA);
        formData.setDefaultTimezone(SupportedTimezone.of("Asia/Tokyo"));
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        String view = controller.step1Submit(formData, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/setup/step1");
        verify(redirectAttributes).addFlashAttribute(eq("error"),
                eq("validation.availableTimezones.empty"));
    }

    @Test
    @DisplayName("step1Submit()でデフォルト言語が利用可能言語に含まれないとstep1にリダイレクト")
    void step1Submit_invalidDefaultLanguage_redirectsToStep1() {
        Step1FormData formData = new Step1FormData();
        formData.setAvailableLanguages(List.of(SupportedLanguage.JA));
        formData.setAvailableTimezones(List.of(SupportedTimezone.of("Asia/Tokyo")));
        formData.setDefaultLanguage(SupportedLanguage.EN);
        formData.setDefaultTimezone(SupportedTimezone.of("Asia/Tokyo"));
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        String view = controller.step1Submit(formData, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/setup/step1");
        verify(redirectAttributes).addFlashAttribute(eq("error"),
                eq("validation.defaultLanguage.invalid"));
    }

    @Test
    @DisplayName("step1Submit()でデフォルトTZが利用可能TZに含まれないとstep1にリダイレクト")
    void step1Submit_invalidDefaultTimezone_redirectsToStep1() {
        Step1FormData formData = new Step1FormData();
        formData.setAvailableLanguages(List.of(SupportedLanguage.JA));
        formData.setAvailableTimezones(List.of(SupportedTimezone.of("Asia/Tokyo")));
        formData.setDefaultLanguage(SupportedLanguage.JA);
        formData.setDefaultTimezone(SupportedTimezone.of("America/New_York"));
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        String view = controller.step1Submit(formData, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/setup/step1");
        verify(redirectAttributes).addFlashAttribute(eq("error"),
                eq("validation.defaultTimezone.invalid"));
    }

    @Test
    @DisplayName("step1Submit()で正常なフォームデータならstep2にリダイレクト")
    void step1Submit_validData_redirectsToStep2() {
        Step1FormData formData = new Step1FormData();
        formData.setAvailableLanguages(List.of(SupportedLanguage.JA, SupportedLanguage.EN));
        formData.setAvailableTimezones(List.of(SupportedTimezone.of("Asia/Tokyo")));
        formData.setDefaultLanguage(SupportedLanguage.JA);
        formData.setDefaultTimezone(SupportedTimezone.of("Asia/Tokyo"));
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        String view = controller.step1Submit(formData, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/setup/step2");
        assertThat(setupSessionData.getDefaultLanguage()).isEqualTo(SupportedLanguage.JA);
        assertThat(setupSessionData.getAvailableLanguages()).hasSize(2);
        assertThat(setupSessionData.getDefaultTimezone().getId()).isEqualTo("Asia/Tokyo");
        assertThat(setupSessionData.getAvailableTimezones()).hasSize(1);
    }

    // ============================================================
    // Step2
    // ============================================================
    @Test
    @DisplayName("step2()でhostが未設定ならリクエストのserverNameを設定")
    void step2_hostNull_setsServerName() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServerName()).thenReturn("kizulog.example.com");
        Model model = mock(Model.class);

        String view = controller.step2(model, request);

        assertThat(view).isEqualTo("setup/step2");
        assertThat(setupSessionData.getHost()).isEqualTo("kizulog.example.com");
    }

    @Test
    @DisplayName("step2()でoidcSettingsが空なら空のOidcSettingを追加")
    void step2_emptyOidcSettings_addsNewOne() {
        setupSessionData.setHost("existing.example.com");
        HttpServletRequest request = mock(HttpServletRequest.class);
        Model model = mock(Model.class);

        controller.step2(model, request);

        assertThat(setupSessionData.getOidcSettings()).hasSize(1);
    }

    @Test
    @DisplayName("step2()でhostが既に設定済みなら上書きしない")
    void step2_hostAlreadySet_doesNotOverwrite() {
        setupSessionData.setHost("existing.example.com");
        HttpServletRequest request = mock(HttpServletRequest.class);
        Model model = mock(Model.class);

        controller.step2(model, request);

        assertThat(setupSessionData.getHost()).isEqualTo("existing.example.com");
    }

    @Test
    @DisplayName("step2Submit()はhostを設定しOIDC IDをmasterに固定してstep3にリダイレクト")
    void step2Submit_setsHostAndMasterId_redirectsToStep3() {
        OidcSetting oidcSetting = new OidcSetting();
        oidcSetting.setId("user-input-id");
        oidcSetting.setUri("https://auth.example.com");
        oidcSetting.setClientId("client-id");
        oidcSetting.setClientSecret("client-secret");

        Step2FormData formData = new Step2FormData();
        formData.setHost("kizulog.example.com");
        formData.setOidcSetting(oidcSetting);

        String view = controller.step2Submit(formData);

        assertThat(view).isEqualTo("redirect:/setup/step3");
        assertThat(setupSessionData.getHost()).isEqualTo("kizulog.example.com");
        assertThat(setupSessionData.getOidcSettings()).hasSize(1);
        assertThat(setupSessionData.getOidcSettings().get(0).getId()).isEqualTo("master");
    }

    // ============================================================
    // Step3 / Step4 / Complete
    // ============================================================
    @Test
    @DisplayName("step3()はstep3テンプレートを返しsessionDataをモデルに設定")
    void step3_returnsTemplate() {
        Model model = mock(Model.class);
        String view = controller.step3(model);
        assertThat(view).isEqualTo("setup/step3");
        verify(model).addAttribute(eq("sessionData"), eq(setupSessionData));
    }

    @Test
    @DisplayName("step4()はstep4テンプレートを返しsessionDataをモデルに設定")
    void step4_returnsTemplate() {
        Model model = mock(Model.class);
        String view = controller.step4(model);
        assertThat(view).isEqualTo("setup/step4");
        verify(model).addAttribute(eq("sessionData"), eq(setupSessionData));
    }

    @Test
    @DisplayName("step4Submit()はsetupServiceのsave()を呼び完了画面にリダイレクト")
    void step4Submit_callsSaveAndRedirectsToComplete() {
        String view = controller.step4Submit();
        assertThat(view).isEqualTo("redirect:/setup/complete");
        verify(setupService).save(setupSessionData);
    }

    @Test
    @DisplayName("complete()はcompleteテンプレートを返す")
    void complete_returnsTemplate() {
        String view = controller.complete();
        assertThat(view).isEqualTo("setup/complete");
    }

    // ============================================================
    // checkOidc()
    // ============================================================
    @Test
    @DisplayName("checkOidc()で接続成功なら成功レスポンスを返す")
    void checkOidc_success_returnsSuccess() {
        when(messageSource.getMessage(eq("oidc.success"), any(), any(Locale.class)))
                .thenReturn("接続成功");

        ResponseEntity<OidcCheckResult> response = controller.checkOidc(
                Map.of("issuerUri", "https://auth.example.com"),
                Locale.JAPAN);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("接続成功");
    }

    @Test
    @DisplayName("checkOidc()でINPUT_ERRORなら入力エラーメッセージを返す")
    void checkOidc_inputError_returnsErrorMessage() {
        Mockito.doThrow(new OidcConnectionException(
                OidcConnectionError.INPUT_ERROR))
                .when(oidcProviderService).verify(anyString());
        when(messageSource.getMessage(eq("oidc.error.input"), any(), any(Locale.class)))
                .thenReturn("入力エラー");

        ResponseEntity<OidcCheckResult> response = controller.checkOidc(
                Map.of("issuerUri", ""),
                Locale.JAPAN);

        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorType()).isEqualTo("INPUT_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("入力エラー");
    }

    @Test
    @DisplayName("checkOidc()でINVALID_RESPONSEなら不正レスポンスメッセージを返す")
    void checkOidc_invalidResponse_returnsErrorMessage() {
        Mockito.doThrow(new OidcConnectionException(
                OidcConnectionError.INVALID_RESPONSE))
                .when(oidcProviderService).verify(anyString());
        when(messageSource.getMessage(eq("oidc.error.invalid_response"),
                any(), any(Locale.class)))
                .thenReturn("不正なレスポンス");

        ResponseEntity<OidcCheckResult> response = controller.checkOidc(
                Map.of("issuerUri", "https://example.com"),
                Locale.JAPAN);

        assertThat(response.getBody().getErrorType()).isEqualTo("INVALID_RESPONSE");
    }

    @Test
    @DisplayName("checkOidc()でUNEXPECTED_ERRORなら予期しないエラーメッセージを返す")
    void checkOidc_unexpectedError_returnsErrorMessage() {
        Mockito.doThrow(new OidcConnectionException(
                OidcConnectionError.UNEXPECTED_ERROR))
                .when(oidcProviderService).verify(anyString());
        when(messageSource.getMessage(eq("oidc.error.unexpected"),
                any(), any(Locale.class)))
                .thenReturn("予期しないエラー");

        ResponseEntity<OidcCheckResult> response = controller.checkOidc(
                Map.of("issuerUri", "https://example.com"),
                Locale.JAPAN);

        assertThat(response.getBody().getErrorType()).isEqualTo("UNEXPECTED_ERROR");
    }

    // ============================================================
    // oidcLogin()
    // ============================================================
    @Test
    @DisplayName("oidcLogin()はOIDCプロバイダーの認証URLにリダイレクト")
    void oidcLogin_redirectsToAuthorizationUrl() {
        OidcSetting oidc = new OidcSetting();
        oidc.setUri("https://auth.example.com/realms/master");
        oidc.setClientId("test-client");
        setupSessionData.getOidcSettings().add(oidc);

        when(oidcProviderService.getMetadata(anyString())).thenReturn(
                Map.of("authorization_endpoint", "https://auth.example.com/auth"));
        when(oidcProviderService.generateState()).thenReturn("random-state");
        when(oidcProviderService.buildAuthorizationUrl(anyString(), anyString(),
                anyString(), anyString()))
                .thenReturn("https://auth.example.com/auth?...");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("kizulog.example.com");
        when(request.getServerPort()).thenReturn(443);
        HttpSession session = mock(HttpSession.class);

        String view = controller.oidcLogin(request, session);

        assertThat(view).startsWith("redirect:");
        verify(session).setAttribute(eq("oidc_state"), eq("random-state"));
        verify(session).setAttribute(eq("oidc_redirect_uri"),
                eq("https://kizulog.example.com/setup/callback"));
    }

    @Test
    @DisplayName("oidcLogin()でポートが80・443以外の場合はURLにポートを含める")
    void oidcLogin_nonStandardPort_includesPortInRedirectUri() {
        OidcSetting oidc = new OidcSetting();
        oidc.setUri("https://auth.example.com/realms/master");
        oidc.setClientId("test-client");
        setupSessionData.getOidcSettings().add(oidc);

        when(oidcProviderService.getMetadata(anyString())).thenReturn(
                Map.of("authorization_endpoint", "https://auth.example.com/auth"));
        when(oidcProviderService.generateState()).thenReturn("random-state");
        when(oidcProviderService.buildAuthorizationUrl(anyString(), anyString(),
                anyString(), anyString()))
                .thenReturn("https://auth.example.com/auth?...");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        HttpSession session = mock(HttpSession.class);

        controller.oidcLogin(request, session);

        verify(session).setAttribute(eq("oidc_redirect_uri"),
                eq("http://localhost:8080/setup/callback"));
    }

    // ============================================================
    // oidcCallback()
    // ============================================================
    @Test
    @DisplayName("oidcCallback()でstate不一致ならエラーパラメーター付きでstep3にリダイレクト")
    void oidcCallback_stateMismatch_redirectsWithError() {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("oidc_state")).thenReturn("expected-state");

        String view = controller.oidcCallback("auth-code", "wrong-state", session);

        assertThat(view).isEqualTo("redirect:/setup/step3?error=state_mismatch");
        verify(oidcProviderService, never()).getMetadata(anyString());
    }

    @Test
    @DisplayName("oidcCallback()で正常な場合はsessionDataにiss/aud/subを設定してstep3にリダイレクト")
    void oidcCallback_success_setsSessionDataAndRedirects() throws Exception {
        OidcSetting oidc = new OidcSetting();
        oidc.setUri("https://auth.example.com/realms/master");
        oidc.setClientId("test-client");
        oidc.setClientSecret("test-secret");
        setupSessionData.getOidcSettings().add(oidc);

        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("oidc_state")).thenReturn("matching-state");
        when(session.getAttribute("oidc_redirect_uri"))
                .thenReturn("https://app.example.com/setup/callback");

        when(oidcProviderService.getMetadata(anyString())).thenReturn(
                Map.of("token_endpoint", "https://auth.example.com/token"));

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("https://auth.example.com/realms/master")
                .subject("user-uuid-123")
                .build();
        when(oidcProviderService.exchangeCodeForClaims(
                anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(claims);

        String view = controller.oidcCallback("auth-code", "matching-state", session);

        assertThat(view).isEqualTo("redirect:/setup/step3");
        assertThat(setupSessionData.getAdminIss())
                .isEqualTo("https://auth.example.com/realms/master");
        assertThat(setupSessionData.getAdminAud()).isEqualTo("test-client");
        assertThat(setupSessionData.getAdminSub()).isEqualTo("user-uuid-123");
        verify(session).removeAttribute("oidc_state");
        verify(session).removeAttribute("oidc_redirect_uri");
    }

}