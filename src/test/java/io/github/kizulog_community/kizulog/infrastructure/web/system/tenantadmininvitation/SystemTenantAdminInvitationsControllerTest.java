package io.github.kizulog_community.kizulog.infrastructure.web.system.tenantadmininvitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import io.github.kizulog_community.kizulog.domain.tenant.model.TenantDetailView;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostStatusValue;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostView;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantStatusValue;
import io.github.kizulog_community.kizulog.domain.tenant.service.TenantManagementService;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.exception.TenantInvitationError;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.exception.TenantInvitationException;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.IssuedTenantInvitation;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantAdminInvitation;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantAdminInvitationStatus;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantInvitationStatusValue;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantInvitationWithStatus;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.service.TenantAdminInvitationService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenantadmininvitation.dto.IssuedTenantInvitationView;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenantadmininvitation.dto.TenantAdminInvitationCancelForm;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenantadmininvitation.dto.TenantAdminInvitationDetailView;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenantadmininvitation.dto.TenantAdminInvitationForm;

/**
 * SystemTenantAdminInvitationsController の単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemTenantAdminInvitationsControllerTest {

    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    private static final String TENANT_ID = "tenant-A";
    private static final String HOST = "acme.example.com";
    private static final String INVITATION_ID = "inv-001";

    private TenantAdminInvitationService invitationService;
    private TenantManagementService tenantManagementService;
    private MessageSource messageSource;
    private SystemTenantAdminInvitationsController controller;

    private Model model;
    private RedirectAttributes redirectAttrs;
    private SystemUserPrincipal principal;

    @BeforeEach
    void setUp() {
        invitationService = mock(TenantAdminInvitationService.class);
        tenantManagementService = mock(TenantManagementService.class);
        messageSource = mock(MessageSource.class);
        controller = new SystemTenantAdminInvitationsController(
                invitationService, tenantManagementService, messageSource);

        model = new ConcurrentModel();
        redirectAttrs = new RedirectAttributesModelMap();
        principal = mock(SystemUserPrincipal.class);
        when(principal.getAccountId()).thenReturn("operator-1");
        when(messageSource.getMessage(any(String.class), any(), any(), any(Locale.class)))
                .thenReturn("dummy message");
    }

    /** ACTIVE ホスト1件を持つテナント */
    private TenantDetailView tenantWithHost() {
        TenantHostView host = new TenantHostView(
                HOST, BASE_TIME, "creator",
                TenantHostStatusValue.ACTIVE, null, BASE_TIME, "creator",
                List.of());
        return new TenantDetailView(
                TENANT_ID, "Tenant A", "slug-a", BASE_TIME, "creator",
                TenantStatusValue.ACTIVE, null, BASE_TIME, "creator",
                List.of(), List.of(host));
    }

    /** ホストを1件も持たないテナント */
    private TenantDetailView tenantWithoutHost() {
        return new TenantDetailView(
                TENANT_ID, "Tenant A", "slug-a", BASE_TIME, "creator",
                TenantStatusValue.ACTIVE, null, BASE_TIME, "creator",
                List.of(), List.of());
    }

    /** INACTIVE ホストのみ（usable なし）のテナント */
    private TenantDetailView tenantWithInactiveHostOnly() {
        TenantHostView host = new TenantHostView(
                HOST, BASE_TIME, "creator",
                TenantHostStatusValue.INACTIVE, null, BASE_TIME, "creator",
                List.of());
        return new TenantDetailView(
                TENANT_ID, "Tenant A", "slug-a", BASE_TIME, "creator",
                TenantStatusValue.ACTIVE, null, BASE_TIME, "creator",
                List.of(), List.of(host));
    }

    private TenantInvitationWithStatus invitationWithStatus(
            String invitationId, TenantInvitationStatusValue status,
            OffsetDateTime expiresAt) {
        TenantAdminInvitation inv = new TenantAdminInvitation(
                invitationId, BASE_TIME, TENANT_ID, "hash", expiresAt,
                "display-" + invitationId, BASE_TIME, "creator");
        TenantAdminInvitationStatus st = new TenantAdminInvitationStatus(
                invitationId, BASE_TIME, status, null, BASE_TIME, "creator");
        return new TenantInvitationWithStatus(inv, st);
    }

    @Test
    @DisplayName("list: テナントが存在する場合、一覧テンプレートを返しitemsをmodelに設定する")
    void list_returnsListView_whenTenantExists() {
        when(tenantManagementService.findTenantDetail(TENANT_ID))
                .thenReturn(Optional.of(tenantWithHost()));
        when(invitationService.listAllInvitations(TENANT_ID))
                .thenReturn(List.of(invitationWithStatus(
                        INVITATION_ID, TenantInvitationStatusValue.PENDING,
                        OffsetDateTime.now(ZoneOffset.UTC).plusDays(1))));

        String view = controller.list(TENANT_ID, model, redirectAttrs);

        assertThat(view).isEqualTo("system/tenants/admin-invitations/list");
        assertThat(model.getAttribute("items")).isInstanceOf(List.class);
        assertThat((List<?>) model.getAttribute("items")).hasSize(1);
        assertThat(model.getAttribute("tenant")).isNotNull();
    }

    @Test
    @DisplayName("list: テナントが存在しない場合、/system/tenants へリダイレクトする")
    void list_redirects_whenTenantNotFound() {
        when(tenantManagementService.findTenantDetail(TENANT_ID))
                .thenReturn(Optional.empty());

        String view = controller.list(TENANT_ID, model, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants");
        assertThat(redirectAttrs.getFlashAttributes().get("flashErrorKey"))
                .isEqualTo("system.tenants.error.notFound");
        verify(invitationService, never()).listAllInvitations(anyString());
    }

    @Test
    @DisplayName("newForm: ホストありの場合、hasUsableHost=trueでフォームを表示する")
    void newForm_setsHasUsableHostTrue_whenHostExists() {
        when(tenantManagementService.findTenantDetail(TENANT_ID))
                .thenReturn(Optional.of(tenantWithHost()));

        String view = controller.newForm(TENANT_ID, model, redirectAttrs);

        assertThat(view).isEqualTo("system/tenants/admin-invitations/new");
        assertThat(model.getAttribute("hasUsableHost")).isEqualTo(true);
        assertThat(model.getAttribute("invitationForm")).isNotNull();
    }

    @Test
    @DisplayName("newForm: ホストなしの場合、hasUsableHost=false")
    void newForm_setsHasUsableHostFalse_whenNoHost() {
        when(tenantManagementService.findTenantDetail(TENANT_ID))
                .thenReturn(Optional.of(tenantWithoutHost()));

        controller.newForm(TENANT_ID, model, redirectAttrs);

        assertThat(model.getAttribute("hasUsableHost")).isEqualTo(false);
    }

    @Test
    @DisplayName("newForm: INACTIVEホストのみの場合、hasUsableHost=false")
    void newForm_setsHasUsableHostFalse_whenOnlyInactiveHost() {
        when(tenantManagementService.findTenantDetail(TENANT_ID))
                .thenReturn(Optional.of(tenantWithInactiveHostOnly()));

        controller.newForm(TENANT_ID, model, redirectAttrs);

        assertThat(model.getAttribute("hasUsableHost")).isEqualTo(false);
    }

    @Test
    @DisplayName("newForm: テナントが存在しない場合、リダイレクト")
    void newForm_redirects_whenTenantNotFound() {
        when(tenantManagementService.findTenantDetail(TENANT_ID))
                .thenReturn(Optional.empty());

        String view = controller.newForm(TENANT_ID, model, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants");
    }

    @Test
    @DisplayName("issue: 正常系 - 招待発行し詳細画面へリダイレクト、受諾URL(テナントホスト)をflashに設定")
    void issue_issuesAndRedirectsToDetail_withInviteUrl() {
        when(tenantManagementService.findTenantDetail(TENANT_ID))
                .thenReturn(Optional.of(tenantWithHost()));
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        when(invitationService.issueInvitation(eq(TENANT_ID), anyString(), anyInt(), anyString()))
                .thenReturn(new IssuedTenantInvitation(
                        INVITATION_ID, "plain-token-xyz", expiresAt));

        TenantAdminInvitationForm form = new TenantAdminInvitationForm();
        form.setDisplayName("New Admin");
        form.setDurationHours(24);
        BindingResult br = new BeanPropertyBindingResult(form, "invitationForm");

        String view = controller.issue(
                TENANT_ID, form, br, principal, Locale.JAPAN, redirectAttrs, model);

        assertThat(view).isEqualTo(
                "redirect:/system/tenants/" + TENANT_ID + "/admin-invitations/" + INVITATION_ID);
        // 受諾URLはテナントホストから組み立てられる（案H1）
        assertThat(redirectAttrs.getFlashAttributes().get("issuedInviteUrl"))
                .isEqualTo("https://" + HOST + "/admin-invite/plain-token-xyz");
        assertThat(redirectAttrs.getFlashAttributes().get("issuedExpiresAt"))
                .isEqualTo(expiresAt);
        verify(invitationService).issueInvitation(TENANT_ID, "New Admin", 24, "operator-1");
    }

    @Test
    @DisplayName("issue: バリデーションエラーの場合、フォームを再表示する")
    void issue_returnsForm_whenValidationError() {
        when(tenantManagementService.findTenantDetail(TENANT_ID))
                .thenReturn(Optional.of(tenantWithHost()));

        TenantAdminInvitationForm form = new TenantAdminInvitationForm();
        BindingResult br = new BeanPropertyBindingResult(form, "invitationForm");
        br.reject("error");

        String view = controller.issue(
                TENANT_ID, form, br, principal, Locale.JAPAN, redirectAttrs, model);

        assertThat(view).isEqualTo("system/tenants/admin-invitations/new");
        verify(invitationService, never())
                .issueInvitation(anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("issue: ホスト未登録の場合、発行せずフォームへリダイレクト（案H1）")
    void issue_redirectsWithError_whenNoUsableHost() {
        when(tenantManagementService.findTenantDetail(TENANT_ID))
                .thenReturn(Optional.of(tenantWithoutHost()));

        TenantAdminInvitationForm form = new TenantAdminInvitationForm();
        form.setDisplayName("New Admin");
        form.setDurationHours(24);
        BindingResult br = new BeanPropertyBindingResult(form, "invitationForm");

        String view = controller.issue(
                TENANT_ID, form, br, principal, Locale.JAPAN, redirectAttrs, model);

        assertThat(view).isEqualTo(
                "redirect:/system/tenants/" + TENANT_ID + "/admin-invitations/new");
        assertThat(redirectAttrs.getFlashAttributes().get("flashErrorMessage")).isNotNull();
        verify(invitationService, never())
                .issueInvitation(anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("issue: テナントが存在しない場合、/system/tenants へリダイレクト")
    void issue_redirects_whenTenantNotFound() {
        when(tenantManagementService.findTenantDetail(TENANT_ID))
                .thenReturn(Optional.empty());

        TenantAdminInvitationForm form = new TenantAdminInvitationForm();
        form.setDisplayName("New Admin");
        form.setDurationHours(24);
        BindingResult br = new BeanPropertyBindingResult(form, "invitationForm");

        String view = controller.issue(
                TENANT_ID, form, br, principal, Locale.JAPAN, redirectAttrs, model);

        assertThat(view).isEqualTo("redirect:/system/tenants");
    }

    @Test
    @DisplayName("issue: Serviceがバリデーション例外の場合、エラーメッセージ付きでフォームへリダイレクト")
    void issue_redirectsWithError_whenServiceThrowsIllegalArgument() {
        when(tenantManagementService.findTenantDetail(TENANT_ID))
                .thenReturn(Optional.of(tenantWithHost()));
        when(invitationService.issueInvitation(eq(TENANT_ID), anyString(), anyInt(), anyString()))
                .thenThrow(new IllegalArgumentException("bad"));

        TenantAdminInvitationForm form = new TenantAdminInvitationForm();
        form.setDisplayName("New Admin");
        form.setDurationHours(24);
        BindingResult br = new BeanPropertyBindingResult(form, "invitationForm");

        String view = controller.issue(
                TENANT_ID, form, br, principal, Locale.JAPAN, redirectAttrs, model);

        assertThat(view).isEqualTo(
                "redirect:/system/tenants/" + TENANT_ID + "/admin-invitations/new");
        assertThat(redirectAttrs.getFlashAttributes().get("flashErrorMessage")).isNotNull();
    }

    @Test
    @DisplayName("detail: 招待が存在する場合、詳細テンプレートを返す")
    void detail_returnsDetailView_whenExists() {
        when(tenantManagementService.findTenantDetail(TENANT_ID))
                .thenReturn(Optional.of(tenantWithHost()));
        when(invitationService.findInvitationDetail(TENANT_ID, INVITATION_ID))
                .thenReturn(Optional.of(invitationWithStatus(
                        INVITATION_ID, TenantInvitationStatusValue.PENDING,
                        OffsetDateTime.now(ZoneOffset.UTC).plusDays(1))));

        String view = controller.detail(TENANT_ID, INVITATION_ID, model, redirectAttrs);

        assertThat(view).isEqualTo("system/tenants/admin-invitations/detail");
        assertThat(model.getAttribute("invitation"))
                .isInstanceOf(TenantAdminInvitationDetailView.class);
        assertThat(model.getAttribute("cancelForm"))
                .isInstanceOf(TenantAdminInvitationCancelForm.class);
    }

    @Test
    @DisplayName("detail: 発行直後モード - flashにissuedInviteUrlがあればissuedViewを設定する")
    void detail_setsIssuedView_whenFlashHasInviteUrl() {
        when(tenantManagementService.findTenantDetail(TENANT_ID))
                .thenReturn(Optional.of(tenantWithHost()));
        when(invitationService.findInvitationDetail(TENANT_ID, INVITATION_ID))
                .thenReturn(Optional.of(invitationWithStatus(
                        INVITATION_ID, TenantInvitationStatusValue.PENDING,
                        OffsetDateTime.now(ZoneOffset.UTC).plusDays(1))));
        // flashAttribute 相当をmodelに事前設定
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        model.addAttribute("issuedInviteUrl", "https://" + HOST + "/admin-invite/tok");
        model.addAttribute("issuedExpiresAt", expiresAt);

        controller.detail(TENANT_ID, INVITATION_ID, model, redirectAttrs);

        Object issuedView = model.getAttribute("issuedView");
        assertThat(issuedView).isInstanceOf(IssuedTenantInvitationView.class);
        assertThat(((IssuedTenantInvitationView) issuedView).getInviteUrl())
                .isEqualTo("https://" + HOST + "/admin-invite/tok");
    }

    @Test
    @DisplayName("detail: 招待が存在しない場合、一覧へリダイレクト")
    void detail_redirects_whenInvitationNotFound() {
        when(tenantManagementService.findTenantDetail(TENANT_ID))
                .thenReturn(Optional.of(tenantWithHost()));
        when(invitationService.findInvitationDetail(TENANT_ID, INVITATION_ID))
                .thenReturn(Optional.empty());

        String view = controller.detail(TENANT_ID, INVITATION_ID, model, redirectAttrs);

        assertThat(view).isEqualTo(
                "redirect:/system/tenants/" + TENANT_ID + "/admin-invitations");
        assertThat(redirectAttrs.getFlashAttributes().get("flashErrorKey"))
                .isEqualTo("system.tenants.invitation.error.notFound");
    }

    @Test
    @DisplayName("detail: テナントが存在しない場合、/system/tenants へリダイレクト")
    void detail_redirects_whenTenantNotFound() {
        when(tenantManagementService.findTenantDetail(TENANT_ID))
                .thenReturn(Optional.empty());

        String view = controller.detail(TENANT_ID, INVITATION_ID, model, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/tenants");
    }

    @Test
    @DisplayName("cancel: 正常系 - 取消し詳細画面へリダイレクト、成功メッセージをflashに設定")
    void cancel_cancelsAndRedirectsToDetail() {
        TenantAdminInvitationCancelForm form = new TenantAdminInvitationCancelForm();
        form.setReason("誤発行");
        BindingResult br = new BeanPropertyBindingResult(form, "cancelForm");

        String view = controller.cancel(
                TENANT_ID, INVITATION_ID, form, br, principal, Locale.JAPAN, redirectAttrs);

        assertThat(view).isEqualTo(
                "redirect:/system/tenants/" + TENANT_ID + "/admin-invitations/" + INVITATION_ID);
        assertThat(redirectAttrs.getFlashAttributes().get("flashSuccessKey"))
                .isEqualTo("system.tenants.invitation.cancel.success");
        verify(invitationService).cancelInvitation(TENANT_ID, INVITATION_ID, "誤発行", "operator-1");
    }

    @Test
    @DisplayName("cancel: バリデーションエラーの場合、モーダル再オープン付きで詳細へリダイレクト")
    void cancel_redirectsWithModal_whenValidationError() {
        TenantAdminInvitationCancelForm form = new TenantAdminInvitationCancelForm();
        BindingResult br = new BeanPropertyBindingResult(form, "cancelForm");
        br.reject("error");

        String view = controller.cancel(
                TENANT_ID, INVITATION_ID, form, br, principal, Locale.JAPAN, redirectAttrs);

        assertThat(view).isEqualTo(
                "redirect:/system/tenants/" + TENANT_ID + "/admin-invitations/" + INVITATION_ID);
        assertThat(redirectAttrs.getFlashAttributes().get("openModal")).isEqualTo("cancel");
        verify(invitationService, never())
                .cancelInvitation(anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("cancel: Service例外の場合、エラーメッセージ付きで詳細へリダイレクト")
    void cancel_redirectsWithError_whenServiceThrows() {
        doThrow(new TenantInvitationException(TenantInvitationError.ALREADY_USED_FOR_CANCEL))
                .when(invitationService)
                .cancelInvitation(eq(TENANT_ID), eq(INVITATION_ID), any(), anyString());

        TenantAdminInvitationCancelForm form = new TenantAdminInvitationCancelForm();
        BindingResult br = new BeanPropertyBindingResult(form, "cancelForm");

        String view = controller.cancel(
                TENANT_ID, INVITATION_ID, form, br, principal, Locale.JAPAN, redirectAttrs);

        assertThat(view).isEqualTo(
                "redirect:/system/tenants/" + TENANT_ID + "/admin-invitations/" + INVITATION_ID);
        assertThat(redirectAttrs.getFlashAttributes().get("flashErrorMessage")).isNotNull();
    }

}
