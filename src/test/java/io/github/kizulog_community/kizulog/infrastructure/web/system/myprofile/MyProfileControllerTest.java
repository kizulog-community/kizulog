package io.github.kizulog_community.kizulog.infrastructure.web.system.myprofile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

/**
 * MyProfileControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class MyProfileControllerTest {

    private MyProfileController controller;
    private Model model;

    @BeforeEach
    void setUp() {
        controller = new MyProfileController();
        model = new ConcurrentModel();
    }

    @Test
    @DisplayName("index: マイプロフィールメニュー画面を表示し、activeMenuが設定される")
    void index_returnsViewAndSetsActiveMenu() {
        String view = controller.index(model);

        assertThat(view).isEqualTo("system/my-profile/index");
        assertThat(model.getAttribute("activeMenu")).isEqualTo("my-profile");
    }

}
