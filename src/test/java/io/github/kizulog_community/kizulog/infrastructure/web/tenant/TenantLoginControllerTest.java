package io.github.kizulog_community.kizulog.infrastructure.web.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import io.github.kizulog_community.kizulog.domain.tenant.model.Tenant;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderChoiceView;
import io.github.kizulog_community.kizulog.domain.tenantoidc.service.TenantOidcProviderService;

/**
 * TenantLoginControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantLoginControllerTest {

    private static final String TENANT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final OffsetDateTime NOW =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private TenantOidcProviderService tenantOidcProviderService;
    private TenantLoginController controller;
    private Model model;

    @BeforeEach
    void setUp() {
        tenantOidcProviderService = mock(TenantOidcProviderService.class);
        controller = new TenantLoginController(tenantOidcProviderService);
        model = new ConcurrentModel();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Map<String, Object> toMap(Model model) {
        return new HashMap<>(model.asMap());
    }

    private void setTenantWithProviders() {
        TenantContext.set(new Tenant(TENANT_ID, NOW, "テナントA", "tenant-a", NOW, "creator"));
        when(tenantOidcProviderService.findEnabledChoicesByTenantId(TENANT_ID))
                .thenReturn(List.of(
                        new TenantOidcProviderChoiceView(
                                "tenant-" + TENANT_ID + "-keycloak", "Keycloak")));
    }

    @Test
    @DisplayName("showLoginPage: ビュー名tenant/loginを返す")
    void showLoginPage_returnsCorrectViewName() {
        setTenantWithProviders();

        String view = controller.showLoginPage(null, null, model);

        assertThat(view).isEqualTo("tenant/login");
    }

    @Test
    @DisplayName("showLoginPage: クエリ未指定時、error=false logout=falseがmodelに設定される")
    void showLoginPage_setsFalseFlagsByDefault() {
        setTenantWithProviders();

        controller.showLoginPage(null, null, model);

        Map<String, Object> attrs = toMap(model);
        assertThat(attrs.get("error")).isEqualTo(false);
        assertThat(attrs.get("logout")).isEqualTo(false);
        assertThat(attrs.get("errorCode")).isNull();
    }

    @Test
    @DisplayName("showLoginPage: ?error空文字指定時、error=true errorCode=unknownがmodelに設定される")
    void showLoginPage_setsErrorTrueAndUnknownCode_whenErrorParamIsBlank() {
        setTenantWithProviders();

        controller.showLoginPage("", null, model);

        Map<String, Object> attrs = toMap(model);
        assertThat(attrs.get("error")).isEqualTo(true);
        assertThat(attrs.get("errorCode")).isEqualTo(TenantLoginController.ERROR_CODE_UNKNOWN);
    }

    @Test
    @DisplayName("showLoginPage: 既知エラーコードACCOUNT_NOT_FOUNDはそのまま渡される")
    void showLoginPage_passesKnownErrorCode_accountNotFound() {
        setTenantWithProviders();

        controller.showLoginPage("ACCOUNT_NOT_FOUND", null, model);

        assertThat(toMap(model).get("errorCode")).isEqualTo("ACCOUNT_NOT_FOUND");
    }

    @Test
    @DisplayName("showLoginPage: 既知エラーコードROLE_NOT_GRANTEDはそのまま渡される")
    void showLoginPage_passesKnownErrorCode_roleNotGranted() {
        setTenantWithProviders();

        controller.showLoginPage("ROLE_NOT_GRANTED", null, model);

        assertThat(toMap(model).get("errorCode")).isEqualTo("ROLE_NOT_GRANTED");
    }

    @Test
    @DisplayName("showLoginPage: 既知エラーコードIDENTITY_INACTIVEはそのまま渡される")
    void showLoginPage_passesKnownErrorCode_identityInactive() {
        setTenantWithProviders();

        controller.showLoginPage("IDENTITY_INACTIVE", null, model);

        assertThat(toMap(model).get("errorCode")).isEqualTo("IDENTITY_INACTIVE");
    }

    @Test
    @DisplayName("showLoginPage: 既知エラーコードACCOUNT_INACTIVEはそのまま渡される")
    void showLoginPage_passesKnownErrorCode_accountInactive() {
        setTenantWithProviders();

        controller.showLoginPage("ACCOUNT_INACTIVE", null, model);

        assertThat(toMap(model).get("errorCode")).isEqualTo("ACCOUNT_INACTIVE");
    }

    @Test
    @DisplayName("showLoginPage: 未知エラーコードはunknownに正規化される（XSS対策）")
    void showLoginPage_normalizesUnknownErrorCode() {
        setTenantWithProviders();

        controller.showLoginPage("<script>alert(1)</script>", null, model);

        assertThat(toMap(model).get("errorCode")).isEqualTo(TenantLoginController.ERROR_CODE_UNKNOWN);
    }

    @Test
    @DisplayName("showLoginPage: ?logout指定時、logout=trueがmodelに設定される")
    void showLoginPage_setsLogoutTrue_whenLogoutParamPresent() {
        setTenantWithProviders();

        controller.showLoginPage(null, "", model);

        Map<String, Object> attrs = toMap(model);
        assertThat(attrs.get("logout")).isEqualTo(true);
        assertThat(attrs.get("error")).isEqualTo(false);
    }

    @Test
    @DisplayName("showLoginPage: TenantContext解決済み時、ENABLEDプロバイダー一覧がmodelに設定される")
    void showLoginPage_setsProviders_whenTenantResolved() {
        setTenantWithProviders();

        controller.showLoginPage(null, null, model);

        @SuppressWarnings("unchecked")
        List<TenantOidcProviderChoiceView> providers =
                (List<TenantOidcProviderChoiceView>) toMap(model).get("providers");
        assertThat(providers).hasSize(1);
        assertThat(providers.get(0).getRegistrationId())
                .isEqualTo("tenant-" + TENANT_ID + "-keycloak");
        assertThat(providers.get(0).getDisplayName()).isEqualTo("Keycloak");
    }

    @Test
    @DisplayName("showLoginPage: TenantContext未解決時、providersは空リストでサービスを呼ばない")
    void showLoginPage_noTenantContext_setsEmptyProviders() {
        // TenantContext を設定しない

        String view = controller.showLoginPage(null, null, model);

        assertThat(view).isEqualTo("tenant/login");
        @SuppressWarnings("unchecked")
        List<TenantOidcProviderChoiceView> providers =
                (List<TenantOidcProviderChoiceView>) toMap(model).get("providers");
        assertThat(providers).isEmpty();
        verify(tenantOidcProviderService, never()).findEnabledChoicesByTenantId(anyString());
    }

}
