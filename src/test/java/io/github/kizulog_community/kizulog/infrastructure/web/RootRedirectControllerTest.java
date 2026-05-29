package io.github.kizulog_community.kizulog.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import io.github.kizulog_community.kizulog.infrastructure.security.matcher.SystemHostMatcher;

/**
 * RootRedirectControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class RootRedirectControllerTest {

    private SystemHostMatcher systemHostMatcher;
    private RootRedirectController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        systemHostMatcher = mock(SystemHostMatcher.class);
        controller = new RootRedirectController(systemHostMatcher);
        request = new MockHttpServletRequest();
    }

    @Test
    @DisplayName("root: システムホストなら /system/dashboard へリダイレクトする")
    void root_systemHost_redirectsToSystemDashboard() {
        when(systemHostMatcher.matches(request)).thenReturn(true);

        String view = controller.root(request);

        assertThat(view).isEqualTo("redirect:/system/dashboard");
    }

    @Test
    @DisplayName("root: テナントホスト（非システムホスト）なら /dashboard へリダイレクトする")
    void root_tenantHost_redirectsToTenantDashboard() {
        when(systemHostMatcher.matches(request)).thenReturn(false);

        String view = controller.root(request);

        assertThat(view).isEqualTo("redirect:/dashboard");
    }

}
