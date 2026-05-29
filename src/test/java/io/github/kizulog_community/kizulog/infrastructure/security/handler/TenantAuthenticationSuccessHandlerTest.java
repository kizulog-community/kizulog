package io.github.kizulog_community.kizulog.infrastructure.security.handler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * TenantAuthenticationSuccessHandlerの単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantAuthenticationSuccessHandlerTest {

    private TenantAuthenticationSuccessHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        handler = new TenantAuthenticationSuccessHandler();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("onAuthenticationSuccess: SavedRequestがない場合はダッシュボード /dashboard へ遷移")
    void onSuccess_withoutSavedRequest_redirectsToDashboard() throws Exception {
        Authentication auth = new TestingAuthenticationToken("user", "creds");

        handler.onAuthenticationSuccess(request, response, auth);

        assertThat(response.getRedirectedUrl()).isEqualTo("/dashboard");
    }

    @Test
    @DisplayName("onAuthenticationSuccess: contextPathがある場合は /dashboard 相対で遷移する")
    void onSuccess_withContextPath_redirectsToContextDashboard() throws Exception {
        request.setContextPath("/app");
        Authentication auth = new TestingAuthenticationToken("user", "creds");

        handler.onAuthenticationSuccess(request, response, auth);

        assertThat(response.getRedirectedUrl()).isEqualTo("/app/dashboard");
    }

}
