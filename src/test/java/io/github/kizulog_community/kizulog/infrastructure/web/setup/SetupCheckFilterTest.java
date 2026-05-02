package io.github.kizulog_community.kizulog.infrastructure.web.setup;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.kizulog_community.kizulog.domain.systemconfig.model.SystemConfig;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.SystemConfigService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * SetupCheckFilterの単体テスト
 *
 * @author Jun Kobayashi
 */
class SetupCheckFilterTest {

    private SystemConfigService systemConfigService;
    private SetupCheckFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        systemConfigService = mock(SystemConfigService.class);
        filter = new SetupCheckFilter(systemConfigService);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        when(request.getContextPath()).thenReturn("");
    }

    /**
     * セットアップ完了状態のSystemConfigを返すモックを設定
     */
    private void mockSetupCompleted() {
        SystemConfig config = new SystemConfig(
                "OIDC",
                OffsetDateTime.now(ZoneOffset.UTC),
                "[{\"id\":\"master\"}]",
                OffsetDateTime.now(ZoneOffset.UTC),
                "test-user");
        when(systemConfigService.findLatestByKey("OIDC"))
                .thenReturn(Optional.of(config));
    }

    /**
     * セットアップ未完了状態（OIDC設定なし）のモックを設定
     */
    private void mockSetupNotCompleted() {
        when(systemConfigService.findLatestByKey("OIDC"))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("/css/ で始まるリクエストはフィルターをスキップしてchainを呼ぶ")
    void doFilter_cssPath_skipsFilter() throws Exception {
        when(request.getRequestURI()).thenReturn("/css/main.css");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(systemConfigService, never()).findLatestByKey(anyString());
    }

    @Test
    @DisplayName("/js/ で始まるリクエストはフィルターをスキップしてchainを呼ぶ")
    void doFilter_jsPath_skipsFilter() throws Exception {
        when(request.getRequestURI()).thenReturn("/js/main.js");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("/images/ で始まるリクエストはフィルターをスキップしてchainを呼ぶ")
    void doFilter_imagesPath_skipsFilter() throws Exception {
        when(request.getRequestURI()).thenReturn("/images/logo.png");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("/favicon で始まるリクエストはフィルターをスキップしてchainを呼ぶ")
    void doFilter_faviconPath_skipsFilter() throws Exception {
        when(request.getRequestURI()).thenReturn("/favicon.ico");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("/setup/complete は常にchainを呼ぶ")
    void doFilter_completePath_alwaysAllowed() throws Exception {
        when(request.getRequestURI()).thenReturn("/setup/complete");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("セットアップ未完了で /setup/ 以外のパスは /setup/step0 にリダイレクト")
    void doFilter_setupNotCompleted_nonSetupPath_redirectsToStep0() throws Exception {
        mockSetupNotCompleted();
        when(request.getRequestURI()).thenReturn("/dashboard");

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/setup/step0");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("セットアップ未完了で /setup/ 配下のパスはchainを呼ぶ")
    void doFilter_setupNotCompleted_setupPath_callsChain() throws Exception {
        mockSetupNotCompleted();
        when(request.getRequestURI()).thenReturn("/setup/step1");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("セットアップ完了で /setup/ 配下のパスはFORBIDDENを返す")
    void doFilter_setupCompleted_setupPath_returnsForbidden() throws Exception {
        mockSetupCompleted();
        when(request.getRequestURI()).thenReturn("/setup/step1");

        filter.doFilter(request, response, chain);

        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN));
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("セットアップ完了で /setup/ 以外のパスはchainを呼ぶ")
    void doFilter_setupCompleted_nonSetupPath_callsChain() throws Exception {
        mockSetupCompleted();
        when(request.getRequestURI()).thenReturn("/dashboard");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("OIDC設定が空配列の場合はセットアップ未完了と判定")
    void doFilter_oidcEmptyArray_treatedAsNotCompleted() throws Exception {
        SystemConfig config = new SystemConfig(
                "OIDC",
                OffsetDateTime.now(ZoneOffset.UTC),
                "[]",
                OffsetDateTime.now(ZoneOffset.UTC),
                "test-user");
        when(systemConfigService.findLatestByKey("OIDC"))
                .thenReturn(Optional.of(config));
        when(request.getRequestURI()).thenReturn("/dashboard");

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/setup/step0");
    }

    @Test
    @DisplayName("OIDC設定の値が空文字の場合はセットアップ未完了と判定")
    void doFilter_oidcEmptyString_treatedAsNotCompleted() throws Exception {
        SystemConfig config = new SystemConfig(
                "OIDC",
                OffsetDateTime.now(ZoneOffset.UTC),
                "",
                OffsetDateTime.now(ZoneOffset.UTC),
                "test-user");
        when(systemConfigService.findLatestByKey("OIDC"))
                .thenReturn(Optional.of(config));
        when(request.getRequestURI()).thenReturn("/dashboard");

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/setup/step0");
    }

    @Test
    @DisplayName("コンテキストパスがある場合は /context/setup/step0 にリダイレクト")
    void doFilter_withContextPath_redirectsCorrectly() throws Exception {
        when(request.getContextPath()).thenReturn("/context");
        mockSetupNotCompleted();
        when(request.getRequestURI()).thenReturn("/context/dashboard");

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/context/setup/step0");
    }

}