package io.github.kizulog_community.kizulog.infrastructure.web.system.tenantadmininvitation;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.kizulog_community.kizulog.domain.tenant.model.TenantDetailView;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostView;
import io.github.kizulog_community.kizulog.domain.tenant.service.TenantManagementService;
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
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenantadmininvitation.dto.TenantAdminInvitationListItem;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * システム管理側 テナント管理者招待管理コントローラー
 *
 * <p>提供画面:
 * <ul>
 * <li>GET  /system/tenants/{tenantId}/admin-invitations             - 一覧</li>
 * <li>GET  /system/tenants/{tenantId}/admin-invitations/new         - 新規発行フォーム</li>
 * <li>POST /system/tenants/{tenantId}/admin-invitations             - 発行実行</li>
 * <li>GET  /system/tenants/{tenantId}/admin-invitations/{id}        - 詳細</li>
 * <li>POST /system/tenants/{tenantId}/admin-invitations/{id}/cancel - 取消</li>
 * </ul>
 *
 * @author Jun Kobayashi
 */
@Controller
@RequestMapping("/system/tenants/{tenantId}/admin-invitations")
@RequiredArgsConstructor
public class SystemTenantAdminInvitationsController {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(SystemTenantAdminInvitationsController.class);

    /** 受諾URLの相対パスプレフィックス（テナントホスト配下） */
    private static final String INVITE_PATH_PREFIX = "/admin-invite/";

    /** 招待サービス */
    private final TenantAdminInvitationService invitationService;

    /** テナント管理サービス */
    private final TenantManagementService tenantManagementService;

    /** メッセージソース */
    private final MessageSource messageSource;

    /**
     * 招待一覧画面を表示する。
     */
    @GetMapping
    public String list(
            @PathVariable("tenantId") String tId,
            Model model,
            RedirectAttributes redirectAttrs) {

        Optional<TenantDetailView> tenantOpt =
                tenantManagementService.findTenantDetail(tId);
        if (tenantOpt.isEmpty()) {
            redirectAttrs.addFlashAttribute("flashErrorKey",
                    "system.tenants.error.notFound");
            return "redirect:/system/tenants";
        }

        List<TenantInvitationWithStatus> invitations =
                invitationService.listAllInvitations(tId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<TenantAdminInvitationListItem> items = invitations.stream()
                .map(iws -> toListItem(iws, now))
                .toList();

        model.addAttribute("activeMenu", "tenants");
        model.addAttribute("tenant", tenantOpt.get());
        model.addAttribute("items", items);
        return "system/tenants/admin-invitations/list";
    }

    /**
     * 招待新規発行フォームを表示する。
     */
    @GetMapping("/new")
    public String newForm(
            @PathVariable("tenantId") String tId,
            Model model,
            RedirectAttributes redirectAttrs) {

        Optional<TenantDetailView> tenantOpt =
                tenantManagementService.findTenantDetail(tId);
        if (tenantOpt.isEmpty()) {
            redirectAttrs.addFlashAttribute("flashErrorKey",
                    "system.tenants.error.notFound");
            return "redirect:/system/tenants";
        }

        TenantDetailView tenant = tenantOpt.get();
        model.addAttribute("activeMenu", "tenants");
        model.addAttribute("tenant", tenant);
        // ホスト未登録の場合は発行不可。テンプレートで警告表示・ボタン無効化に使う
        model.addAttribute("hasUsableHost", findFirstUsableHost(tenant).isPresent());
        if (!model.containsAttribute("invitationForm")) {
            TenantAdminInvitationForm form = new TenantAdminInvitationForm();
            form.setDurationHours(24);
            model.addAttribute("invitationForm", form);
        }
        return "system/tenants/admin-invitations/new";
    }

    /**
     * 招待を発行する。
     */
    @PostMapping
    public String issue(
            @PathVariable("tenantId") String tId,
            @Valid @ModelAttribute("invitationForm") TenantAdminInvitationForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            RedirectAttributes redirectAttrs,
            Model model) {

        Optional<TenantDetailView> tenantOpt =
                tenantManagementService.findTenantDetail(tId);
        if (tenantOpt.isEmpty()) {
            redirectAttrs.addFlashAttribute("flashErrorKey",
                    "system.tenants.error.notFound");
            return "redirect:/system/tenants";
        }
        TenantDetailView tenant = tenantOpt.get();

        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "tenants");
            model.addAttribute("tenant", tenant);
            model.addAttribute("hasUsableHost",
                    findFirstUsableHost(tenant).isPresent());
            return "system/tenants/admin-invitations/new";
        }

