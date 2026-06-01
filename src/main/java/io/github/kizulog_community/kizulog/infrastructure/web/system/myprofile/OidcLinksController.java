package io.github.kizulog_community.kizulog.infrastructure.web.system.myprofile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.kizulog_community.kizulog.domain.systemaccount.exception.IdentityLinkError;
import io.github.kizulog_community.kizulog.domain.systemaccount.exception.IdentityLinkException;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.LinkedIdentityView;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentity;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.service.SystemAccountIdentityLinkService;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.ClaimsMappingTarget;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.ProviderWithStatus;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.IdentityClaimsViewService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import lombok.RequiredArgsConstructor;

/**
 * マイプロフィール: 連携OIDCプロバイダー管理コントローラー
 *
 * <p>URL設計：</p>
 * <ul>
 * <li>GET  /system/my-profile/oidc-links                       - 一覧</li>
 * <li>GET  /system/my-profile/oidc-links/add                   - 追加プロバイダー選択</li>
 * <li>POST /system/my-profile/oidc-links/start                 - OIDC認可リクエスト開始（CSRF守備）</li>
 * <li>POST /system/my-profile/oidc-links/{identityId}/unlink   - 連携解除（CSRF守備）</li>
 * <li>GET  /system/my-profile/oidc-links/error                 - エラー画面</li>
 * </ul>
 *
 * @author Jun Kobayashi
 */
@Controller
@RequestMapping("/system/my-profile/oidc-links")
@RequiredArgsConstructor
public class OidcLinksController {

    /** ロガー */
    private static final Logger log = LoggerFactory.getLogger(OidcLinksController.class);

    /** エラーコード（不明） */
    private static final String ERROR_CODE_UNKNOWN = "UNKNOWN";

    /** Spring Security 標準の認可開始URLプレフィックス */
    private static final String OAUTH2_AUTHORIZATION_PREFIX = "/oauth2/authorization/";

    /** identityリンクサービス */
    private final SystemAccountIdentityLinkService identityLinkService;

    /** identityリンクセッション(session-scopedプロキシBean) */
    private final IdentityLinkSession identityLinkSession;

    /** identityリポジトリ（aud取得用） */
    private final SystemAccountIdentityRepository identityRepository;

    /** Identityクレームビューサービス */
    private final IdentityClaimsViewService identityClaimsViewService;

    /**
     * 連携済みOIDCプロバイダー一覧画面を表示する。
     *
     * @param principal 認証済みプリンシパル
     * @param model モデル
     * @return ビュー名
     */
    @GetMapping
    public String list(
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Model model) {

        String accountId = principal.getAccountId();
        String currentIdentityId = principal.getIdentityId();

        List<LinkedIdentityView> items =
                identityLinkService.listLinkedIdentities(accountId, currentIdentityId);

        long activeCount = items.stream().filter(LinkedIdentityView::isActive).count();

        // T.0: 各identityの aud とクレーム連携情報を取得
        Map<String, String> audPerIdentity = new LinkedHashMap<>();
        Map<String, Map<String, String>> claimsPerIdentity = new LinkedHashMap<>();

        for (LinkedIdentityView item : items) {
            String identityId = item.getIdentityId();

            // aud は identity テーブルから直接取得
            Optional<SystemAccountIdentity> identityOpt =
                    identityRepository.findLatestByIdentityId(identityId);
            identityOpt.ifPresent(identity ->
                    audPerIdentity.put(identityId, identity.getAud()));

            // クレーム連携情報を取得（fail-openで空マップになることもある）
            Map<String, String> claims =
                    identityClaimsViewService.resolveClaimsView(identityId);
            claimsPerIdentity.put(identityId, claims);
        }

        model.addAttribute("activeMenu", "my-profile");
        model.addAttribute("items", items);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("audPerIdentity", audPerIdentity);
        model.addAttribute("claimsPerIdentity", claimsPerIdentity);
        model.addAttribute("claimsMappingTargets", ClaimsMappingTarget.orderedList());
        return "system/my-profile/oidc-links/list";
    }

