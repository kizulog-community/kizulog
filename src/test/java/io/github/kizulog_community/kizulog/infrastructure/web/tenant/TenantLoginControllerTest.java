package io.github.kizulog_community.kizulog.infrastructure.web.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

/**
 * TenantLoginControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantLoginControllerTest {

    private TenantLoginController controller;
    private Model model;

    @BeforeEach
    void setUp() {
        controller = new TenantLoginController();
        model = new ConcurrentModel();
    }

    private Map<String, Object> toMap(Model model) {
        return new HashMap<>(model.asMap());
    }

    @Test
    @DisplayName("showLoginPage: ビュー名tenant/loginを返す")
    void showLoginPage_returnsCorrectViewName() {
        String view = controller.showLoginPage(null, null, model);
        assertThat(view).isEqualTo("tenant/login");
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
        assertThat(attrs.get("errorCode")).isEqualTo(TenantLoginController.ERROR_CODE_UNKNOWN);
    }

    @Test
    @DisplayName("showLoginPage: 既知エラーコードACCOUNT_NOT_FOUNDはそのまま渡される")
    void showLoginPage_passesKnownErrorCode_accountNotFound() {
        controller.showLoginPage("ACCOUNT_NOT_FOUND", null, model);

        assertThat(toMap(model).get("errorCode")).isEqualTo("ACCOUNT_NOT_FOUND");
    }

    @Test
    @DisplayName("showLoginPage: 既知エラーコードROLE_NOT_GRANTEDはそのまま渡される")
    void showLoginPage_passesKnownErrorCode_roleNotGranted() {
        controller.showLoginPage("ROLE_NOT_GRANTED", null, model);

        assertThat(toMap(model).get("errorCode")).isEqualTo("ROLE_NOT_GRANTED");
    }

    @Test
    @DisplayName("showLoginPage: 既知エラーコードIDENTITY_INACTIVEはそのまま渡される")
    void showLoginPage_passesKnownErrorCode_identityInactive() {
        controller.showLoginPage("IDENTITY_INACTIVE", null, model);

        assertThat(toMap(model).get("errorCode")).isEqualTo("IDENTITY_INACTIVE");
    }

    @Test
    @DisplayName("showLoginPage: 既知エラーコードACCOUNT_INACTIVEはそのまま渡される")
    void showLoginPage_passesKnownErrorCode_accountInactive() {
        controller.showLoginPage("ACCOUNT_INACTIVE", null, model);

        assertThat(toMap(model).get("errorCode")).isEqualTo("ACCOUNT_INACTIVE");
    }

    @Test
    @DisplayName("showLoginPage: 未知エラーコードはunknownに正規化される（XSS対策）")
    void showLoginPage_normalizesUnknownErrorCode() {
        controller.showLoginPage("<script>alert(1)</script>", null, model);

        assertThat(toMap(model).get("errorCode")).isEqualTo(TenantLoginController.ERROR_CODE_UNKNOWN);
    }

    @Test
    @DisplayName("showLoginPage: ?logout指定時、logout=trueがmodelに設定される")
    void showLoginPage_setsLogoutTrue_whenLogoutParamPresent() {
        controller.showLoginPage(null, "", model);

        Map<String, Object> attrs = toMap(model);
        assertThat(attrs.get("logout")).isEqualTo(true);
        assertThat(attrs.get("error")).isEqualTo(false);
    }

}
