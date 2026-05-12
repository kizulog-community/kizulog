package io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityRepository;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.OidcConnectionError;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.OidcConnectionException;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.OidcProviderService;
import io.github.kizulog_community.kizulog.domain.systemoidc.exception.OidcProviderError;
import io.github.kizulog_community.kizulog.domain.systemoidc.exception.OidcProviderException;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.OidcProviderStatusValue;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProvider;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProviderStatus;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.SystemOidcProviderService;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.SystemOidcProviderService.ProviderWithStatus;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto.OidcConnectionTestForEditRequest;
import io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto.OidcConnectionTestRequest;
import io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto.OidcConnectionTestResponse;
import io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto.OidcProviderEditForm;
import io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto.OidcProviderForm;
import io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto.OidcProviderStatusChangeForm;

/**
 * OidcProvidersControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class OidcProvidersControllerTest {

    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    private SystemOidcProviderService systemOidcProviderService;
    private OidcProviderService oidcProviderService;
    private SystemAccountIdentityRepository identityRepository;
    private MessageSource messageSource;
    private OidcProvidersController controller;

    private Model model;
    private RedirectAttributes redirectAttrs;
    private SystemUserPrincipal principal;

    @BeforeEach
    void setUp() {
        systemOidcProviderService = mock(SystemOidcProviderService.class);
        oidcProviderService = mock(OidcProviderService.class);
        identityRepository = mock(SystemAccountIdentityRepository.class);
        messageSource = mock(MessageSource.class);
        controller = new OidcProvidersController(
                systemOidcProviderService,
                oidcProviderService,
                identityRepository,
                messageSource);

        model = new ConcurrentModel();
        redirectAttrs = new RedirectAttributesModelMap();
        principal = mock(SystemUserPrincipal.class);
        when(principal.getAccountId()).thenReturn("admin-account-id");
        when(messageSource.getMessage(any(String.class), any(), any(), any(Locale.class)))
                .thenReturn("dummy message");
    }

    private SystemOidcProvider providerOf(String id) {
        return new SystemOidcProvider(
                id, BASE_TIME, "Display " + id,
                "https://auth.example/realms/" + id,
                "client-" + id, "encrypted-secret",
                BASE_TIME, "user");
    }

    private SystemOidcProviderStatus statusOf(
            String id, OidcProviderStatusValue value) {
        return new SystemOidcProviderStatus(
                id, BASE_TIME, value, null, BASE_TIME, "user");
    }

    private ProviderWithStatus pwsOf(String id, OidcProviderStatusValue value) {
        return new ProviderWithStatus(providerOf(id), statusOf(id, value));
    }

    @Test
    @DisplayName("list: filter=allの時、全プロバイダーをmodelに設定する")
    void list_returnsAllProviders_whenFilterAll() {
        when(systemOidcProviderService.listAll()).thenReturn(List.of(
                pwsOf("master", OidcProviderStatusValue.ENABLED),
                pwsOf("google", OidcProviderStatusValue.DISABLED)));
        when(identityRepository.countActiveByIss(any())).thenReturn(0);

        String view = controller.list("all", model);

        assertThat(view).isEqualTo("system/system-settings/oidc-providers/list");
        assertThat(model.getAttribute("filter")).isEqualTo("all");
        @SuppressWarnings("unchecked")
        List<Object> items = (List<Object>) model.getAttribute("items");
        assertThat(items).hasSize(2);
    }

    @Test
    @DisplayName("list: filter=enabledの時、ENABLEDのみ返す")
    void list_returnsOnlyEnabled_whenFilterEnabled() {
        when(systemOidcProviderService.listAll()).thenReturn(List.of(
                pwsOf("master", OidcProviderStatusValue.ENABLED),
                pwsOf("google", OidcProviderStatusValue.DISABLED)));
        when(identityRepository.countActiveByIss(any())).thenReturn(0);

        controller.list("enabled", model);

        @SuppressWarnings("unchecked")
        List<Object> items = (List<Object>) model.getAttribute("items");
        assertThat(items).hasSize(1);
    }

    @Test
    @DisplayName("list: filter=disabledの時、DISABLEDのみ返す")
    void list_returnsOnlyDisabled_whenFilterDisabled() {
        when(systemOidcProviderService.listAll()).thenReturn(List.of(
                pwsOf("master", OidcProviderStatusValue.ENABLED),
                pwsOf("google", OidcProviderStatusValue.DISABLED)));
        when(identityRepository.countActiveByIss(any())).thenReturn(0);

        controller.list("disabled", model);

        @SuppressWarnings("unchecked")
        List<Object> items = (List<Object>) model.getAttribute("items");
        assertThat(items).hasSize(1);
    }

    @Test
    @DisplayName("list: プロバイダーが空の場合、空のリストをmodelに設定")
    void list_returnsEmptyList_whenNoProviders() {
        when(systemOidcProviderService.listAll()).thenReturn(List.of());

        controller.list("all", model);

        @SuppressWarnings("unchecked")
        List<Object> items = (List<Object>) model.getAttribute("items");
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("detail: プロバイダーが存在する場合、詳細ビューを返す")
    void detail_returnsDetailView_whenExists() {
        when(systemOidcProviderService.findDetailByProviderId("master"))
                .thenReturn(Optional.of(pwsOf("master", OidcProviderStatusValue.ENABLED)));
        when(identityRepository.countActiveByIss(any())).thenReturn(2);

        String view = controller.detail("master", model, redirectAttrs);

        assertThat(view).isEqualTo("system/system-settings/oidc-providers/detail");
        assertThat(model.getAttribute("provider")).isNotNull();
    }

    @Test
    @DisplayName("detail: プロバイダーが存在しない場合、一覧へリダイレクトしフラッシュエラー設定")
    void detail_redirectsToList_whenNotFound() {
        when(systemOidcProviderService.findDetailByProviderId("not-exist"))
                .thenReturn(Optional.empty());

        String view = controller.detail("not-exist", model, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/system-settings/oidc-providers");
        assertThat(redirectAttrs.getFlashAttributes())
                .containsKey("flashErrorKey");
    }

    @Test
    @DisplayName("newForm: 空のフォームをmodelに設定し、newビューを返す")
    void newForm_setsEmptyFormAndReturnsNewView() {
        String view = controller.newForm(model);

        assertThat(view).isEqualTo("system/system-settings/oidc-providers/new");
        assertThat(model.getAttribute("oidcProviderForm"))
                .isInstanceOf(OidcProviderForm.class);
    }

    @Test
    @DisplayName("newForm: model既にform attributeがある場合は上書きしない")
    void newForm_doesNotOverwriteExistingForm() {
        OidcProviderForm preExisting = new OidcProviderForm();
        preExisting.setProviderId("preset");
        model.addAttribute("oidcProviderForm", preExisting);

        controller.newForm(model);

        OidcProviderForm result =
                (OidcProviderForm) model.getAttribute("oidcProviderForm");
        assertThat(result.getProviderId()).isEqualTo("preset");
    }

    @Test
    @DisplayName("create: 正常系では一覧へリダイレクトしフラッシュ成功設定")
    void create_redirectsToListAndSetsFlashSuccess_whenValid() {
        OidcProviderForm form = new OidcProviderForm();
        form.setProviderId("test");
        form.setDisplayName("Test");
        form.setUri("https://example.com");
        form.setClientId("c");
        form.setClientSecret("s");
        BindingResult br = new BeanPropertyBindingResult(form, "oidcProviderForm");

        String view = controller.create(form, br, principal,
                Locale.JAPAN, redirectAttrs, model);

        assertThat(view).isEqualTo("redirect:/system/system-settings/oidc-providers");
        assertThat(redirectAttrs.getFlashAttributes())
                .containsKey("flashSuccessKey")
                .containsKey("flashSuccessParam");
        verify(systemOidcProviderService).registerWithValidation(
                eq("test"), eq("Test"), eq("https://example.com"),
                eq("c"), eq("s"), eq("admin-account-id"));
    }

    @Test
    @DisplayName("create: BindingResultにエラーがある場合、フォームに戻る")
    void create_returnsToForm_whenBindingErrors() {
        OidcProviderForm form = new OidcProviderForm();
        BindingResult br = new BeanPropertyBindingResult(form, "oidcProviderForm");
        br.reject("forced", "forced error");

        String view = controller.create(form, br, principal,
                Locale.JAPAN, redirectAttrs, model);

        assertThat(view).isEqualTo("system/system-settings/oidc-providers/new");
        verify(systemOidcProviderService, never()).registerWithValidation(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("create: OIDC接続確認失敗時、フォームに戻りBindingResultにエラー追加")
    void create_returnsToFormWithError_whenConnectionFails() {
        OidcProviderForm form = new OidcProviderForm();
        form.setProviderId("test");
        form.setUri("https://bad.example.com");
        BindingResult br = new BeanPropertyBindingResult(form, "oidcProviderForm");

        doThrow(new OidcConnectionException(OidcConnectionError.CONNECTION_ERROR))
                .when(oidcProviderService).verify("https://bad.example.com");

        String view = controller.create(form, br, principal,
                Locale.JAPAN, redirectAttrs, model);

        assertThat(view).isEqualTo("system/system-settings/oidc-providers/new");
        assertThat(br.getGlobalErrors()).isNotEmpty();
        verify(systemOidcProviderService, never()).registerWithValidation(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("create: PROVIDER_ID_DUPLICATE例外時、フォームに戻りproviderIdフィールドにエラー")
    void create_returnsToFormWithFieldError_whenProviderIdDuplicate() {
        OidcProviderForm form = new OidcProviderForm();
        form.setProviderId("master");
        form.setUri("https://example.com");
        BindingResult br = new BeanPropertyBindingResult(form, "oidcProviderForm");

        doThrow(new OidcProviderException(OidcProviderError.PROVIDER_ID_DUPLICATE))
                .when(systemOidcProviderService).registerWithValidation(
                        any(), any(), any(), any(), any(), any());

        String view = controller.create(form, br, principal,
                Locale.JAPAN, redirectAttrs, model);

        assertThat(view).isEqualTo("system/system-settings/oidc-providers/new");
        assertThat(br.getFieldErrors("providerId")).isNotEmpty();
    }

    @Test
    @DisplayName("create: PROVIDER_ID_INVALID_FORMAT例外時、フォームに戻りproviderIdフィールドにエラー")
    void create_returnsToFormWithFieldError_whenInvalidProviderIdFormat() {
        OidcProviderForm form = new OidcProviderForm();
        form.setProviderId("INVALID");
        form.setUri("https://example.com");
        BindingResult br = new BeanPropertyBindingResult(form, "oidcProviderForm");

        doThrow(new OidcProviderException(OidcProviderError.PROVIDER_ID_INVALID_FORMAT))
                .when(systemOidcProviderService).registerWithValidation(
                        any(), any(), any(), any(), any(), any());

        String view = controller.create(form, br, principal,
                Locale.JAPAN, redirectAttrs, model);

        assertThat(view).isEqualTo("system/system-settings/oidc-providers/new");
        assertThat(br.getFieldErrors("providerId")).isNotEmpty();
    }

    @Test
    @DisplayName("editForm: プロバイダーが存在する場合、編集ビューを返す")
    void editForm_returnsEditView_whenExists() {
        when(systemOidcProviderService.findDetailByProviderId("master"))
                .thenReturn(Optional.of(pwsOf("master", OidcProviderStatusValue.ENABLED)));

        String view = controller.editForm("master", model, redirectAttrs);

        assertThat(view).isEqualTo("system/system-settings/oidc-providers/edit");
        OidcProviderEditForm form =
                (OidcProviderEditForm) model.getAttribute("oidcProviderEditForm");
        assertThat(form).isNotNull();
        assertThat(form.getProviderId()).isEqualTo("master");
    }

    @Test
    @DisplayName("editForm: プロバイダーが存在しない場合、一覧へリダイレクト")
    void editForm_redirectsToList_whenNotFound() {
        when(systemOidcProviderService.findDetailByProviderId("not-exist"))
                .thenReturn(Optional.empty());

        String view = controller.editForm("not-exist", model, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/system-settings/oidc-providers");
        assertThat(redirectAttrs.getFlashAttributes())
                .containsKey("flashErrorKey");
    }

    @Test
    @DisplayName("update: 正常系では詳細画面へリダイレクトしフラッシュ成功設定")
    void update_redirectsToDetailAndSetsFlashSuccess_whenValid() {
        OidcProviderEditForm form = new OidcProviderEditForm();
        form.setProviderId("master");
        form.setUri("https://auth.example/realms/master");
        form.setDisplayName("New Master");
        form.setClientId("new-client");
        form.setClientSecret("new-secret");
        BindingResult br = new BeanPropertyBindingResult(form, "oidcProviderEditForm");

        when(systemOidcProviderService.findDetailByProviderId("master"))
                .thenReturn(Optional.of(pwsOf("master", OidcProviderStatusValue.ENABLED)));

        String view = controller.update("master", form, br, principal,
                Locale.JAPAN, redirectAttrs, model);

        assertThat(view).isEqualTo("redirect:/system/system-settings/oidc-providers/master");
        verify(systemOidcProviderService).updateMutableFields(
                eq("master"), eq("New Master"), eq("new-client"),
                eq("new-secret"), eq("admin-account-id"));
    }

    @Test
    @DisplayName("update: pathとformのIDが不一致の場合、一覧へリダイレクト")
    void update_redirectsToList_whenIdMismatch() {
        OidcProviderEditForm form = new OidcProviderEditForm();
        form.setProviderId("OTHER");
        BindingResult br = new BeanPropertyBindingResult(form, "oidcProviderEditForm");

        String view = controller.update("master", form, br, principal,
                Locale.JAPAN, redirectAttrs, model);

        assertThat(view).isEqualTo("redirect:/system/system-settings/oidc-providers");
        verify(systemOidcProviderService, never()).updateMutableFields(
                any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("update: BindingResultにエラーがある場合、編集フォームに戻る")
    void update_returnsToEditForm_whenBindingErrors() {
        OidcProviderEditForm form = new OidcProviderEditForm();
        form.setProviderId("master");
        BindingResult br = new BeanPropertyBindingResult(form, "oidcProviderEditForm");
        br.reject("forced", "forced error");

        String view = controller.update("master", form, br, principal,
                Locale.JAPAN, redirectAttrs, model);

        assertThat(view).isEqualTo("system/system-settings/oidc-providers/edit");
        verify(systemOidcProviderService, never()).updateMutableFields(
                any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("update: 編集対象のプロバイダーが消えていた場合、一覧へリダイレクト")
    void update_redirectsToList_whenProviderDisappeared() {
        OidcProviderEditForm form = new OidcProviderEditForm();
        form.setProviderId("master");
        BindingResult br = new BeanPropertyBindingResult(form, "oidcProviderEditForm");

        when(systemOidcProviderService.findDetailByProviderId("master"))
                .thenReturn(Optional.empty());

        String view = controller.update("master", form, br, principal,
                Locale.JAPAN, redirectAttrs, model);

        assertThat(view).isEqualTo("redirect:/system/system-settings/oidc-providers");
        verify(systemOidcProviderService, never()).updateMutableFields(
                any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("update: OIDC接続確認失敗時、編集フォームに戻る")
    void update_returnsToEditForm_whenConnectionFails() {
        OidcProviderEditForm form = new OidcProviderEditForm();
        form.setProviderId("master");
        BindingResult br = new BeanPropertyBindingResult(form, "oidcProviderEditForm");

        when(systemOidcProviderService.findDetailByProviderId("master"))
                .thenReturn(Optional.of(pwsOf("master", OidcProviderStatusValue.ENABLED)));
        doThrow(new OidcConnectionException(OidcConnectionError.CONNECTION_ERROR))
                .when(oidcProviderService).verify(any());

        String view = controller.update("master", form, br, principal,
                Locale.JAPAN, redirectAttrs, model);

        assertThat(view).isEqualTo("system/system-settings/oidc-providers/edit");
        assertThat(br.getGlobalErrors()).isNotEmpty();
        verify(systemOidcProviderService, never()).updateMutableFields(
                any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("update: 更新時にService例外が発生した場合、編集フォームに戻る")
    void update_returnsToEditForm_whenServiceException() {
        OidcProviderEditForm form = new OidcProviderEditForm();
        form.setProviderId("master");
        form.setDisplayName("X");
        form.setClientId("Y");
        BindingResult br = new BeanPropertyBindingResult(form, "oidcProviderEditForm");

        when(systemOidcProviderService.findDetailByProviderId("master"))
                .thenReturn(Optional.of(pwsOf("master", OidcProviderStatusValue.ENABLED)));
        doThrow(new OidcProviderException(OidcProviderError.PROVIDER_NOT_FOUND))
                .when(systemOidcProviderService).updateMutableFields(
                        any(), any(), any(), any(), any());

        String view = controller.update("master", form, br, principal,
                Locale.JAPAN, redirectAttrs, model);

        assertThat(view).isEqualTo("system/system-settings/oidc-providers/edit");
        assertThat(br.getGlobalErrors()).isNotEmpty();
    }

    @Test
    @DisplayName("enable: 正常系では詳細画面へリダイレクトしフラッシュ成功設定")
    void enable_redirectsToDetailAndSetsFlashSuccess_whenValid() {
        OidcProviderStatusChangeForm form = new OidcProviderStatusChangeForm();
        form.setReason("re-activated");
        BindingResult br = new BeanPropertyBindingResult(form, "statusChangeForm");

        String view = controller.enable("master", form, br, principal,
                Locale.JAPAN, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/system-settings/oidc-providers/master");
        verify(systemOidcProviderService).enable(
                eq("master"), eq("re-activated"), eq("admin-account-id"));
        assertThat(redirectAttrs.getFlashAttributes())
                .containsKey("flashSuccessKey");
    }

    @Test
    @DisplayName("enable: BindingResultエラーの場合、詳細画面リダイレクト時にopenModal=enable設定")
    void enable_redirectsWithOpenModal_whenBindingErrors() {
        OidcProviderStatusChangeForm form = new OidcProviderStatusChangeForm();
        BindingResult br = new BeanPropertyBindingResult(form, "statusChangeForm");
        br.reject("forced", "forced error");

        String view = controller.enable("master", form, br, principal,
                Locale.JAPAN, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/system-settings/oidc-providers/master");
        assertThat(redirectAttrs.getFlashAttributes().get("openModal"))
                .isEqualTo("enable");
        verify(systemOidcProviderService, never()).enable(any(), any(), any());
    }

    @Test
    @DisplayName("enable: Service例外発生時、詳細画面リダイレクト時にflashErrorMessage設定")
    void enable_redirectsWithFlashError_whenServiceException() {
        OidcProviderStatusChangeForm form = new OidcProviderStatusChangeForm();
        form.setReason("any");
        BindingResult br = new BeanPropertyBindingResult(form, "statusChangeForm");

        doThrow(new OidcProviderException(OidcProviderError.ALREADY_ENABLED))
                .when(systemOidcProviderService).enable(any(), any(), any());

        String view = controller.enable("master", form, br, principal,
                Locale.JAPAN, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/system-settings/oidc-providers/master");
        assertThat(redirectAttrs.getFlashAttributes())
                .containsKey("flashErrorMessage");
    }

    @Test
    @DisplayName("disable: 正常系では詳細画面へリダイレクトしフラッシュ成功設定")
    void disable_redirectsToDetailAndSetsFlashSuccess_whenValid() {
        OidcProviderStatusChangeForm form = new OidcProviderStatusChangeForm();
        form.setReason("deprecated");
        BindingResult br = new BeanPropertyBindingResult(form, "statusChangeForm");

        String view = controller.disable("master", form, br, principal,
                Locale.JAPAN, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/system-settings/oidc-providers/master");
        verify(systemOidcProviderService).disable(
                eq("master"), eq("deprecated"), eq("admin-account-id"));
        assertThat(redirectAttrs.getFlashAttributes())
                .containsKey("flashSuccessKey");
    }

    @Test
    @DisplayName("disable: BindingResultエラーの場合、詳細画面リダイレクト時にopenModal=disable設定")
    void disable_redirectsWithOpenModal_whenBindingErrors() {
        OidcProviderStatusChangeForm form = new OidcProviderStatusChangeForm();
        BindingResult br = new BeanPropertyBindingResult(form, "statusChangeForm");
        br.reject("forced", "forced error");

        String view = controller.disable("master", form, br, principal,
                Locale.JAPAN, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/system-settings/oidc-providers/master");
        assertThat(redirectAttrs.getFlashAttributes().get("openModal"))
                .isEqualTo("disable");
        verify(systemOidcProviderService, never()).disable(any(), any(), any());
    }

    @Test
    @DisplayName("disable: Service例外発生時、詳細画面リダイレクト時にflashErrorMessage設定")
    void disable_redirectsWithFlashError_whenServiceException() {
        OidcProviderStatusChangeForm form = new OidcProviderStatusChangeForm();
        form.setReason("any");
        BindingResult br = new BeanPropertyBindingResult(form, "statusChangeForm");

        doThrow(new OidcProviderException(OidcProviderError.LAST_ENABLED_REQUIRED))
                .when(systemOidcProviderService).disable(any(), any(), any());

        String view = controller.disable("master", form, br, principal,
                Locale.JAPAN, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/system-settings/oidc-providers/master");
        assertThat(redirectAttrs.getFlashAttributes())
                .containsKey("flashErrorMessage");
    }

    @Test
    @DisplayName("testConnection: 接続成功時、success=trueレスポンスを返す")
    void testConnection_returnsSuccess_whenConnectionOk() {
        OidcConnectionTestRequest request = new OidcConnectionTestRequest();
        request.setUri("https://valid.example.com");

        ResponseEntity<OidcConnectionTestResponse> response =
                controller.testConnection(request, Locale.JAPAN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        verify(oidcProviderService).verify("https://valid.example.com");
    }

    @Test
    @DisplayName("testConnection: 接続失敗時、success=false + errorTypeレスポンスを返す")
    void testConnection_returnsFailure_whenConnectionFails() {
        OidcConnectionTestRequest request = new OidcConnectionTestRequest();
        request.setUri("https://bad.example.com");

        doThrow(new OidcConnectionException(OidcConnectionError.CONNECTION_ERROR))
                .when(oidcProviderService).verify("https://bad.example.com");

        ResponseEntity<OidcConnectionTestResponse> response =
                controller.testConnection(request, Locale.JAPAN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("CONNECTION_ERROR");
    }

    @Test
    @DisplayName("testConnectionForEdit: 接続成功時、DB上のURIで検証してsuccess=trueを返す")
    void testConnectionForEdit_returnsSuccess_whenConnectionOk() {
        OidcConnectionTestForEditRequest request = new OidcConnectionTestForEditRequest();
        request.setProviderId("master");

        when(systemOidcProviderService.findDetailByProviderId("master"))
                .thenReturn(Optional.of(pwsOf("master", OidcProviderStatusValue.ENABLED)));

        ResponseEntity<OidcConnectionTestResponse> response =
                controller.testConnectionForEdit(request, Locale.JAPAN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        verify(oidcProviderService).verify("https://auth.example/realms/master");
    }

    @Test
    @DisplayName("testConnectionForEdit: 接続失敗時、success=false + errorTypeレスポンスを返す")
    void testConnectionForEdit_returnsFailure_whenConnectionFails() {
        OidcConnectionTestForEditRequest request = new OidcConnectionTestForEditRequest();
        request.setProviderId("master");

        when(systemOidcProviderService.findDetailByProviderId("master"))
                .thenReturn(Optional.of(pwsOf("master", OidcProviderStatusValue.ENABLED)));
        doThrow(new OidcConnectionException(OidcConnectionError.INVALID_RESPONSE))
                .when(oidcProviderService).verify(any());

        ResponseEntity<OidcConnectionTestResponse> response =
                controller.testConnectionForEdit(request, Locale.JAPAN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("INVALID_RESPONSE");
    }

    @Test
    @DisplayName("testConnectionForEdit: プロバイダーが存在しない場合、success=false + PROVIDER_NOT_FOUND")
    void testConnectionForEdit_returnsFailure_whenProviderNotFound() {
        OidcConnectionTestForEditRequest request = new OidcConnectionTestForEditRequest();
        request.setProviderId("not-exist");

        when(systemOidcProviderService.findDetailByProviderId("not-exist"))
                .thenReturn(Optional.empty());

        ResponseEntity<OidcConnectionTestResponse> response =
                controller.testConnectionForEdit(request, Locale.JAPAN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("PROVIDER_NOT_FOUND");
        verify(oidcProviderService, never()).verify(any());
    }

}
