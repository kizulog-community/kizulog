package io.github.kizulog_community.kizulog.infrastructure.web.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;

/**
 * SystemAccountsControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemAccountsControllerTest {

    private final SystemAccountsController controller = new SystemAccountsController();

    @Test
    @DisplayName("accounts: activeMenuがmodelに設定され、view名がsystem/accountsである")
    void accounts_setsModelAttributesAndReturnsView() {
        // given
        Model model = mock(Model.class);

        // when
        String view = controller.accounts(model);

        // then
        assertThat(view).isEqualTo("system/accounts");
        verify(model).addAttribute("activeMenu", "accounts");
    }

}