    /**
     * 追加可能なプロバイダー一覧から選択する画面を表示する。
     *
     * @param principal 認証済みプリンシパル
     * @param model モデル
     * @return ビュー名
     */
    @GetMapping("/add")
    public String addForm(
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Model model) {

        // 過去のpending状態が残っていればクリア
        identityLinkSession.clear();

        String accountId = principal.getAccountId();
        List<ProviderWithStatus> providers =
                identityLinkService.listLinkableProvidersForAccount(accountId);

        model.addAttribute("activeMenu", "my-profile");
        model.addAttribute("providers", providers);
        return "system/my-profile/oidc-links/add";
    }

    /**
     * OIDC認可リクエストを開始する。
     *
     * @param id 連携対象のproviderId
     * @param principal 認証済みプリンシパル
     * @param redirectAttrs リダイレクト属性
     * @return リダイレクト先
     */
    @PostMapping("/start")
    public String startLink(
            @RequestParam("providerId") String id,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            RedirectAttributes redirectAttrs) {

        if (id == null || id.isBlank()) {
            log.warn("identityリンク開始拒否: providerId未指定 accountId={}",
                    principal.getAccountId());
            return "redirect:/system/my-profile/oidc-links/error?code="
                    + IdentityLinkError.PROVIDER_NOT_FOUND.name();
        }

        identityLinkSession.setTargetAccountId(principal.getAccountId());
        identityLinkSession.setProviderId(id);

        log.info("identityリンク開始: accountId={}, providerId={}",
                principal.getAccountId(), id);

        return "redirect:" + OAUTH2_AUTHORIZATION_PREFIX + id;
    }

    /**
     * 連携解除を実行する。
     *
     * @param identityId 解除対象のidentityId
     * @param principal 認証済みプリンシパル
     * @param redirectAttrs リダイレクト属性
     * @return リダイレクト先
     */
    @PostMapping("/{identityId}/unlink")
    public String unlink(
            @PathVariable String identityId,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            RedirectAttributes redirectAttrs) {

        String accountId = principal.getAccountId();
        String currentIdentityId = principal.getIdentityId();

        try {
            identityLinkService.unlinkIdentity(
                    accountId, identityId, currentIdentityId,
                    "User-initiated unlink");
        } catch (IdentityLinkException e) {
            log.info("identity解除失敗: accountId={}, identityId={}, error={}",
                    accountId, identityId, e.getError());
            redirectAttrs.addFlashAttribute(
                    "flashErrorKey",
                    "system.my-profile.oidc-links.error." + e.getError().name());
            return "redirect:/system/my-profile/oidc-links";
        }

        redirectAttrs.addFlashAttribute(
                "flashSuccessKey", "system.my-profile.oidc-links.flash.unlinked");
        return "redirect:/system/my-profile/oidc-links";
    }

    /**
     * エラー画面を表示する。
     *
     * @param code エラーコード（クエリパラメータ）
     * @param model モデル
     * @return ビュー名
     */
    @GetMapping("/error")
    public String error(
            @RequestParam(required = false) String code,
            Model model) {

        // エラー画面到達でセッションpending状態をクリア
        identityLinkSession.clear();

        model.addAttribute("activeMenu", "my-profile");
        model.addAttribute("errorCode", normalizeErrorCode(code));
        return "system/my-profile/oidc-links/error";
    }

    /**
     * エラーコードをホワイトリスト検証し、不明なコードは UNKNOWN に正規化する。
     */
    private String normalizeErrorCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return ERROR_CODE_UNKNOWN;
        }
        for (IdentityLinkError e : IdentityLinkError.values()) {
            if (e.name().equals(raw)) {
                return raw;
            }
        }
        return ERROR_CODE_UNKNOWN;
    }

}
