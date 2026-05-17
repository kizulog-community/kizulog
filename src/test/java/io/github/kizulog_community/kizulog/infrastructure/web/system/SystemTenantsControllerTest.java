package io.github.kizulog_community.kizulog.infrastructure.web.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantHostError;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantHostException;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantRegistrationError;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantRegistrationException;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantStatusChangeError;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantStatusChangeException;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantUpdateError;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantUpdateException;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantDetailView;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostStatusValue;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantListItemView;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantStatusValue;
import io.github.kizulog_community.kizulog.domain.tenant.service.TenantManagementService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenants.dto.TenantEditForm;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenants.dto.TenantHostAddForm;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenants.dto.TenantHostStatusChangeForm;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenants.dto.TenantRegistrationForm;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenants.dto.TenantStatusChangeForm;
import jakarta.servlet.http.HttpServletRequest;

/**
 * SystemTenantsControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemTenantsControllerTest {

    private static final String OPERATOR_ID = "operator-acc";
    private static final String IDENTITY_ID = "id-op";
    private static final String ISS = "https://auth.example/realms/master";
    private static final String AUD = "kizulog-master";
    private static final String SUB = "op-sub";
    private static final OffsetDateTime T0 =
            OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private TenantManagementService service;
    private MessageSource messageSource;
    private SystemTenantsController sut;

    @BeforeEach
    void setUp() {
        service = mock(TenantManagementService.class);
        messageSource = mock(MessageSource.class);
        sut = new SystemTenantsController(service, messageSource);
    }

    private SystemUserPrincipal operator() {
        OidcIdToken idToken = OidcIdToken.withTokenValue("v")
                .issuer(ISS).subject(SUB).audience(List.of(AUD)).build();
        return SystemUserPrincipal.ofSystemAdmin(
                OPERATOR_ID, IDENTITY_ID, ISS, AUD, SUB, idToken);
    }

    /**
     * HttpServletRequest のモックを生成する。
     * 既定では scheme=https を返す。
     */
    private HttpServletRequest mockRequest() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getScheme()).thenReturn("https");
        return req;
    }

    private TenantListItemView listItem(String tenantId) {
        return new TenantListItemView(
                tenantId, "name", "slug-" + tenantId,
                TenantStatusValue.ACTIVE, T0, List.of(), 0);
    }

    private TenantDetailView detailView(String tenantId) {
        return new TenantDetailView(
                tenantId, "name", "slug",
                T0, "creator",
                TenantStatusValue.ACTIVE, "reason", T0, "creator",
                List.of(), List.of());
    }

    @Test
    @DisplayName("list: 一覧をmodelに設定、activeMenu=tenants、テンプレート名を返す")
    void list_setsAttributes() {
        TenantListItemView a = listItem("t1");
        TenantListItemView b = listItem("t2");
        when(service.listAllTenants()).thenReturn(List.of(a, b));

        Model model = new ConcurrentModel();
        String view = sut.list(model);

        assertThat(view).isEqualTo("system/tenants/list");
        assertThat(model.getAttribute("activeMenu")).isEqualTo("tenants");
        assertThat(model.getAttribute("items")).isEqualTo(List.of(a, b));
    }

    @Test
    @DisplayName("newForm: activeMenu設定、空のregistrationFormをmodelに乗せる")
    void newForm_setsEmptyForm() {
        Model model = new ConcurrentModel();
        String view = sut.newForm(model);

        assertThat(view).isEqualTo("system/tenants/new");
        assertThat(model.getAttribute("activeMenu")).isEqualTo("tenants");
        assertThat(model.getAttribute("registrationForm"))
                .isInstanceOf(TenantRegistrationForm.class);
    }

    @Test
    @DisplayName("register: 正常系→サービス呼び出し+成功フラッシュ+詳細リダイレクト")
    void register_success() {
        TenantRegistrationForm form = new TenantRegistrationForm();
        form.setName("name");
        form.setHosts(List.of("example.com"));
        form.setReason("reason");
        BindingResult br = new BeanPropertyBindingResult(form, "registrationForm");
        when(service.registerTenant(
                eq("name"), eq(List.of("example.com")), eq("reason"), eq(OPERATOR_ID)))
                .thenReturn("new-tenant-id");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.register(form, br, operator(), Locale.JAPANESE, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants/new-tenant-id");
        verify(service).registerTenant(
                "name", List.of("example.com"), "reason", OPERATOR_ID);
        assertThat(redirectAttrs.getFlashAttributes().get("flashSuccessKey"))
                .isEqualTo("system.tenants.register.success");
    }

    @Test
    @DisplayName("register: BindingResultエラー→サービス未呼び出し、newへリダイレクト")
    void register_bindingErrors_redirectsToNew() {
        TenantRegistrationForm form = new TenantRegistrationForm();
        BindingResult br = new BeanPropertyBindingResult(form, "registrationForm");
        br.reject("dummy");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.register(form, br, operator(), Locale.JAPANESE, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants/new");
        verify(service, never()).registerTenant(
                anyString(), anyList(), anyString(), anyString());
        assertThat(redirectAttrs.getFlashAttributes().get("registrationForm")).isSameAs(form);
    }

    @Test
    @DisplayName("register: TenantRegistrationException→エラーメッセージをフラッシュに設定")
    void register_serviceThrows_setsErrorFlash() {
        TenantRegistrationForm form = new TenantRegistrationForm();
        form.setName("name");
        form.setHosts(List.of("example.com"));
        form.setReason("reason");
        BindingResult br = new BeanPropertyBindingResult(form, "registrationForm");

        doThrow(new TenantRegistrationException(TenantRegistrationError.HOST_INVALID))
                .when(service).registerTenant(
                        anyString(), anyList(), anyString(), anyString());
        when(messageSource.getMessage(
                eq("system.tenants.form.error.HOST_INVALID"),
                any(), anyString(), eq(Locale.JAPANESE)))
                .thenReturn("ホスト名が不正です。");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.register(form, br, operator(), Locale.JAPANESE, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants/new");
        assertThat(redirectAttrs.getFlashAttributes().get("flashErrorMessage"))
                .isEqualTo("ホスト名が不正です。");
    }

    @Test
    @DisplayName("detail: 存在するtenantIdは詳細をmodelに乗せて詳細テンプレートを返す")
    void detail_found_renders() {
        TenantDetailView v = detailView("t1");
        when(service.findTenantDetail("t1")).thenReturn(Optional.of(v));

        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.detail("t1", mockRequest(), model, redirectAttrs);

        assertThat(view).isEqualTo("system/tenants/detail");
        assertThat(model.getAttribute("activeMenu")).isEqualTo("tenants");
        assertThat(model.getAttribute("tenant")).isEqualTo(v);
        assertThat(model.getAttribute("statusChangeForm"))
                .isInstanceOf(TenantStatusChangeForm.class);
        assertThat(model.getAttribute("hostAddForm"))
                .isInstanceOf(TenantHostAddForm.class);
        assertThat(model.getAttribute("hostStatusChangeForm"))
                .isInstanceOf(TenantHostStatusChangeForm.class);
    }

    @Test
    @DisplayName("detail: ログインURL生成用のscheme(loginUrlScheme)がmodelにセットされる")
    void detail_setsLoginUrlScheme() {
        TenantDetailView v = detailView("t1");
        when(service.findTenantDetail("t1")).thenReturn(Optional.of(v));

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getScheme()).thenReturn("https");

        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        sut.detail("t1", req, model, redirectAttrs);

        assertThat(model.getAttribute("loginUrlScheme")).isEqualTo("https");
    }

    @Test
    @DisplayName("detail: scheme=httpの場合もそのままmodelにセットされる")
    void detail_setsLoginUrlScheme_http() {
        TenantDetailView v = detailView("t1");
        when(service.findTenantDetail("t1")).thenReturn(Optional.of(v));

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getScheme()).thenReturn("http");

        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        sut.detail("t1", req, model, redirectAttrs);

        assertThat(model.getAttribute("loginUrlScheme")).isEqualTo("http");
    }

    @Test
    @DisplayName("detail: 存在しないtenantIdは一覧へリダイレクトしフラッシュにエラーキー設定")
    void detail_notFound_redirects() {
        when(service.findTenantDetail("missing")).thenReturn(Optional.empty());

        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.detail("missing", mockRequest(), model, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants");
        assertThat(redirectAttrs.getFlashAttributes().get("flashErrorKey"))
                .isEqualTo("system.tenants.error.notFound");
    }

    @Test
    @DisplayName("editForm: 存在するtenantIdは編集フォームに現在のnameをセット")
    void editForm_found_setsCurrentName() {
        TenantDetailView v = detailView("t1");
        when(service.findTenantDetail("t1")).thenReturn(Optional.of(v));

        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.editForm("t1", model, redirectAttrs);

        assertThat(view).isEqualTo("system/tenants/edit");
        assertThat(model.getAttribute("tenant")).isEqualTo(v);
        TenantEditForm editForm = (TenantEditForm) model.getAttribute("editForm");
        assertThat(editForm).isNotNull();
        assertThat(editForm.getName()).isEqualTo("name");
    }

    @Test
    @DisplayName("editForm: 存在しないtenantIdは一覧へリダイレクト")
    void editForm_notFound_redirects() {
        when(service.findTenantDetail("missing")).thenReturn(Optional.empty());

        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.editForm("missing", model, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants");
        assertThat(redirectAttrs.getFlashAttributes().get("flashErrorKey"))
                .isEqualTo("system.tenants.error.notFound");
    }

    @Test
    @DisplayName("update: 正常系→サービス呼び出し+成功フラッシュ+詳細リダイレクト")
    void update_success() {
        TenantEditForm form = new TenantEditForm();
        form.setName("new name");
        form.setReason("reason");
        BindingResult br = new BeanPropertyBindingResult(form, "editForm");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.update(
                "t1", form, br, operator(), Locale.JAPANESE, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants/t1");
        verify(service).updateTenantName("t1", "new name", "reason", OPERATOR_ID);
        assertThat(redirectAttrs.getFlashAttributes().get("flashSuccessKey"))
                .isEqualTo("system.tenants.update.success");
    }

    @Test
    @DisplayName("update: BindingResultエラー→サービス未呼び出し、editへリダイレクト")
    void update_bindingErrors_redirectsToEdit() {
        TenantEditForm form = new TenantEditForm();
        BindingResult br = new BeanPropertyBindingResult(form, "editForm");
        br.reject("dummy");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.update(
                "t1", form, br, operator(), Locale.JAPANESE, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants/t1/edit");
        verify(service, never()).updateTenantName(
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("update: TenantUpdateException→エラーメッセージをフラッシュに設定、editへ")
    void update_serviceThrows_setsErrorFlash() {
        TenantEditForm form = new TenantEditForm();
        form.setName("name");
        form.setReason("reason");
        BindingResult br = new BeanPropertyBindingResult(form, "editForm");

        doThrow(new TenantUpdateException(TenantUpdateError.TENANT_NOT_FOUND))
                .when(service).updateTenantName(
                        anyString(), anyString(), anyString(), anyString());
        when(messageSource.getMessage(
                eq("system.tenants.form.error.TENANT_NOT_FOUND"),
                any(), anyString(), eq(Locale.JAPANESE)))
                .thenReturn("テナントが見つかりません。");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.update(
                "t1", form, br, operator(), Locale.JAPANESE, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants/t1/edit");
        assertThat(redirectAttrs.getFlashAttributes().get("flashErrorMessage"))
                .isEqualTo("テナントが見つかりません。");
    }

    @Test
    @DisplayName("changeStatus: 正常系→サービス呼び出し+成功フラッシュ+詳細リダイレクト")
    void changeStatus_success() {
        TenantStatusChangeForm form = new TenantStatusChangeForm();
        form.setTargetStatus(TenantStatusValue.SUSPENDED);
        form.setReason("suspend");
        BindingResult br = new BeanPropertyBindingResult(form, "statusChangeForm");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.changeStatus(
                "t1", form, br, operator(), Locale.JAPANESE, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants/t1");
        verify(service).changeTenantStatus(
                "t1", TenantStatusValue.SUSPENDED, "suspend", OPERATOR_ID);
        assertThat(redirectAttrs.getFlashAttributes().get("flashSuccessKey"))
                .isEqualTo("system.tenants.statusChange.success");
    }

    @Test
    @DisplayName("changeStatus: BindingResultエラー→openModal=tenantStatusChangeをフラッシュ設定")
    void changeStatus_bindingErrors_setsOpenModal() {
        TenantStatusChangeForm form = new TenantStatusChangeForm();
        BindingResult br = new BeanPropertyBindingResult(form, "statusChangeForm");
        br.reject("dummy");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.changeStatus(
                "t1", form, br, operator(), Locale.JAPANESE, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants/t1");
        verify(service, never()).changeTenantStatus(any(), any(), any(), any());
        assertThat(redirectAttrs.getFlashAttributes().get("openModal"))
                .isEqualTo("tenantStatusChange");
    }

    @Test
    @DisplayName("changeStatus: TenantStatusChangeException→エラーメッセージをフラッシュ設定")
    void changeStatus_serviceThrows_setsErrorFlash() {
        TenantStatusChangeForm form = new TenantStatusChangeForm();
        form.setTargetStatus(TenantStatusValue.ACTIVE);
        form.setReason("reason");
        BindingResult br = new BeanPropertyBindingResult(form, "statusChangeForm");

        doThrow(new TenantStatusChangeException(
                TenantStatusChangeError.ALREADY_IN_TARGET_STATUS))
                .when(service).changeTenantStatus(
                        anyString(), any(TenantStatusValue.class), anyString(), anyString());
        when(messageSource.getMessage(
                eq("system.tenants.form.error.ALREADY_IN_TARGET_STATUS"),
                any(), anyString(), eq(Locale.JAPANESE)))
                .thenReturn("既に同一ステータスです。");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.changeStatus(
                "t1", form, br, operator(), Locale.JAPANESE, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants/t1");
        assertThat(redirectAttrs.getFlashAttributes().get("flashErrorMessage"))
                .isEqualTo("既に同一ステータスです。");
    }

    @Test
    @DisplayName("addHost: 正常系→サービス呼び出し+成功フラッシュ+詳細リダイレクト")
    void addHost_success() {
        TenantHostAddForm form = new TenantHostAddForm();
        form.setHost("new.example.com");
        form.setReason("add reason");
        BindingResult br = new BeanPropertyBindingResult(form, "hostAddForm");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.addHost(
                "t1", form, br, operator(), Locale.JAPANESE, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants/t1");
        verify(service).addHost("t1", "new.example.com", "add reason", OPERATOR_ID);
        assertThat(redirectAttrs.getFlashAttributes().get("flashSuccessKey"))
                .isEqualTo("system.tenants.hostAdd.success");
    }

    @Test
    @DisplayName("addHost: BindingResultエラー→openModal=hostAdd設定")
    void addHost_bindingErrors_setsOpenModal() {
        TenantHostAddForm form = new TenantHostAddForm();
        BindingResult br = new BeanPropertyBindingResult(form, "hostAddForm");
        br.reject("dummy");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.addHost(
                "t1", form, br, operator(), Locale.JAPANESE, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants/t1");
        verify(service, never()).addHost(any(), any(), any(), any());
        assertThat(redirectAttrs.getFlashAttributes().get("openModal"))
                .isEqualTo("hostAdd");
    }

    @Test
    @DisplayName("addHost: TenantHostException→エラーメッセージをフラッシュ設定、openModal=hostAdd")
    void addHost_serviceThrows_setsErrorFlash() {
        TenantHostAddForm form = new TenantHostAddForm();
        form.setHost("dup.example.com");
        form.setReason("reason");
        BindingResult br = new BeanPropertyBindingResult(form, "hostAddForm");

        doThrow(new TenantHostException(TenantHostError.HOST_DUPLICATE))
                .when(service).addHost(anyString(), anyString(), anyString(), anyString());
        when(messageSource.getMessage(
                eq("system.tenants.form.error.HOST_DUPLICATE"),
                any(), anyString(), eq(Locale.JAPANESE)))
                .thenReturn("host重複");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.addHost(
                "t1", form, br, operator(), Locale.JAPANESE, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants/t1");
        assertThat(redirectAttrs.getFlashAttributes().get("flashErrorMessage"))
                .isEqualTo("host重複");
        assertThat(redirectAttrs.getFlashAttributes().get("openModal"))
                .isEqualTo("hostAdd");
    }

    @Test
    @DisplayName("changeHostStatus: targetStatus=INACTIVE→disableHostを呼ぶ")
    void changeHostStatus_disable_callsDisableHost() {
        TenantHostStatusChangeForm form = new TenantHostStatusChangeForm();
        form.setTargetStatus(TenantHostStatusValue.INACTIVE);
        form.setReason("disable");
        BindingResult br = new BeanPropertyBindingResult(form, "hostStatusChangeForm");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.changeHostStatus(
                "t1", "example.com", form, br, operator(),
                Locale.JAPANESE, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants/t1");
        verify(service).disableHost("t1", "example.com", "disable", OPERATOR_ID);
        verify(service, never()).enableHost(any(), any(), any(), any());
        assertThat(redirectAttrs.getFlashAttributes().get("flashSuccessKey"))
                .isEqualTo("system.tenants.hostStatusChange.success");
    }

    @Test
    @DisplayName("changeHostStatus: targetStatus=ACTIVE→enableHostを呼ぶ")
    void changeHostStatus_enable_callsEnableHost() {
        TenantHostStatusChangeForm form = new TenantHostStatusChangeForm();
        form.setTargetStatus(TenantHostStatusValue.ACTIVE);
        form.setReason("enable");
        BindingResult br = new BeanPropertyBindingResult(form, "hostStatusChangeForm");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.changeHostStatus(
                "t1", "example.com", form, br, operator(),
                Locale.JAPANESE, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants/t1");
        verify(service).enableHost("t1", "example.com", "enable", OPERATOR_ID);
        verify(service, never()).disableHost(any(), any(), any(), any());
    }

    @Test
    @DisplayName("changeHostStatus: TenantHostException→エラーメッセージをフラッシュ設定")
    void changeHostStatus_serviceThrows_setsErrorFlash() {
        TenantHostStatusChangeForm form = new TenantHostStatusChangeForm();
        form.setTargetStatus(TenantHostStatusValue.INACTIVE);
        form.setReason("reason");
        BindingResult br = new BeanPropertyBindingResult(form, "hostStatusChangeForm");

        doThrow(new TenantHostException(TenantHostError.HOST_NOT_FOUND))
                .when(service).disableHost(
                        anyString(), anyString(), anyString(), anyString());
        when(messageSource.getMessage(
                eq("system.tenants.form.error.HOST_NOT_FOUND"),
                any(), anyString(), eq(Locale.JAPANESE)))
                .thenReturn("hostが見つかりません。");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.changeHostStatus(
                "t1", "example.com", form, br, operator(),
                Locale.JAPANESE, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants/t1");
        assertThat(redirectAttrs.getFlashAttributes().get("flashErrorMessage"))
                .isEqualTo("hostが見つかりません。");
    }

    @Test
    @DisplayName("register: principalがnullでもoperatorId='system'で呼ばれる")
    void register_principalNull_usesSystemOperator() {
        TenantRegistrationForm form = new TenantRegistrationForm();
        form.setName("name");
        form.setHosts(List.of("example.com"));
        form.setReason("reason");
        BindingResult br = new BeanPropertyBindingResult(form, "registrationForm");
        when(service.registerTenant(anyString(), anyList(), anyString(), eq("system")))
                .thenReturn("t1");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        sut.register(form, br, null, Locale.JAPANESE, redirectAttrs);

        verify(service).registerTenant(
                "name", List.of("example.com"), "reason", "system");
    }

}
