package io.github.kizulog_community.kizulog.infrastructure.web.system.tenantoidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import io.github.kizulog_community.kizulog.domain.systemconfig.exception.OidcConnectionError;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.OidcConnectionException;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.OidcProviderService;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantDetailView;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantStatusValue;
import io.github.kizulog_community.kizulog.domain.tenant.service.TenantManagementService;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderRegistrationError;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderRegistrationException;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderStatusChangeError;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderStatusChangeException;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderUpdateError;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderUpdateException;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.EnabledTenantOidcProviderView;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderDetailView;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderListItemView;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderStatusValue;
import io.github.kizulog_community.kizulog.domain.tenantoidc.service.TenantOidcProviderService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto.OidcConnectionTestResponse;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenantoidc.dto.TenantOidcConnectionTestRequest;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenantoidc.dto.TenantOidcProviderEditForm;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenantoidc.dto.TenantOidcProviderRegistrationForm;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenantoidc.dto.TenantOidcProviderStatusChangeForm;

/**
 * SystemTenantOidcProvidersControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemTenantOidcProvidersControllerTest {

    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    private static final String TENANT_ID = "tenant-A";
    private static final String PROVIDER_ID = "google";

    private TenantOidcProviderService tenantOidcProviderService;
    private TenantManagementService tenantManagementService;
    private OidcProviderService oidcProviderService;
    private MessageSource messageSource;
    private SystemTenantOidcProvidersController controller;

    private Model model;
    private RedirectAttributes redirectAttrs;
    private SystemUserPrincipal principal;

    @BeforeEach
    void setUp() {
        tenantOidcProviderService = mock(TenantOidcProviderService.class);
        tenantManagementService = mock(TenantManagementService.class);
        oidcProviderService = mock(OidcProviderService.class);
        messageSource = mock(MessageSource.class);
        controller = new SystemTenantOidcProvidersController(
                tenantOidcProviderService,
                tenantManagementService,
                oidcProviderService,
                messageSource);

        model = new ConcurrentModel();
        redirectAttrs = new RedirectAttributesModelMap();
        principal = mock(SystemUserPrincipal.class);
        when(principal.getAccountId()).thenReturn("operator-1");
        when(messageSource.getMessage(any(String.class), any(), any(), any(Locale.class)))
                .thenReturn("dummy message");
    }

    private TenantDetailView tenantDetailViewOf(String tenantId) {
        return new TenantDetailView(
                tenantId,
                "Tenant " + tenantId,
                "slug-" + tenantId,
                BASE_TIME,
                "creator",
                TenantStatusValue.ACTIVE,
                null,
                BASE_TIME,
                "creator",
                List.of(),
                List.of());
    }

    private TenantOidcProviderDetailView providerDetailOf(
            String tenantId, String providerId, TenantOidcProviderStatusValue currentStatus) {
        return new TenantOidcProviderDetailView(
                tenantId,
                providerId,
                "Display " + providerId,
                "https://auth.example/" + providerId,
                "aud-" + providerId,
                "client-" + providerId,
                BASE_TIME,
                BASE_TIME,
                "creator",
                currentStatus,
                null,
                BASE_TIME,
                "creator",
                List.of());
    }

    private EnabledTenantOidcProviderView enabledViewOf(String providerId) {
        return new EnabledTenantOidcProviderView(providerId, "Display " + providerId);
    }

    private TenantOidcProviderListItemView listItemViewOf(
            String tenantId, String providerId, TenantOidcProviderStatusValue status) {
        return new TenantOidcProviderListItemView(
                tenantId,
                providerId,
                "Display " + providerId,
                "https://auth.example/" + providerId,
                "aud-" + providerId,
                status,
                BASE_TIME);
    }

    private void mockTenantExists(String tenantId) {
        when(tenantManagementService.findTenantDetail(tenantId))
                .thenReturn(Optional.of(tenantDetailViewOf(tenantId)));
    }

    private void mockTenantNotExists(String tenantId) {
        when(tenantManagementService.findTenantDetail(tenantId))
                .thenReturn(Optional.empty());
    }

    private BindingResult bindingResultOf(Object target, String objectName) {
        return new BeanPropertyBindingResult(target, objectName);
    }

    private BindingResult bindingResultWithError(Object target, String objectName) {
        BindingResult br = new BeanPropertyBindingResult(target, objectName);
        br.reject("test.error", "dummy error");
        return br;
    }

    @Test
    @DisplayName("list: テナントとプロバイダーが存在する場合、一覧テンプレートを返す")
    void list_returnsListTemplate_whenTenantExists() {
        mockTenantExists(TENANT_ID);
        TenantOidcProviderListItemView item = listItemViewOf(
                TENANT_ID, PROVIDER_ID, TenantOidcProviderStatusValue.ENABLED);
        when(tenantOidcProviderService.listAllByTenantId(TENANT_ID))
                .thenReturn(List.of(item));

        String view = controller.list(TENANT_ID, model, redirectAttrs);

        assertThat(view).isEqualTo("system/tenants/oidc-providers/list");
        assertThat(model.getAttribute("activeMenu")).isEqualTo("tenants");
        assertThat(model.getAttribute("tenant")).isNotNull();
        assertThat(model.getAttribute("items")).isEqualTo(List.of(item));
    }

    @Test
    @DisplayName("list: テナント未存在の場合、テナント一覧へリダイレクト")
    void list_redirectsToTenantList_whenTenantNotFound() {
        mockTenantNotExists(TENANT_ID);

        String view = controller.list(TENANT_ID, model, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants");
        assertThat(redirectAttrs.getFlashAttributes().get("flashErrorKey"))
                .isEqualTo("system.tenants.error.notFound");
        verify(tenantOidcProviderService, never()).listAllByTenantId(any());
    }

    @Test
    @DisplayName("list: プロバイダー0件でも正常にテンプレートを返す")
    void list_returnsListTemplate_whenNoProviders() {
        mockTenantExists(TENANT_ID);
        when(tenantOidcProviderService.listAllByTenantId(TENANT_ID))
                .thenReturn(List.of());

        String view = controller.list(TENANT_ID, model, redirectAttrs);

        assertThat(view).isEqualTo("system/tenants/oidc-providers/list");
        assertThat(model.getAttribute("items")).isEqualTo(List.of());
    }

    @Test
    @DisplayName("newForm: テナント存在時、newテンプレートと空のregistrationFormを返す")
    void newForm_returnsTemplate_whenTenantExists() {
        mockTenantExists(TENANT_ID);

        String view = controller.newForm(TENANT_ID, model, redirectAttrs);

        assertThat(view).isEqualTo("system/tenants/oidc-providers/new");
        assertThat(model.getAttribute("activeMenu")).isEqualTo("tenants");
        assertThat(model.getAttribute("registrationForm"))
                .isInstanceOf(TenantOidcProviderRegistrationForm.class);
    }

    @Test
    @DisplayName("newForm: テナント未存在の場合、テナント一覧へリダイレクト")
    void newForm_redirectsToTenantList_whenTenantNotFound() {
        mockTenantNotExists(TENANT_ID);

        String view = controller.newForm(TENANT_ID, model, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants");
    }

    @Test
    @DisplayName("register: 正常系、Service呼び出し後に詳細画面へリダイレクト")
    void register_redirectsToDetail_whenSuccess() {
        TenantOidcProviderRegistrationForm form = new TenantOidcProviderRegistrationForm();
        form.setProviderId(PROVIDER_ID);
        form.setDisplayName("Google Workspace");
        form.setIss("https://iss");
        form.setAud("aud");
        form.setClientId("cid");
        form.setClientSecret("secret");
        form.setReason("Setup");
        BindingResult br = bindingResultOf(form, "registrationForm");

        String view = controller.register(
                TENANT_ID, form, br, principal, Locale.JAPAN, redirectAttrs);

        assertThat(view).isEqualTo(
                "redirect:/system/tenants/" + TENANT_ID + "/oidc-providers/" + PROVIDER_ID);
        assertThat(redirectAttrs.getFlashAttributes().get("flashSuccessKey"))
                .isEqualTo("system.tenants.oidc.register.success");
        verify(tenantOidcProviderService).registerProvider(
                TENANT_ID, PROVIDER_ID, "Google Workspace",
                "https://iss", "aud", "cid", "secret", "Setup", "operator-1");
    }

    @Test
    @DisplayName("register: BindingError時、newフォームへリダイレクト、Service呼び出しなし")
    void register_redirectsToNew_whenBindingError() {
        TenantOidcProviderRegistrationForm form = new TenantOidcProviderRegistrationForm();
        BindingResult br = bindingResultWithError(form, "registrationForm");

        String view = controller.register(
                TENANT_ID, form, br, principal, Locale.JAPAN, redirectAttrs);

        assertThat(view).isEqualTo(
                "redirect:/system/tenants/" + TENANT_ID + "/oidc-providers/new");
        verify(tenantOidcProviderService, never()).registerProvider(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("register: RegistrationException発生時、エラーメッセージと共にnewフォームへリダイレクト")
    void register_redirectsToNew_whenServiceThrows() {
        TenantOidcProviderRegistrationForm form = new TenantOidcProviderRegistrationForm();
        form.setProviderId(PROVIDER_ID);
        form.setDisplayName("name");
        form.setIss("https://iss");
        form.setAud("aud");
        form.setClientId("cid");
        form.setClientSecret("secret");
        form.setReason("reason");
        BindingResult br = bindingResultOf(form, "registrationForm");
        doThrow(new TenantOidcProviderRegistrationException(
                TenantOidcProviderRegistrationError.PROVIDER_ID_DUPLICATE))
                .when(tenantOidcProviderService).registerProvider(
                        any(), any(), any(), any(), any(), any(), any(), any(), any());

        String view = controller.register(
                TENANT_ID, form, br, principal, Locale.JAPAN, redirectAttrs);

        assertThat(view).isEqualTo(
                "redirect:/system/tenants/" + TENANT_ID + "/oidc-providers/new");
        assertThat(redirectAttrs.getFlashAttributes()).containsKey("flashErrorMessage");
    }

    @Test
    @DisplayName("register: principalがnullの場合、operatorId='system'でService呼び出し")
    void register_usesSystemOperator_whenPrincipalNull() {
        TenantOidcProviderRegistrationForm form = new TenantOidcProviderRegistrationForm();
        form.setProviderId(PROVIDER_ID);
        form.setDisplayName("name");
        form.setIss("https://iss");
        form.setAud("aud");
        form.setClientId("cid");
        form.setClientSecret("secret");
        form.setReason("reason");
        BindingResult br = bindingResultOf(form, "registrationForm");

        controller.register(
                TENANT_ID, form, br, null, Locale.JAPAN, redirectAttrs);

        verify(tenantOidcProviderService).registerProvider(
                any(), any(), any(), any(), any(), any(), any(), any(),
                org.mockito.ArgumentMatchers.eq("system"));
    }

    @Test
    @DisplayName("detail: テナント+プロバイダー存在、ENABLEDかつ他にENABLEDなしの場合、isLastEnabled=true")
    void detail_isLastEnabledTrue_whenSoleEnabled() {
        mockTenantExists(TENANT_ID);
        TenantOidcProviderDetailView detail = providerDetailOf(
                TENANT_ID, PROVIDER_ID, TenantOidcProviderStatusValue.ENABLED);
        when(tenantOidcProviderService.findDetail(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.of(detail));
        // 自分のみがENABLED
        when(tenantOidcProviderService.findAllEnabledByTenantId(TENANT_ID))
                .thenReturn(List.of(enabledViewOf(PROVIDER_ID)));

        String view = controller.detail(TENANT_ID, PROVIDER_ID, model, redirectAttrs);

        assertThat(view).isEqualTo("system/tenants/oidc-providers/detail");
        assertThat(model.getAttribute("isLastEnabled")).isEqualTo(true);
        assertThat(model.getAttribute("provider")).isEqualTo(detail);
        assertThat(model.getAttribute("statusChangeForm"))
                .isInstanceOf(TenantOidcProviderStatusChangeForm.class);
    }

    @Test
    @DisplayName("detail: ENABLEDだが他にENABLEDがある場合、isLastEnabled=false")
    void detail_isLastEnabledFalse_whenOthersEnabled() {
        mockTenantExists(TENANT_ID);
        TenantOidcProviderDetailView detail = providerDetailOf(
                TENANT_ID, PROVIDER_ID, TenantOidcProviderStatusValue.ENABLED);
        when(tenantOidcProviderService.findDetail(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.of(detail));
        // 自分以外にもENABLEDあり
        when(tenantOidcProviderService.findAllEnabledByTenantId(TENANT_ID))
                .thenReturn(List.of(enabledViewOf(PROVIDER_ID), enabledViewOf("other")));

        controller.detail(TENANT_ID, PROVIDER_ID, model, redirectAttrs);

        assertThat(model.getAttribute("isLastEnabled")).isEqualTo(false);
    }

    @Test
    @DisplayName("detail: DISABLEDのプロバイダーの場合、isLastEnabled=false（計算スキップ）")
    void detail_isLastEnabledFalse_whenCurrentStatusDisabled() {
        mockTenantExists(TENANT_ID);
        TenantOidcProviderDetailView detail = providerDetailOf(
                TENANT_ID, PROVIDER_ID, TenantOidcProviderStatusValue.DISABLED);
        when(tenantOidcProviderService.findDetail(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.of(detail));

        controller.detail(TENANT_ID, PROVIDER_ID, model, redirectAttrs);

        assertThat(model.getAttribute("isLastEnabled")).isEqualTo(false);
        // DISABLEDの場合は findAllEnabledByTenantId が呼ばれないこと
        verify(tenantOidcProviderService, never()).findAllEnabledByTenantId(any());
    }

    @Test
    @DisplayName("detail: テナント未存在の場合、テナント一覧へリダイレクト")
    void detail_redirectsToTenantList_whenTenantNotFound() {
        mockTenantNotExists(TENANT_ID);

        String view = controller.detail(TENANT_ID, PROVIDER_ID, model, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants");
    }

    @Test
    @DisplayName("detail: プロバイダー未存在の場合、OIDC一覧へリダイレクト")
    void detail_redirectsToProviderList_whenProviderNotFound() {
        mockTenantExists(TENANT_ID);
        when(tenantOidcProviderService.findDetail(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.empty());

        String view = controller.detail(TENANT_ID, PROVIDER_ID, model, redirectAttrs);

        assertThat(view).isEqualTo(
                "redirect:/system/tenants/" + TENANT_ID + "/oidc-providers");
        assertThat(redirectAttrs.getFlashAttributes().get("flashErrorKey"))
                .isEqualTo("system.tenants.oidc.error.notFound");
    }

    @Test
    @DisplayName("editForm: テナント+プロバイダー存在、editテンプレートと既存値が入ったeditFormを返す")
    void editForm_returnsTemplate_whenExists() {
        mockTenantExists(TENANT_ID);
        TenantOidcProviderDetailView detail = providerDetailOf(
                TENANT_ID, PROVIDER_ID, TenantOidcProviderStatusValue.ENABLED);
        when(tenantOidcProviderService.findDetail(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.of(detail));

        String view = controller.editForm(TENANT_ID, PROVIDER_ID, model, redirectAttrs);

        assertThat(view).isEqualTo("system/tenants/oidc-providers/edit");
        TenantOidcProviderEditForm form =
                (TenantOidcProviderEditForm) model.getAttribute("editForm");
        assertThat(form).isNotNull();
        assertThat(form.getDisplayName()).isEqualTo("Display " + PROVIDER_ID);
        assertThat(form.getClientId()).isEqualTo("client-" + PROVIDER_ID);
        // clientSecret は表示しない
        assertThat(form.getClientSecret()).isNull();
    }

    @Test
    @DisplayName("editForm: プロバイダー未存在の場合、OIDC一覧へリダイレクト")
    void editForm_redirectsToList_whenProviderNotFound() {
        mockTenantExists(TENANT_ID);
        when(tenantOidcProviderService.findDetail(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.empty());

        String view = controller.editForm(TENANT_ID, PROVIDER_ID, model, redirectAttrs);

        assertThat(view).isEqualTo(
                "redirect:/system/tenants/" + TENANT_ID + "/oidc-providers");
    }

    @Test
    @DisplayName("update: 正常系、Service呼び出し後に詳細画面へリダイレクト")
    void update_redirectsToDetail_whenSuccess() {
        TenantOidcProviderEditForm form = new TenantOidcProviderEditForm();
        form.setDisplayName("Updated Name");
        form.setClientId("new-cid");
        form.setClientSecret("");  // 空 = 現在値維持
        form.setReason("Updating");
        BindingResult br = bindingResultOf(form, "editForm");

        String view = controller.update(
                TENANT_ID, PROVIDER_ID, form, br, principal, Locale.JAPAN, redirectAttrs);

        assertThat(view).isEqualTo(
                "redirect:/system/tenants/" + TENANT_ID + "/oidc-providers/" + PROVIDER_ID);
        verify(tenantOidcProviderService).updateProvider(
                TENANT_ID, PROVIDER_ID, "Updated Name", "new-cid", "",
                "Updating", "operator-1");
    }

    @Test
    @DisplayName("update: BindingError時、編集画面へリダイレクト")
    void update_redirectsToEdit_whenBindingError() {
        TenantOidcProviderEditForm form = new TenantOidcProviderEditForm();
        BindingResult br = bindingResultWithError(form, "editForm");

        String view = controller.update(
                TENANT_ID, PROVIDER_ID, form, br, principal, Locale.JAPAN, redirectAttrs);

        assertThat(view).isEqualTo(
                "redirect:/system/tenants/" + TENANT_ID
                        + "/oidc-providers/" + PROVIDER_ID + "/edit");
        verify(tenantOidcProviderService, never()).updateProvider(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("update: UpdateException発生時、エラーメッセージと共に編集画面へリダイレクト")
    void update_redirectsToEdit_whenServiceThrows() {
        TenantOidcProviderEditForm form = new TenantOidcProviderEditForm();
        form.setDisplayName("name");
        form.setClientId("cid");
        form.setReason("reason");
        BindingResult br = bindingResultOf(form, "editForm");
        doThrow(new TenantOidcProviderUpdateException(
                TenantOidcProviderUpdateError.PROVIDER_NOT_FOUND))
                .when(tenantOidcProviderService).updateProvider(
                        any(), any(), any(), any(), any(), any(), any());

        String view = controller.update(
                TENANT_ID, PROVIDER_ID, form, br, principal, Locale.JAPAN, redirectAttrs);

        assertThat(view).isEqualTo(
                "redirect:/system/tenants/" + TENANT_ID
                        + "/oidc-providers/" + PROVIDER_ID + "/edit");
        assertThat(redirectAttrs.getFlashAttributes()).containsKey("flashErrorMessage");
    }

    @Test
    @DisplayName("changeStatus: 正常系、Service呼び出し後に詳細画面へリダイレクト")
    void changeStatus_redirectsToDetail_whenSuccess() {
        TenantOidcProviderStatusChangeForm form = new TenantOidcProviderStatusChangeForm();
        form.setTargetStatus(TenantOidcProviderStatusValue.DISABLED);
        form.setReason("Disabling");
        BindingResult br = bindingResultOf(form, "statusChangeForm");

        String view = controller.changeStatus(
                TENANT_ID, PROVIDER_ID, form, br, principal, Locale.JAPAN, redirectAttrs);

        assertThat(view).isEqualTo(
                "redirect:/system/tenants/" + TENANT_ID + "/oidc-providers/" + PROVIDER_ID);
        verify(tenantOidcProviderService).changeStatus(
                TENANT_ID, PROVIDER_ID,
                TenantOidcProviderStatusValue.DISABLED, "Disabling", "operator-1");
        assertThat(redirectAttrs.getFlashAttributes().get("flashSuccessKey"))
                .isEqualTo("system.tenants.oidc.statusChange.success");
    }

    @Test
    @DisplayName("changeStatus: BindingError時、modal再表示用openModal属性付きで詳細画面へリダイレクト")
    void changeStatus_redirectsToDetailWithModal_whenBindingError() {
        TenantOidcProviderStatusChangeForm form = new TenantOidcProviderStatusChangeForm();
        BindingResult br = bindingResultWithError(form, "statusChangeForm");

        String view = controller.changeStatus(
                TENANT_ID, PROVIDER_ID, form, br, principal, Locale.JAPAN, redirectAttrs);

        assertThat(view).isEqualTo(
                "redirect:/system/tenants/" + TENANT_ID + "/oidc-providers/" + PROVIDER_ID);
        assertThat(redirectAttrs.getFlashAttributes().get("openModal"))
                .isEqualTo("tenantOidcStatusChange");
        verify(tenantOidcProviderService, never()).changeStatus(
                any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("changeStatus: ALREADY_IN_TARGET_STATUS発生時、エラーメッセージと共に詳細画面へリダイレクト")
    void changeStatus_redirectsToDetailWithError_whenServiceThrows() {
        TenantOidcProviderStatusChangeForm form = new TenantOidcProviderStatusChangeForm();
        form.setTargetStatus(TenantOidcProviderStatusValue.ENABLED);
        form.setReason("Re-enabling");
        BindingResult br = bindingResultOf(form, "statusChangeForm");
        doThrow(new TenantOidcProviderStatusChangeException(
                TenantOidcProviderStatusChangeError.ALREADY_IN_TARGET_STATUS))
                .when(tenantOidcProviderService).changeStatus(
                        any(), any(), any(), any(), any());

        String view = controller.changeStatus(
                TENANT_ID, PROVIDER_ID, form, br, principal, Locale.JAPAN, redirectAttrs);

        assertThat(view).isEqualTo(
                "redirect:/system/tenants/" + TENANT_ID + "/oidc-providers/" + PROVIDER_ID);
        assertThat(redirectAttrs.getFlashAttributes()).containsKey("flashErrorMessage");
    }

    @Test
    @DisplayName("changeStatus: principalがnullの場合、operatorId='system'でService呼び出し")
    void changeStatus_usesSystemOperator_whenPrincipalNull() {
        TenantOidcProviderStatusChangeForm form = new TenantOidcProviderStatusChangeForm();
        form.setTargetStatus(TenantOidcProviderStatusValue.DISABLED);
        form.setReason("reason");
        BindingResult br = bindingResultOf(form, "statusChangeForm");

        controller.changeStatus(
                TENANT_ID, PROVIDER_ID, form, br, null, Locale.JAPAN, redirectAttrs);

        verify(tenantOidcProviderService).changeStatus(
                any(), any(), any(), any(),
                org.mockito.ArgumentMatchers.eq("system"));
    }

    @Test
    @DisplayName("testConnection: 接続成功時、success=trueのレスポンスを返す")
    void testConnection_returnsSuccess_whenVerifyOk() {
        TenantOidcConnectionTestRequest request = new TenantOidcConnectionTestRequest();
        request.setIss("https://iss");

        ResponseEntity<OidcConnectionTestResponse> response =
                controller.testConnection(TENANT_ID, request, Locale.JAPAN);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        verify(oidcProviderService).verify("https://iss");
    }

    @Test
    @DisplayName("testConnection: OidcConnectionException発生時、failureレスポンスを返す")
    void testConnection_returnsFailure_whenVerifyThrows() {
        TenantOidcConnectionTestRequest request = new TenantOidcConnectionTestRequest();
        request.setIss("https://bad-iss");
        doThrow(new OidcConnectionException(OidcConnectionError.CONNECTION_ERROR))
                .when(oidcProviderService).verify("https://bad-iss");

        ResponseEntity<OidcConnectionTestResponse> response =
                controller.testConnection(TENANT_ID, request, Locale.JAPAN);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("CONNECTION_ERROR");
        assertThat(response.getBody().getErrorMessage()).isEqualTo("dummy message");
    }

    @Test
    @DisplayName("testConnectionForEdit: プロバイダー存在 + verify成功時、successレスポンスを返す")
    void testConnectionForEdit_returnsSuccess_whenVerifyOk() {
        TenantOidcProviderDetailView detail = providerDetailOf(
                TENANT_ID, PROVIDER_ID, TenantOidcProviderStatusValue.ENABLED);
        when(tenantOidcProviderService.findDetail(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.of(detail));

        ResponseEntity<OidcConnectionTestResponse> response =
                controller.testConnectionForEdit(TENANT_ID, PROVIDER_ID, Locale.JAPAN);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        verify(oidcProviderService).verify(detail.getIss());
    }

    @Test
    @DisplayName("testConnectionForEdit: プロバイダー未存在時、PROVIDER_NOT_FOUNDのfailureレスポンスを返す")
    void testConnectionForEdit_returnsProviderNotFound_whenMissing() {
        when(tenantOidcProviderService.findDetail(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.empty());

        ResponseEntity<OidcConnectionTestResponse> response =
                controller.testConnectionForEdit(TENANT_ID, PROVIDER_ID, Locale.JAPAN);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("PROVIDER_NOT_FOUND");
        verify(oidcProviderService, never()).verify(any());
    }

    @Test
    @DisplayName("testConnectionForEdit: verify失敗時、エラーコード付きfailureレスポンスを返す")
    void testConnectionForEdit_returnsFailure_whenVerifyThrows() {
        TenantOidcProviderDetailView detail = providerDetailOf(
                TENANT_ID, PROVIDER_ID, TenantOidcProviderStatusValue.ENABLED);
        when(tenantOidcProviderService.findDetail(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.of(detail));
        doThrow(new OidcConnectionException(OidcConnectionError.INVALID_RESPONSE))
                .when(oidcProviderService).verify(any());

        ResponseEntity<OidcConnectionTestResponse> response =
                controller.testConnectionForEdit(TENANT_ID, PROVIDER_ID, Locale.JAPAN);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("INVALID_RESPONSE");
    }

}
