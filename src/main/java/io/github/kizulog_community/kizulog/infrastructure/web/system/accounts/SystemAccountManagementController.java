package io.github.kizulog_community.kizulog.infrastructure.web.system.accounts;

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

import io.github.kizulog_community.kizulog.domain.systemaccount.exception.AccountStatusChangeException;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountDetailView;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountListItemView;
import io.github.kizulog_community.kizulog.domain.systemaccount.service.SystemAccountManagementService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import io.github.kizulog_community.kizulog.infrastructure.web.system.accounts.dto.AccountStatusChangeForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * システム管理アカウント管理画面のコントローラー
 *
 * <p>提供画面:
 * <ul>
 * <li>GET  /system/accounts/list                       - 一覧</li>
 * <li>GET  /system/accounts/{accountId}                - 詳細</li>
 * <li>POST /system/accounts/{accountId}/status         - ステータス変更</li>
 * </ul>
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

    /**
     * アカウント一覧画面を表示する。
     *
     * @param principal 認証済みプリンシパル
     * @param model モデル
     * @return 一覧テンプレート
     */
    @GetMapping("/list")
    public String list(
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Model model) {

        String operatorId = (principal != null) ? principal.getAccountId() : null;
        List<AccountListItemView> items =
                accountManagementService.listAllAccounts(operatorId);

        model.addAttribute("activeMenu", "accounts");
        model.addAttribute("items", items);
        return "system/accounts/list";
    }

    /**
     * アカウント詳細画面を表示する。
     *
     * @param accountId アカウントID
     * @param principal 認証済みプリンシパル
     * @param model モデル
     * @param redirectAttrs リダイレクト属性（見つからない場合のリダイレクト用）
     * @return 詳細テンプレート、または一覧へのリダイレクト
     */
    @GetMapping("/{accountId}")
    public String detail(
            @PathVariable("accountId") String id,
            @AuthenticationPrincipal SystemUserPrincipal principal,
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

        if (!model.containsAttribute("statusChangeForm")) {
            model.addAttribute("statusChangeForm", new AccountStatusChangeForm());
        }
        return "system/accounts/detail";
    }

    /**
     * アカウントのステータスを変更する。
     *
     * @param accountId アカウントID
     * @param form フォーム
     * @param bindingResult 検証結果
     * @param principal 認証済みプリンシパル
     * @param locale ロケール
     * @param redirectAttrs リダイレクト属性
     * @return 詳細画面へリダイレクト
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