package io.github.kizulog_community.kizulog.infrastructure.web.system;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

/**
 * SystemLoginControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemLoginControllerTest {

    private SystemLoginController controller;
    private Model model;

    @BeforeEach
    void setUp() {
        controller = new SystemLoginController();
        model = new ConcurrentModel();
    }

    @Test
    @DisplayName("showLoginPage: ビュー名system/loginを返す")
    void showLoginPage_returnsCorrectViewName() {
        String view = controller.showLoginPage(null, null, model);
        assertThat(view).isEqualTo("system/login");
    }

    @Test
    @DisplayName("showLoginPage: クエリ未指定時、error=false logout=falseがmodelに設定される")
    void showLoginPage_setsFalseFlagsByDefault() {
        controller.showLoginPage(null, null, model);

        Map<String, Object> attrs = toMap(model);
        assertThat(attrs.get("error")).isEqualTo(false);
        assertThat(attrs.get("logout")).isEqualTo(false);
        assertThat(attrs.get("errorCode")).isNull();
    }

    @Test
    @DisplayName("showLoginPage: ?error空文字指定時、error=true errorCode=unknownがmodelに設定される")
    void showLoginPage_setsErrorTrueAndUnknownCode_whenErrorParamIsBlank() {
        controller.showLoginPage("", null, model);

        Map<String, Object> attrs = toMap(model);
        assertThat(attrs.get("error")).isEqualTo(true);
        assertThat(attrs.get("errorCode")).isEqualTo(SystemLoginController.ERROR_CODE_UNKNOWN);
    }

    @Test
    @DisplayName("showLoginPage: ?logout指定時、logout=trueがmodelに設定される")
    void showLoginPage_setsLogoutTrue_whenLogoutParamProvided() {
        controller.showLoginPage(null, "", model);

        Map<String, Object> attrs = toMap(model);
        assertThat(attrs.get("error")).isEqualTo(false);
        assertThat(attrs.get("logout")).isEqualTo(true);
    }

    @Test
    @DisplayName("showLoginPage: oauth2LoginUrl属性が/oauth2/authorization/masterに設定される")
    void showLoginPage_setsOauth2LoginUrl() {
        controller.showLoginPage(null, null, model);

        Map<String, Object> attrs = toMap(model);
        assertThat(attrs.get("oauth2LoginUrl"))
                .isEqualTo("/oauth2/authorization/master");
    }

    @Test
    @DisplayName("showLoginPage: 既知エラーコード（ACCOUNT_NOT_FOUND）はそのままmodelに設定される")
    void showLoginPage_passesKnownErrorCode_accountNotFound() {
        controller.showLoginPage("ACCOUNT_NOT_FOUND", null, model);

        Map<String, Object> attrs = toMap(model);
        assertThat(attrs.get("error")).isEqualTo(true);
        assertThat(attrs.get("errorCode")).isEqualTo("ACCOUNT_NOT_FOUND");
    }

    @Test
    @DisplayName("showLoginPage: 既知エラーコード（ACCOUNT_INACTIVE）はそのままmodelに設定される")
    void showLoginPage_passesKnownErrorCode_accountInactive() {
        controller.showLoginPage("ACCOUNT_INACTIVE", null, model);

        Map<String, Object> attrs = toMap(model);
        assertThat(attrs.get("errorCode")).isEqualTo("ACCOUNT_INACTIVE");
    }

    @Test
    @DisplayName("showLoginPage: 既知エラーコード（ROLE_NOT_GRANTED）はそのままmodelに設定される")
    void showLoginPage_passesKnownErrorCode_roleNotGranted() {
        controller.showLoginPage("ROLE_NOT_GRANTED", null, model);

        Map<String, Object> attrs = toMap(model);
        assertThat(attrs.get("errorCode")).isEqualTo("ROLE_NOT_GRANTED");
    }

    @Test
    @DisplayName("showLoginPage: 未知エラーコードはunknownに正規化される（XSS対策）")
    void showLoginPage_normalizesUnknownErrorCode() {
        controller.showLoginPage("<script>alert(1)</script>", null, model);

        Map<String, Object> attrs = toMap(model);
        assertThat(attrs.get("error")).isEqualTo(true);
        assertThat(attrs.get("errorCode")).isEqualTo(SystemLoginController.ERROR_CODE_UNKNOWN);
    }

    @Test
    @DisplayName("showLoginPage: 任意のenum名でない値はunknownに正規化される")
    void showLoginPage_normalizesArbitraryString() {
        controller.showLoginPage("not_a_valid_code", null, model);

        Map<String, Object> attrs = toMap(model);
        assertThat(attrs.get("errorCode")).isEqualTo(SystemLoginController.ERROR_CODE_UNKNOWN);
    }

    /**
     * Modelの属性をMapに変換するヘルパー
     */
    private static Map<String, Object> toMap(Model model) {
        return new HashMap<>(model.asMap());
    }

}
