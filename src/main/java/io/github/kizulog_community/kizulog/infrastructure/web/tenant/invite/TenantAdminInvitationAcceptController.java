package io.github.kizulog_community.kizulog.infrastructure.web.tenant.invite;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.kizulog_community.kizulog.domain.tenant.model.Tenant;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.exception.TenantInvitationError;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.exception.TenantInvitationException;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantAdminInvitation;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.service.TenantAdminInvitationService;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderChoiceView;
import io.github.kizulog_community.kizulog.domain.tenantoidc.service.TenantOidcProviderService;
import io.github.kizulog_community.kizulog.infrastructure.web.tenant.TenantContext;
import lombok.RequiredArgsConstructor;

/**
 * テナント管理者 招待受諾コントローラー
 *
 * <p>テナントホスト上の招待リンク経由のフローを担当する（認証不要でアクセス可）</p>
 *
 * <p>提供画面・URL（すべてテナントホスト配下）:
 * <ul>
 * <li>GET /admin-invite/{token}        - トークン検証 → 受諾確認画面 or エラー画面</li>
 * <li>GET /admin-invite/accept-confirm - 受諾確認画面（プロバイダー選択）</li>
 * <li>GET /admin-invite/error          - エラー画面（エラーコード別メッセージ）</li>
 * </ul>
 *
 * @author Jun Kobayashi
 */
@Controller
@RequestMapping("/admin-invite")
@RequiredArgsConstructor
public class TenantAdminInvitationAcceptController {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(TenantAdminInvitationAcceptController.class);

    /** エラーコード（不明） */
    private static final String ERROR_CODE_UNKNOWN = "UNKNOWN";

    /** 受諾確認テンプレート */
    private static final String VIEW_ACCEPT_CONFIRM = "tenant/invite/accept-confirm";

    /** エラーテンプレート */
    private static final String VIEW_ERROR = "tenant/invite/error";

    /** 受諾確認画面へのリダイレクト */
    private static final String REDIRECT_ACCEPT_CONFIRM = "redirect:/admin-invite/accept-confirm";

    /** エラー画面へのリダイレクトテンプレート */
    private static final String REDIRECT_ERROR = "redirect:/admin-invite/error?code=";

    /** 招待サービス */
    private final TenantAdminInvitationService invitationService;

    /** テナントOIDCプロバイダーサービス */
    private final TenantOidcProviderService tenantOidcProviderService;

    /** 招待受諾セッション */
    private final TenantInvitationAcceptanceSession invitationSession;

    /**
     * 招待リンク受信時のエンドポイント。
     *
     * @param token 平文トークン（URLパスから受け取る）
     * @return リダイレクト先
     */
    @GetMapping("/{token}")
    public String receive(@PathVariable String token) {
        // 前回の途中状態が残っていればクリア
        invitationSession.clear();

        // 受諾アクセス元ホストから解決したテナント（TenantResolverFilter が設定済み）
        Tenant tenant = TenantContext.current();
        if (tenant == null) {
            // 通常 TenantResolverFilter を通過しており null にはならないが、防御的に弾く
            log.warn("受諾リンク受信時にTenantContextが未設定: token受信");
            return REDIRECT_ERROR + TenantInvitationError.INVALID_TOKEN.name();
        }

        TenantAdminInvitation invitation;
        try {
            invitation = invitationService.findValidInvitationByToken(token);
        } catch (TenantInvitationException e) {
            log.info("テナント招待トークン検証失敗: error={}", e.getError());
            return REDIRECT_ERROR + e.getError().name();
        }

        // テナント境界検証: 招待のテナントと受諾アクセス元ホストのテナントが一致するか
        if (!invitation.getTenantId().equals(tenant.getTenantId())) {
            log.warn("テナント不一致の受諾リンクアクセス: invitationTenantId={}, resolvedTenantId={}",
                    invitation.getTenantId(), tenant.getTenantId());
            return REDIRECT_ERROR + TenantInvitationError.TENANT_MISMATCH.name();
        }

        // 検証OK → セッションに保存
        invitationSession.setInvitationId(invitation.getInvitationId());
        invitationSession.setTenantId(tenant.getTenantId());
        invitationSession.setPlainToken(token);
        invitationSession.setDisplayName(invitation.getDisplayName());

        log.info("テナント招待トークン検証成功: invitationId={}, tenantId={}, displayName={}",
                invitation.getInvitationId(), tenant.getTenantId(), invitation.getDisplayName());

        return REDIRECT_ACCEPT_CONFIRM;
    }

    /**
     * 受諾確認画面を表示する。
     *
     * @param model モデル
     * @return 受諾確認テンプレート、またはエラー画面リダイレクト
     */
    @GetMapping("/accept-confirm")
    public String acceptConfirm(Model model) {
        if (!invitationSession.isPending()) {
            log.info("受諾確認画面: セッションpending無しのためエラー画面へ");
            return REDIRECT_ERROR + TenantInvitationError.INVALID_TOKEN.name();
        }

        String tenantId = invitationSession.getTenantId();

        // 受諾確認画面では、テナントの ENABLED プロバイダーを registrationId 付きで提示する。
        List<TenantOidcProviderChoiceView> choices =
                tenantOidcProviderService.findEnabledChoicesByTenantId(tenantId);

        model.addAttribute("displayName", invitationSession.getDisplayName());
        model.addAttribute("providers", choices);
        return VIEW_ACCEPT_CONFIRM;
    }

    /**
     * エラー画面を表示する。
     *
     * @param code エラーコード（クエリパラメータ）
     * @param model モデル
     * @return エラーテンプレート
     */
    @GetMapping("/error")
    public String error(@RequestParam(required = false) String code, Model model) {
        // エラー画面表示時にセッションをクリア
        invitationSession.clear();

        String normalized = normalizeErrorCode(code);
        model.addAttribute("errorCode", normalized);
        return VIEW_ERROR;
    }

    /**
     * エラーコードをホワイトリスト検証し、不明なコードはUNKNOWNに正規化する。
     *
     * @param raw クエリで受け取ったエラーコード
     * @return 正規化済みのエラーコード
     */
    private String normalizeErrorCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return ERROR_CODE_UNKNOWN;
        }
        for (TenantInvitationError e : TenantInvitationError.values()) {
            if (e.name().equals(raw)) {
                return raw;
            }
        }
        return ERROR_CODE_UNKNOWN;
    }

}
