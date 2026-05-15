package io.github.kizulog_community.kizulog.infrastructure.security.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.service.AccountLocalizationApplicationService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;

/**
 * SystemAuthenticationSuccessHandlerの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemAuthenticationSuccessHandlerTest {

    private static final String ACCOUNT_ID = "account-001";

    private AccountLocalizationApplicationService accountLocalizationApplicationService;
    private SessionLocaleResolver localeResolver;
    private SystemAuthenticationSuccessHandler handler;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Authentication authentication;
    private SystemUserPrincipal principal;

    @BeforeEach
    void setUp() {
        accountLocalizationApplicationService =
                mock(AccountLocalizationApplicationService.class);
        localeResolver = mock(SessionLocaleResolver.class);
        handler = new SystemAuthenticationSuccessHandler(
                accountLocalizationApplicationService, localeResolver);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        authentication = mock(Authentication.class);
        principal = mock(SystemUserPrincipal.class);
        when(principal.getAccountId()).thenReturn(ACCOUNT_ID);
        when(authentication.getPrincipal()).thenReturn(principal);
    }

    @Test
    @DisplayName("onAuthenticationSuccess: 言語・TZ両方解決できる場合、セッションに即時反映される")
    void onAuthenticationSuccess_appliesLocaleAndTimezoneToSession() throws Exception {
        when(accountLocalizationApplicationService.resolveEffectiveLanguage(ACCOUNT_ID))
                .thenReturn(Optional.of(SupportedLanguage.JA));
        when(accountLocalizationApplicationService.resolveEffectiveTimezone(ACCOUNT_ID))
                .thenReturn(Optional.of(SupportedTimezone.of("Asia/Tokyo")));

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<LocaleContext> ctxCap = ArgumentCaptor.forClass(LocaleContext.class);
        verify(localeResolver).setLocaleContext(
                eq(request), eq(response), ctxCap.capture());

        LocaleContext ctx = ctxCap.getValue();
        assertThat(ctx).isInstanceOf(SimpleTimeZoneAwareLocaleContext.class);
        assertThat(ctx.getLocale()).isEqualTo(SupportedLanguage.JA.getLocale());

        TimeZoneAwareLocaleContext tzCtx = (TimeZoneAwareLocaleContext) ctx;
        assertThat(tzCtx.getTimeZone().getID()).isEqualTo("Asia/Tokyo");
    }

    @Test
    @DisplayName("onAuthenticationSuccess: 反映成功後、/system/dashboardへリダイレクトする")
    void onAuthenticationSuccess_redirectsToDashboard() throws Exception {
        when(accountLocalizationApplicationService.resolveEffectiveLanguage(ACCOUNT_ID))
                .thenReturn(Optional.of(SupportedLanguage.JA));
        when(accountLocalizationApplicationService.resolveEffectiveTimezone(ACCOUNT_ID))
                .thenReturn(Optional.of(SupportedTimezone.of("Asia/Tokyo")));

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).isEqualTo("/system/dashboard");
    }

    @Test
    @DisplayName("onAuthenticationSuccess: 英語+UTCの場合も正しく反映される")
    void onAuthenticationSuccess_englishAndUtc() throws Exception {
        when(accountLocalizationApplicationService.resolveEffectiveLanguage(ACCOUNT_ID))
                .thenReturn(Optional.of(SupportedLanguage.EN));
        when(accountLocalizationApplicationService.resolveEffectiveTimezone(ACCOUNT_ID))
                .thenReturn(Optional.of(SupportedTimezone.of("UTC")));

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<LocaleContext> ctxCap = ArgumentCaptor.forClass(LocaleContext.class);
        verify(localeResolver).setLocaleContext(any(), any(), ctxCap.capture());

        TimeZoneAwareLocaleContext tzCtx = (TimeZoneAwareLocaleContext) ctxCap.getValue();
        assertThat(tzCtx.getLocale()).isEqualTo(SupportedLanguage.EN.getLocale());
        assertThat(tzCtx.getTimeZone().getID()).isEqualTo("UTC");
    }

    @Test
    @DisplayName("onAuthenticationSuccess: 言語が空Optionalの場合、Locale反映をスキップ")
    void onAuthenticationSuccess_languageEmpty_skipsLocaleApply() throws Exception {
        when(accountLocalizationApplicationService.resolveEffectiveLanguage(ACCOUNT_ID))
                .thenReturn(Optional.empty());
        when(accountLocalizationApplicationService.resolveEffectiveTimezone(ACCOUNT_ID))
                .thenReturn(Optional.of(SupportedTimezone.of("Asia/Tokyo")));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(localeResolver, never()).setLocaleContext(any(), any(), any());
        // ログイン自体は続行（リダイレクトされる）
        assertThat(response.getRedirectedUrl()).isEqualTo("/system/dashboard");
    }

    @Test
    @DisplayName("onAuthenticationSuccess: TZが空Optionalの場合、Locale反映をスキップ")
    void onAuthenticationSuccess_timezoneEmpty_skipsLocaleApply() throws Exception {
        when(accountLocalizationApplicationService.resolveEffectiveLanguage(ACCOUNT_ID))
                .thenReturn(Optional.of(SupportedLanguage.JA));
        when(accountLocalizationApplicationService.resolveEffectiveTimezone(ACCOUNT_ID))
                .thenReturn(Optional.empty());

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(localeResolver, never()).setLocaleContext(any(), any(), any());
        assertThat(response.getRedirectedUrl()).isEqualTo("/system/dashboard");
    }

    @Test
    @DisplayName("onAuthenticationSuccess: 両方空Optionalの場合、Locale反映をスキップ")
    void onAuthenticationSuccess_bothEmpty_skipsLocaleApply() throws Exception {
        when(accountLocalizationApplicationService.resolveEffectiveLanguage(ACCOUNT_ID))
                .thenReturn(Optional.empty());
        when(accountLocalizationApplicationService.resolveEffectiveTimezone(ACCOUNT_ID))
                .thenReturn(Optional.empty());

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(localeResolver, never()).setLocaleContext(any(), any(), any());
        assertThat(response.getRedirectedUrl()).isEqualTo("/system/dashboard");
    }

    @Test
    @DisplayName("onAuthenticationSuccess: PrincipalがSystemUserPrincipalでない場合、Locale反映をスキップ")
    void onAuthenticationSuccess_nonSystemPrincipal_skipsLocaleApply() throws Exception {
        Object nonSystemPrincipal = "string-principal"; // 意図的に別型
        when(authentication.getPrincipal()).thenReturn(nonSystemPrincipal);

        handler.onAuthenticationSuccess(request, response, authentication);

        // Service自体呼ばれない
        verify(accountLocalizationApplicationService, never())
                .resolveEffectiveLanguage(any());
        verify(accountLocalizationApplicationService, never())
                .resolveEffectiveTimezone(any());
        verify(localeResolver, never()).setLocaleContext(any(), any(), any());
        assertThat(response.getRedirectedUrl()).isEqualTo("/system/dashboard");
    }

    @Test
    @DisplayName("onAuthenticationSuccess: Principalがnullの場合、Locale反映をスキップ")
    void onAuthenticationSuccess_nullPrincipal_skipsLocaleApply() throws Exception {
        when(authentication.getPrincipal()).thenReturn(null);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(accountLocalizationApplicationService, never())
                .resolveEffectiveLanguage(any());
        verify(localeResolver, never()).setLocaleContext(any(), any(), any());
        assertThat(response.getRedirectedUrl()).isEqualTo("/system/dashboard");
    }

    @Test
    @DisplayName("onAuthenticationSuccess: accountIdがnullの場合、Locale反映をスキップ")
    void onAuthenticationSuccess_nullAccountId_skipsLocaleApply() throws Exception {
        when(principal.getAccountId()).thenReturn(null);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(accountLocalizationApplicationService, never())
                .resolveEffectiveLanguage(any());
        verify(localeResolver, never()).setLocaleContext(any(), any(), any());
        assertThat(response.getRedirectedUrl()).isEqualTo("/system/dashboard");
    }

    @Test
    @DisplayName("onAuthenticationSuccess: accountIdが空文字の場合、Locale反映をスキップ")
    void onAuthenticationSuccess_blankAccountId_skipsLocaleApply() throws Exception {
        when(principal.getAccountId()).thenReturn("");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(accountLocalizationApplicationService, never())
                .resolveEffectiveLanguage(any());
        verify(localeResolver, never()).setLocaleContext(any(), any(), any());
        assertThat(response.getRedirectedUrl()).isEqualTo("/system/dashboard");
    }

    @Test
    @DisplayName("onAuthenticationSuccess: Service例外発生時もログイン続行（例外伝播しない）")
    void onAuthenticationSuccess_serviceThrows_continuesLoginGracefully() throws Exception {
        when(accountLocalizationApplicationService.resolveEffectiveLanguage(ACCOUNT_ID))
                .thenThrow(new RuntimeException("simulated DB error"));

        // 例外が伝播せずリダイレクトに到達することを確認
        handler.onAuthenticationSuccess(request, response, authentication);

        verify(localeResolver, never()).setLocaleContext(any(), any(), any());
        assertThat(response.getRedirectedUrl()).isEqualTo("/system/dashboard");
    }

    @Test
    @DisplayName("onAuthenticationSuccess: localeResolver例外発生時もログイン続行")
    void onAuthenticationSuccess_localeResolverThrows_continuesLoginGracefully() throws Exception {
        when(accountLocalizationApplicationService.resolveEffectiveLanguage(ACCOUNT_ID))
                .thenReturn(Optional.of(SupportedLanguage.JA));
        when(accountLocalizationApplicationService.resolveEffectiveTimezone(ACCOUNT_ID))
                .thenReturn(Optional.of(SupportedTimezone.of("Asia/Tokyo")));
        org.mockito.Mockito.doThrow(new RuntimeException("simulated session error"))
                .when(localeResolver).setLocaleContext(any(), any(), any());

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo("/system/dashboard");
    }

}
