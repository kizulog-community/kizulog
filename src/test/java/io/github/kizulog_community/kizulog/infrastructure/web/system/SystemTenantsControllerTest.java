package io.github.kizulog_community.kizulog.infrastructure.web.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;

/**
 * SystemTenantsControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemTenantsControllerTest {

    private final SystemTenantsController controller = new SystemTenantsController();

    @Test
    @DisplayName("tenants: activeMenuがmodelに設定され、view名がsystem/tenantsである")
    void tenants_setsModelAttributesAndReturnsView() {
        // given
        Model model = mock(Model.class);

        // when
        String view = controller.tenants(model);

        // then
        assertThat(view).isEqualTo("system/tenants");
        verify(model).addAttribute("activeMenu", "tenants");
    }

}
