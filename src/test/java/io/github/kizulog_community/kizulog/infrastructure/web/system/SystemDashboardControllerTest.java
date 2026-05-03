package io.github.kizulog_community.kizulog.infrastructure.web.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.ui.Model;

import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;

/**
 * SystemDashboardControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemDashboardControllerTest {

    private final SystemDashboardController controller = new SystemDashboardController();

    /**
     * テスト用OidcIdTokenを構築する.
     *
     * @return ID Token
     */
    private static OidcIdToken buildIdToken() {
        return new OidcIdToken(
                "dummy-token-value",
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(3600),
                Map.of(
                        "iss", "https://auth.example.com/realms/kizulog",
                        "aud", "kizulog-system",
                        "sub", "user-sub-1"));
    }

    @Test
    @DisplayName("dashboard: principalとactiveMenuがmodelに設定され、view名がsystem/dashboardである")
    void dashboard_setsModelAttributesAndReturnsView() {
        // given
        SystemUserPrincipal principal = SystemUserPrincipal.ofSystemAdmin(
                "account-id-1",
                "identity-id-1",
                "https://auth.example.com/realms/kizulog",
                "kizulog-system",
                "user-sub-1",
                buildIdToken());
        Model model = mock(Model.class);

        // when
        String view = controller.dashboard(principal, model);

        // then
        assertThat(view).isEqualTo("system/dashboard");
        verify(model).addAttribute("activeMenu", "dashboard");
        verify(model).addAttribute("principal", principal);
    }

    @Test
    @DisplayName("dashboard: principalがnullでもエラーにならず、view名がsystem/dashboardである")
    void dashboard_withNullPrincipal_returnsViewWithoutError() {
        // given
        Model model = mock(Model.class);

        // when
        String view = controller.dashboard(null, model);

        // then
        assertThat(view).isEqualTo("system/dashboard");
        verify(model).addAttribute("activeMenu", "dashboard");
        verify(model).addAttribute("principal", null);
    }

}
