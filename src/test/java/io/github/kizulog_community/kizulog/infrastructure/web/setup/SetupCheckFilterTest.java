package io.github.kizulog_community.kizulog.infrastructure.web.setup;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.kizulog_community.kizulog.domain.systemoidc.port.SystemOidcProviderRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * SetupCheckFilterの単体テスト
 *
 * @author Jun Kobayashi
 */
class SetupCheckFilterTest {

    private SystemOidcProviderRepository systemOidcProviderRepository;
    private SetupCheckFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        systemOidcProviderRepository = mock(SystemOidcProviderRepository.class);
        filter = new SetupCheckFilter(systemOidcProviderRepository);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        when(request.getContextPath()).thenReturn("");
    }

    private void mockSetupCompleted() {
        when(systemOidcProviderRepository.existsAny()).thenReturn(true);
    }

    private void mockSetupNotCompleted() {
        when(systemOidcProviderRepository.existsAny()).thenReturn(false);
    }

    @Test
    @DisplayName("/css/ で始まるリクエストはフィルターをスキップしてchainを呼ぶ")
    void doFilter_cssPath_skipsFilter() throws Exception {
        when(request.getRequestURI()).thenReturn("/css/main.css");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(systemOidcProviderRepository, never()).existsAny();
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
    @DisplayName("セットアップ完了で /setup/step0 へのアクセスもFORBIDDENを返す")
    void doFilter_setupCompleted_step0Path_returnsForbidden() throws Exception {
        mockSetupCompleted();
        when(request.getRequestURI()).thenReturn("/setup/step0");

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
    @DisplayName("コンテキストパスがある場合は /context/setup/step0 にリダイレクト")
    void doFilter_withContextPath_redirectsCorrectly() throws Exception {
        when(request.getContextPath()).thenReturn("/context");
        mockSetupNotCompleted();
        when(request.getRequestURI()).thenReturn("/context/dashboard");

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/context/setup/step0");
    }

    @Test
    @DisplayName("セットアップ完了でも /setup/complete はchainを呼ぶ")
    void doFilter_setupCompleted_completePath_callsChain() throws Exception {
        when(request.getRequestURI()).thenReturn("/setup/complete");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(systemOidcProviderRepository, never()).existsAny();
    }

}
