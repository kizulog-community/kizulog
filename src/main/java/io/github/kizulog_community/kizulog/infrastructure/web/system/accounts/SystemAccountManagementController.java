package io.github.kizulog_community.kizulog.infrastructure.web.system.accounts;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

import io.github.kizulog_community.kizulog.domain.systemaccount.exception.AccountStatusChangeException;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountDetailView;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountListItemView;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentity;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.service.SystemAccountManagementService;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProvider;
import io.github.kizulog_community.kizulog.domain.systemoidc.port.SystemOidcProviderRepository;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.IdentityClaimsViewService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import io.github.kizulog_community.kizulog.infrastructure.web.system.accounts.dto.AccountStatusChangeForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * システム管理アカウント管理画面のコントローラー
 *
 * @author Jun Kobayashi
 */
@Controller
@RequestMapping("/system/accounts")
@RequiredArgsConstructor
public class SystemAccountManagementController {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(SystemAccountManagementController.class);

    /** アカウント管理サービス */
    private final SystemAccountManagementService accountManagementService;

    /** メッセージソース */
    private final MessageSource messageSource;

    /** Identityリポジトリ（T.0シリーズで追加） */
    private final SystemAccountIdentityRepository identityRepository;

    /** OIDCプロバイダーリポジトリ（T.0シリーズで追加） */
    private final SystemOidcProviderRepository providerRepository;

    /** Identityクレームビューサービス（T.0シリーズで追加） */
    private final IdentityClaimsViewService identityClaimsViewService;

    /**
     * アカウント一覧画面を表示する。
     */
    @GetMapping("/list")
    public String list(
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            Model model) {

        String operatorId = (principal != null) ? principal.getAccountId() : null;
        List<AccountListItemView> items =
                accountManagementService.listAllAccounts(operatorId);

        Map<String, List<Map<String, String>>> claimsDisplayPerAccount = new LinkedHashMap<>();
        Map<String, Map<String, String>> claimsPerAccount = new LinkedHashMap<>();

        for (AccountListItemView item : items) {
            List<Map<String, String>> display =
                    identityClaimsViewService.resolveClaimsDisplayForAccount(
                            item.getAccountId(), locale);
            claimsDisplayPerAccount.put(item.getAccountId(), display);

            Map<String, String> claims =
                    identityClaimsViewService.resolveClaimsViewForAccount(item.getAccountId());
            claimsPerAccount.put(item.getAccountId(), claims);
        }

        model.addAttribute("activeMenu", "accounts");
        model.addAttribute("items", items);
        model.addAttribute("claimsDisplayPerAccount", claimsDisplayPerAccount);
        model.addAttribute("claimsPerAccount", claimsPerAccount);
        return "system/accounts/list";
    }

    /**
     * アカウント詳細画面を表示する。
     */
    @GetMapping("/{accountId}")
    public String detail(
            @PathVariable("accountId") String id,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            Model model,
            RedirectAttributes redirectAttrs) {

        String operatorId = (principal != null) ? principal.getAccountId() : null;
        Optional<AccountDetailView> opt =
                accountManagementService.findAccountDetail(id, operatorId);
        if (opt.isEmpty()) {
            redirectAttrs.addFlashAttribute("flashErrorKey",
                    "system.accounts.error.notFound");
            return "redirect:/system/accounts/list";
        }

        model.addAttribute("activeMenu", "accounts");
        model.addAttribute("account", opt.get());

        List<SystemAccountIdentity> identities = identityRepository.findLatestByAccountId(id);
        List<Map<String, Object>> identitiesView = new ArrayList<>();
        List<SystemOidcProvider> allProviders = providerRepository.findAllLatest();

        for (SystemAccountIdentity identity : identities) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("identityId", identity.getIdentityId());
            view.put("iss", identity.getIss());
            view.put("aud", identity.getAud());
            view.put("sub", identity.getSub());
            view.put("createdAt", identity.getCreatedAt());

            String providerDisplayName = resolveProviderDisplayName(
                    identity.getIss(), allProviders);
            view.put("providerDisplayName", providerDisplayName);

            List<Map<String, String>> claimsDisplay =
                    identityClaimsViewService.resolveClaimsDisplay(
                            identity.getIdentityId(), locale);
            view.put("claimsDisplay", claimsDisplay);

            identitiesView.add(view);
        }

        model.addAttribute("identitiesView", identitiesView);

        if (!model.containsAttribute("statusChangeForm")) {
            model.addAttribute("statusChangeForm", new AccountStatusChangeForm());
        }
        return "system/accounts/detail";
    }

    /**
     * iss URI から OIDC プロバイダの表示名を解決する。末尾スラッシュ揺れを吸収。
     */
    private String resolveProviderDisplayName(
            String iss, List<SystemOidcProvider> allProviders) {
        if (iss == null) {
            return null;
        }
        String normalizedIss = iss.replaceAll("/+$", "");
        for (SystemOidcProvider p : allProviders) {
            if (p.getUri() == null) {
                continue;
            }
            String normalizedUri = p.getUri().replaceAll("/+$", "");
            if (normalizedUri.equals(normalizedIss)) {
                return p.getDisplayName();
            }
        }
        return null;
    }

    /**
     * アカウントのステータスを変更する。
     */
    @PostMapping("/{accountId}/status")
    public String changeStatus(
            @PathVariable("accountId") String id,
            @Valid @ModelAttribute("statusChangeForm") AccountStatusChangeForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            RedirectAttributes redirectAttrs) {

        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute(
                    "org.springframework.validation.BindingResult.statusChangeForm",
                    bindingResult);
            redirectAttrs.addFlashAttribute("statusChangeForm", form);
            redirectAttrs.addFlashAttribute("openModal", "statusChange");
            return "redirect:/system/accounts/" + id;
        }

        String operatorId = (principal != null) ? principal.getAccountId() : "system";
        try {
            accountManagementService.changeAccountStatus(
                    id, form.getTargetStatus(), form.getReason(), operatorId);
        } catch (AccountStatusChangeException e) {
            log.warn("アカウントステータス変更に失敗: accountId={}, error={}",
                    id, e.getError());
            String errorMessage = messageSource.getMessage(
                    "system.accounts.form.error." + e.getError().name(),
                    null,
                    "Status change failed",
                    locale);
            redirectAttrs.addFlashAttribute("flashErrorMessage", errorMessage);
            return "redirect:/system/accounts/" + id;
        }

        log.info("アカウントステータスを変更しました: accountId={}, newStatus={}, operatorId={}",
                id, form.getTargetStatus(), operatorId);
        redirectAttrs.addFlashAttribute("flashSuccessKey",
                "system.accounts.statusChange.success");
        return "redirect:/system/accounts/" + id;
    }

}
