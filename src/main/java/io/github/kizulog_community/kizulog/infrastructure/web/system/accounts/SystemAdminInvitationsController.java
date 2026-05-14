package io.github.kizulog_community.kizulog.infrastructure.web.system.accounts;

import java.time.OffsetDateTime;
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

import io.github.kizulog_community.kizulog.domain.systemadmininvitation.exception.InvitationError;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.exception.InvitationException;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.InvitationStatusValue;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.InvitationWithStatus;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.IssuedInvitation;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.SystemAdminInvitation;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.SystemAdminInvitationStatus;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.service.SystemAdminInvitationService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import io.github.kizulog_community.kizulog.infrastructure.web.system.accounts.dto.InvitationCancelForm;
import io.github.kizulog_community.kizulog.infrastructure.web.system.accounts.dto.InvitationDetailView;
import io.github.kizulog_community.kizulog.infrastructure.web.system.accounts.dto.InvitationForm;
import io.github.kizulog_community.kizulog.infrastructure.web.system.accounts.dto.InvitationListItem;
import io.github.kizulog_community.kizulog.infrastructure.web.system.accounts.dto.IssuedInvitationView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * システム管理者招待管理画面のコントローラー
 *
 * <p>SYSTEM_ADMIN が新規システム管理者を招待するための画面群を提供する。
 * URL は /system/accounts/invitations 配下に配置する。</p>
 *
 * <p>提供画面:
 * <ul>
 *   <li>GET  /system/accounts/invitations             - 一覧</li>
 *   <li>GET  /system/accounts/invitations/new         - 新規発行フォーム</li>
 *   <li>POST /system/accounts/invitations             - 発行実行</li>
 *   <li>GET  /system/accounts/invitations/{id}        - 詳細</li>
 *   <li>POST /system/accounts/invitations/{id}/cancel - 取消</li>
 * </ul>
 *
 * @author Jun Kobayashi
 */
@Controller
@RequestMapping("/system/accounts/invitations")
@RequiredArgsConstructor
public class SystemAdminInvitationsController {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(SystemAdminInvitationsController.class);

    /** 招待URLの相対パスプレフィックス */
    private static final String INVITE_PATH_PREFIX = "/system/invite/";

    /** 招待サービス */
    private final SystemAdminInvitationService invitationService;

    /** メッセージソース */
    private final MessageSource messageSource;

    /**
     * 招待一覧画面を表示する。
     *
     * @param model モデル
     * @return 一覧テンプレート
     */
    @GetMapping
    public String list(Model model) {
        List<InvitationWithStatus> invitations = invitationService.listAllInvitations();
        OffsetDateTime now = OffsetDateTime.now(java.time.ZoneOffset.UTC);
        List<InvitationListItem> items = invitations.stream()
                .map(iws -> toListItem(iws, now))
                .toList();

        model.addAttribute("activeMenu", "accounts");
        model.addAttribute("items", items);
        return "system/accounts/invitations/list";
    }