        Optional<TenantHostView> hostOpt = findFirstUsableHost(tenant);
        if (hostOpt.isEmpty()) {
            log.warn("受諾URL生成不可（テナントに利用可能ホストなし）: tenantId={}", tId);
            String errorMessage = messageSource.getMessage(
                    "system.tenants.invitation.form.error.noHost",
                    null,
                    "This tenant has no usable host; cannot generate invite URL",
                    locale);
            redirectAttrs.addFlashAttribute("flashErrorMessage", errorMessage);
            redirectAttrs.addFlashAttribute("invitationForm", form);
            return "redirect:/system/tenants/" + tId + "/admin-invitations/new";
        }

        String createdBy = (principal != null) ? principal.getAccountId() : "system";
        IssuedTenantInvitation issued;
        try {
            issued = invitationService.issueInvitation(
                    tId,
                    form.getDisplayName(),
                    form.getDurationHours(),
                    createdBy);
        } catch (IllegalArgumentException e) {
            log.warn("テナント管理者招待発行のバリデーションに失敗: tenantId={}, msg={}",
                    tId, e.getMessage());
            String errorMessage = messageSource.getMessage(
                    "system.tenants.invitation.form.error.unexpected",
                    null, "Unexpected error", locale);
            redirectAttrs.addFlashAttribute("flashErrorMessage", errorMessage);
            redirectAttrs.addFlashAttribute("invitationForm", form);
            return "redirect:/system/tenants/" + tId + "/admin-invitations/new";
        }

        log.info("テナント管理者招待を発行しました: tenantId={}, invitationId={}, createdBy={}",
                tId, issued.getInvitationId(), createdBy);

