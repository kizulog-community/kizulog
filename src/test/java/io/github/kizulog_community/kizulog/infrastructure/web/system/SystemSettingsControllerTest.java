package io.github.kizulog_community.kizulog.infrastructure.web.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;

/**
 * SystemSettingsControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemSettingsControllerTest {

    private final SystemSettingsController controller = new SystemSettingsController();

    @Test
    @DisplayName("systemSettings: activeMenuがmodelに設定され、view名がsystem/system-settingsである")
    void systemSettings_setsModelAttributesAndReturnsView() {
        // given
        Model model = mock(Model.class);

        // when
        String view = controller.systemSettings(model);

        // then
        assertThat(view).isEqualTo("system/system-settings");
        verify(model).addAttribute("activeMenu", "system-settings");
    }

}