    /**
     * 招待新規発行フォームを表示する。
     *
     * @param model モデル
     * @return 新規発行テンプレート
     */
    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("invitationForm")) {
            InvitationForm form = new InvitationForm();
            // デフォルト24時間
            form.setDurationHours(24);
            model.addAttribute("invitationForm", form);
        }
        model.addAttribute("activeMenu", "accounts");
        return "system/accounts/invitations/new";
    }

    /**
     * 招待を発行する。
     *
     * <p>発行成功時は flashAttribute に平文トークン込みの URL を渡して
     * 詳細画面にリダイレクトする。詳細画面で flash の値を使って
     * 「発行直後の URL 表示モード」を有効化する。</p>
     *
     * @param form フォーム
     * @param bindingResult 検証結果
     * @param principal 認証済みプリンシパル
     * @param request HTTPリクエスト（招待URLのベース構築用）
     * @param redirectAttrs リダイレクト属性
     * @param model モデル
     * @return 発行成功時は詳細画面へリダイレクト、失敗時はフォーム再表示
     */
    @PostMapping
    public String issue(
            @Valid @ModelAttribute("invitationForm") InvitationForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            HttpServletRequest request,
            RedirectAttributes redirectAttrs,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "accounts");
            return "system/accounts/invitations/new";
        }

        String createdBy = (principal != null) ? principal.getAccountId() : "system";
        IssuedInvitation issued;
        try {
            issued = invitationService.issueInvitation(
                    form.getDisplayName(),
                    form.getDurationHours(),
                    createdBy);
        } catch (IllegalArgumentException e) {
            log.warn("招待発行のバリデーションに失敗: {}", e.getMessage());
            bindingResult.reject("invitation.issue.failed",
                    messageSource.getMessage(
                            "system.invitations.form.error.unexpected",
                            null, "Unexpected error", Locale.getDefault()));
            model.addAttribute("activeMenu", "accounts");
            return "system/accounts/invitations/new";
        }

        log.info("招待を発行しました: invitationId={}, createdBy={}",
                issued.getInvitationId(), createdBy);

        String inviteUrl = buildInviteUrl(request, issued.getPlainToken());
        redirectAttrs.addFlashAttribute("issuedInviteUrl", inviteUrl);
        redirectAttrs.addFlashAttribute("issuedExpiresAt", issued.getExpiresAt());
        return "redirect:/system/accounts/invitations/" + issued.getInvitationId();
    }

    /**
     * 招待詳細画面を表示する。
     *
     * <p>flashAttribute に issuedInviteUrl があれば「発行直後モード」として
     * 平文トークン込みURLを表示する。それ以外は通常の詳細表示。</p>
     *
     * @param invitationId 招待ID
     * @param model モデル
     * @param redirectAttrs リダイレクト属性（見つからない場合のリダイレクト用）
     * @return 詳細テンプレート、または一覧へのリダイレクト
     */
    @GetMapping("/{invitationId}")
    public String detail(
            @PathVariable String invitationId,
            Model model,
            RedirectAttributes redirectAttrs) {

        Optional<InvitationWithStatus> opt =
                invitationService.findInvitationDetail(invitationId);
        if (opt.isEmpty()) {
            redirectAttrs.addFlashAttribute("flashErrorKey",
                    "system.invitations.error.notFound");
            return "redirect:/system/accounts/invitations";
        }

        InvitationWithStatus iws = opt.get();
        OffsetDateTime now = OffsetDateTime.now(java.time.ZoneOffset.UTC);
        InvitationDetailView view = toDetailView(iws, now);

        model.addAttribute("activeMenu", "accounts");
        model.addAttribute("invitation", view);

        // 発行直後モード: flashAttribute に issuedInviteUrl があれば設定
        // （リロード時は flash が消えるので通常表示に戻る）
        if (model.containsAttribute("issuedInviteUrl")) {
            String url = (String) model.getAttribute("issuedInviteUrl");
            OffsetDateTime expiresAt = (OffsetDateTime) model.getAttribute("issuedExpiresAt");
            model.addAttribute("issuedView",
                    new IssuedInvitationView(url, expiresAt));
        }

        if (!model.containsAttribute("cancelForm")) {
            model.addAttribute("cancelForm", new InvitationCancelForm());
        }
        return "system/accounts/invitations/detail";
    }

    /**
     * 招待を取消する。
     *
     * @param invitationId 招待ID
     * @param form 取消フォーム
     * @param bindingResult 検証結果
     * @param principal 認証済みプリンシパル
     * @param locale ロケール
     * @param redirectAttrs リダイレクト属性
     * @return 詳細画面へリダイレクト
     */
    @PostMapping("/{invitationId}/cancel")
    public String cancel(
            @PathVariable String invitationId,
            @Valid @ModelAttribute("cancelForm") InvitationCancelForm form,
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
            return "redirect:/system/accounts/invitations/" + invitationId;
        }

        String cancelledBy = (principal != null) ? principal.getAccountId() : "system";
        try {
            invitationService.cancelInvitation(invitationId, form.getReason(), cancelledBy);
        } catch (InvitationException e) {
            log.warn("招待取消に失敗: invitationId={}, error={}",
                    invitationId, e.getError());
            String errorMessage = messageSource.getMessage(
                    "system.invitations.form.error." + e.getError().name(),
                    null,
                    "Cancel failed",
                    locale);
            redirectAttrs.addFlashAttribute("flashErrorMessage", errorMessage);
            return "redirect:/system/accounts/invitations/" + invitationId;
        }

        log.info("招待を取消しました: invitationId={}, cancelledBy={}",
                invitationId, cancelledBy);
        redirectAttrs.addFlashAttribute("flashSuccessKey",
                "system.invitations.cancel.success");
        return "redirect:/system/accounts/invitations/" + invitationId;
    }

    /**
     * 招待URLを構築する。
     *
     * <p>X-Forwarded-* を考慮して、
     * request.getScheme() / getServerName() / getServerPort() から組み立てる。
     * forward-headers-strategy=framework が設定済みのためこれらは nginx 経由でも正しい値を返す。</p>
     */
    private String buildInviteUrl(HttpServletRequest request, String plainToken) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(host);
        boolean defaultPort = (("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443));
        if (!defaultPort) {
            sb.append(":").append(port);
        }
        sb.append(request.getContextPath()).append(INVITE_PATH_PREFIX).append(plainToken);
        return sb.toString();
    }

    /**
     * ListItem DTO 変換。
     */
    private InvitationListItem toListItem(InvitationWithStatus iws, OffsetDateTime now) {
        SystemAdminInvitation inv = iws.getInvitation();
        SystemAdminInvitationStatus s = iws.getStatus();
        InvitationStatusValue statusValue =
                (s != null) ? s.getStatus() : InvitationStatusValue.PENDING;
        boolean expired = !inv.getExpiresAt().isAfter(now);
        return new InvitationListItem(
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
    private InvitationDetailView toDetailView(InvitationWithStatus iws, OffsetDateTime now) {
        SystemAdminInvitation inv = iws.getInvitation();
        SystemAdminInvitationStatus s = iws.getStatus();
        InvitationStatusValue statusValue =
                (s != null) ? s.getStatus() : InvitationStatusValue.PENDING;
        String reason = (s != null) ? s.getReason() : null;
        OffsetDateTime statusUpdatedAt = (s != null) ? s.getCreatedAt() : null;
        String statusUpdatedBy = (s != null) ? s.getCreatedBy() : null;
        boolean expired = !inv.getExpiresAt().isAfter(now);
        boolean cancellable = (statusValue == InvitationStatusValue.PENDING) && !expired;
        return new InvitationDetailView(
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

    /**
     * InvitationError を i18n キーに変換する。
     */
    @SuppressWarnings("unused")
    private static String errorToMessageKey(InvitationError error) {
        return "system.invitations.form.error." + error.name();
    }

}
