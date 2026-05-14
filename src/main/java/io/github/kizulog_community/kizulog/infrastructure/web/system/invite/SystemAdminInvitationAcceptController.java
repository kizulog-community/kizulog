package io.github.kizulog_community.kizulog.infrastructure.web.system.invite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.kizulog_community.kizulog.domain.systemadmininvitation.exception.InvitationError;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.exception.InvitationException;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.SystemAdminInvitation;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.service.SystemAdminInvitationService;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.SystemOidcProviderService;
import lombok.RequiredArgsConstructor;

/**
 * 招待受諾コントローラー
 *
 * <p>招待リンク経由のフローを担当する。認証不要でアクセスできる。</p>
 *
 * <p>提供画面・URL:
 * <ul>
 *   <li>GET /system/invite/{token}        - トークン検証 → 受諾確認画面 or エラー画面</li>
 *   <li>GET /system/invite/accept-confirm - 受諾確認画面（プロバイダー選択）</li>
 *   <li>GET /system/invite/error          - エラー画面（エラーコード別メッセージ）</li>
 * </ul>
 *
 * @author Jun Kobayashi
 */
@Controller
@RequestMapping("/system/invite")
@RequiredArgsConstructor
public class SystemAdminInvitationAcceptController {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(SystemAdminInvitationAcceptController.class);

    /** エラーコード（不明） */
    private static final String ERROR_CODE_UNKNOWN = "UNKNOWN";

    /** 招待サービス */
    private final SystemAdminInvitationService invitationService;

    /** OIDCプロバイダーサービス */
    private final SystemOidcProviderService systemOidcProviderService;

    /** 招待受諾セッション */
    private final InvitationAcceptanceSession invitationSession;

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

        SystemAdminInvitation invitation;
        try {
            invitation = invitationService.findValidInvitationByToken(token);
        } catch (InvitationException e) {
            log.info("招待トークン検証失敗: error={}", e.getError());
            return "redirect:/system/invite/error?code=" + e.getError().name();
        }

        // 検証OK → セッションに保存
        invitationSession.setInvitationId(invitation.getInvitationId());
        invitationSession.setPlainToken(token);
        invitationSession.setDisplayName(invitation.getDisplayName());

        log.info("招待トークン検証成功: invitationId={}, displayName={}",
                invitation.getInvitationId(), invitation.getDisplayName());

        return "redirect:/system/invite/accept-confirm";
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
            return "redirect:/system/invite/error?code=" + InvitationError.INVALID_TOKEN.name();
        }

        model.addAttribute("displayName", invitationSession.getDisplayName());
        model.addAttribute("providers", systemOidcProviderService.listEnabledForLogin());
        return "system/invite/accept-confirm";
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
        return "system/invite/error";
    }

    /**
     * エラーコードをホワイトリスト検証し、不明なコードはUNKNOWNに正規化する。
     */
    private String normalizeErrorCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return ERROR_CODE_UNKNOWN;
        }
        for (InvitationError e : InvitationError.values()) {
            if (e.name().equals(raw)) {
                return raw;
            }
        }
        return ERROR_CODE_UNKNOWN;
    }

}