        String inviteUrl = buildInviteUrl(hostOpt.get().getHost(), issued.getPlainToken());
        redirectAttrs.addFlashAttribute("issuedInviteUrl", inviteUrl);
        redirectAttrs.addFlashAttribute("issuedExpiresAt", issued.getExpiresAt());
        return "redirect:/system/tenants/" + tId
                + "/admin-invitations/" + issued.getInvitationId();
    }

    /**
     * 招待詳細画面を表示する。
     */
    @GetMapping("/{invitationId}")
    public String detail(
            @PathVariable("tenantId") String tId,
            @PathVariable("invitationId") String inId,
            Model model,
            RedirectAttributes redirectAttrs) {

        Optional<TenantDetailView> tenantOpt =
                tenantManagementService.findTenantDetail(tId);
        if (tenantOpt.isEmpty()) {
            redirectAttrs.addFlashAttribute("flashErrorKey",
                    "system.tenants.error.notFound");
            return "redirect:/system/tenants";
        }

        Optional<TenantInvitationWithStatus> opt =
                invitationService.findInvitationDetail(tId, inId);
        if (opt.isEmpty()) {
            redirectAttrs.addFlashAttribute("flashErrorKey",
                    "system.tenants.invitation.error.notFound");
            return "redirect:/system/tenants/" + tId + "/admin-invitations";
        }

        TenantInvitationWithStatus iws = opt.get();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        TenantAdminInvitationDetailView view = toDetailView(iws, now);

        model.addAttribute("activeMenu", "tenants");
        model.addAttribute("tenant", tenantOpt.get());
        model.addAttribute("invitation", view);

        // 発行直後モード: flashAttribute に issuedInviteUrl があれば設定
        if (model.containsAttribute("issuedInviteUrl")) {
            String url = (String) model.getAttribute("issuedInviteUrl");
            OffsetDateTime expiresAt =
                    (OffsetDateTime) model.getAttribute("issuedExpiresAt");
            model.addAttribute("issuedView",
                    new IssuedTenantInvitationView(url, expiresAt));
        }

        if (!model.containsAttribute("cancelForm")) {
            model.addAttribute("cancelForm", new TenantAdminInvitationCancelForm());
        }
        return "system/tenants/admin-invitations/detail";
    }

    /**
     * 招待を取消する。
     */
    @PostMapping("/{invitationId}/cancel")
    public String cancel(
            @PathVariable("tenantId") String tId,
            @PathVariable("invitationId") String inId,
            @Valid @ModelAttribute("cancelForm") TenantAdminInvitationCancelForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            RedirectAttributes redirectAttrs) {

        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute(
                    "org.springframework.validation.BindingResult.cancelForm",
                    bindingResult);
            redirectAttrs.addFlashAttribute("cancelForm", form);
            redirectAttrs.addFlashAttribute("openModal", "cancel");
            return "redirect:/system/tenants/" + tId
                    + "/admin-invitations/" + inId;
        }

        String cancelledBy = (principal != null) ? principal.getAccountId() : "system";
        try {
            invitationService.cancelInvitation(
                    tId, inId, form.getReason(), cancelledBy);
        } catch (TenantInvitationException e) {
            log.warn("テナント管理者招待取消に失敗: tenantId={}, invitationId={}, error={}",
                    tId, inId, e.getError());
            String errorMessage = messageSource.getMessage(
                    "system.tenants.invitation.form.error." + e.getError().name(),
                    null,
                    "Cancel failed",
                    locale);
            redirectAttrs.addFlashAttribute("flashErrorMessage", errorMessage);
            return "redirect:/system/tenants/" + tId
                    + "/admin-invitations/" + inId;
        }

        log.info("テナント管理者招待を取消しました: tenantId={}, invitationId={}, cancelledBy={}",
                tId, inId, cancelledBy);
        redirectAttrs.addFlashAttribute("flashSuccessKey",
                "system.tenants.invitation.cancel.success");
        return "redirect:/system/tenants/" + tId
                + "/admin-invitations/" + inId;
    }

    /**
     * テナントの利用可能（ACTIVE）なホストのうち最初の1件を返す。
     *
     * @param tenant テナント詳細
     * @return 利用可能ホスト。なければ空。
     */
    private Optional<TenantHostView> findFirstUsableHost(TenantDetailView tenant) {
        if (tenant.getHosts() == null) {
            return Optional.empty();
        }
        return tenant.getHosts().stream()
                .filter(h -> h.getCurrentStatus() != null
                        && h.getCurrentStatus().isUsable())
                .findFirst();
    }

    /**
     * 受諾URLを構築する。
     *
     * @param host テナント識別ホスト
     * @param plainToken 平文トークン
     * @return 受諾URL
     */
    private String buildInviteUrl(String host, String plainToken) {
        return "https://" + host + INVITE_PATH_PREFIX + plainToken;
    }

    /**
     * ListItem DTO 変換。
     */
    private TenantAdminInvitationListItem toListItem(
            TenantInvitationWithStatus iws, OffsetDateTime now) {
        TenantAdminInvitation inv = iws.getInvitation();
        TenantAdminInvitationStatus s = iws.getStatus();
        TenantInvitationStatusValue statusValue =
                (s != null) ? s.getStatus() : TenantInvitationStatusValue.PENDING;
        boolean expired = !inv.getExpiresAt().isAfter(now);
        return new TenantAdminInvitationListItem(
                inv.getInvitationId(),
                inv.getDisplayName(),
                statusValue,
                expired,
                inv.getCreatedAt(),
                inv.getExpiresAt());
    }

    /**
     * DetailView DTO 変換。
     */
    private TenantAdminInvitationDetailView toDetailView(
            TenantInvitationWithStatus iws, OffsetDateTime now) {
        TenantAdminInvitation inv = iws.getInvitation();
        TenantAdminInvitationStatus s = iws.getStatus();
        TenantInvitationStatusValue statusValue =
                (s != null) ? s.getStatus() : TenantInvitationStatusValue.PENDING;
        String reason = (s != null) ? s.getReason() : null;
        OffsetDateTime statusUpdatedAt = (s != null) ? s.getCreatedAt() : null;
        String statusUpdatedBy = (s != null) ? s.getCreatedBy() : null;
        boolean expired = !inv.getExpiresAt().isAfter(now);
        boolean cancellable = (statusValue == TenantInvitationStatusValue.PENDING) && !expired;
        return new TenantAdminInvitationDetailView(
                inv.getInvitationId(),
                inv.getDisplayName(),
                statusValue,
                expired,
                cancellable,
                reason,
                inv.getCreatedAt(),
                inv.getCreatedBy(),
                inv.getExpiresAt(),
                statusUpdatedAt,
                statusUpdatedBy);
    }

}
