package io.github.kizulog_community.kizulog.infrastructure.web.system;

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

import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantHostException;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantRegistrationException;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantStatusChangeException;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantUpdateException;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantDetailView;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostStatusValue;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantListItemView;
import io.github.kizulog_community.kizulog.domain.tenant.service.TenantManagementService;
import io.github.kizulog_community.kizulog.domain.tenantoidc.service.TenantOidcProviderService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenants.dto.TenantEditForm;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenants.dto.TenantHostAddForm;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenants.dto.TenantHostStatusChangeForm;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenants.dto.TenantRegistrationForm;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenants.dto.TenantStatusChangeForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * テナント管理画面のコントローラー
 *
 * <p>提供画面:</p>
 * <ul>
 * <li>GET  /system/tenants                                       - 一覧</li>
 * <li>GET  /system/tenants/new                                   - 新規登録フォーム</li>
 * <li>POST /system/tenants                                       - 新規登録実行</li>
 * <li>GET  /system/tenants/{tenantId}                            - 詳細</li>
 * <li>GET  /system/tenants/{tenantId}/edit                       - 編集フォーム</li>
 * <li>POST /system/tenants/{tenantId}                            - name更新</li>
 * <li>POST /system/tenants/{tenantId}/status                     - ステータス変更</li>
 * <li>POST /system/tenants/{tenantId}/hosts                      - host追加</li>
 * <li>POST /system/tenants/{tenantId}/hosts/{host}/status        - hostステータス変更</li>
 * </ul>
 *
 * <p>テナント識別方式: 方式3「全URLにテナント識別子」。
 * URLパターン: https://{host}/t/{slug}/...</p>
 *
 * @author Jun Kobayashi
 */
@Controller
@RequestMapping("/system/tenants")
@RequiredArgsConstructor
public class SystemTenantsController {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(SystemTenantsController.class);

    /** テナント管理サービス */
    private final TenantManagementService tenantManagementService;

    /** テナントOIDCプロバイダー管理サービス */
    private final TenantOidcProviderService tenantOidcProviderService;

    /** メッセージソース */
    private final MessageSource messageSource;

    /**
     * テナント一覧画面を表示する。
     *
     * @param model モデル
     * @return 一覧テンプレート
     */
    @GetMapping
    public String list(Model model) {
        List<TenantListItemView> items = tenantManagementService.listAllTenants();
        model.addAttribute("activeMenu", "tenants");
        model.addAttribute("items", items);
        return "system/tenants/list";
    }

    /**
     * 新規登録フォーム画面を表示する。
     *
     * @param model モデル
     * @return 新規登録テンプレート
     */
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("activeMenu", "tenants");
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new TenantRegistrationForm());
        }
        return "system/tenants/new";
    }

    /**
     * 新規テナントを登録する。
     *
     * @param form フォーム
     * @param bindingResult 検証結果
     * @param principal 認証済みプリンシパル
     * @param locale ロケール
     * @param redirectAttrs リダイレクト属性
     * @return 詳細画面または新規登録フォームへのリダイレクト
     */
    @PostMapping
    public String register(
            @Valid @ModelAttribute("registrationForm") TenantRegistrationForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            RedirectAttributes redirectAttrs) {

        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute(
                    "org.springframework.validation.BindingResult.registrationForm",
                    bindingResult);
            redirectAttrs.addFlashAttribute("registrationForm", form);
            return "redirect:/system/tenants/new";
        }

        String operatorId = (principal != null) ? principal.getAccountId() : "system";
        String tenantId;
        try {
            tenantId = tenantManagementService.registerTenant(
                    form.getName(),
                    form.getHosts(),
                    form.getReason(),
                    operatorId);
        } catch (TenantRegistrationException e) {
            log.warn("テナント新規登録に失敗: error={}", e.getError());
            String errorMessage = messageSource.getMessage(
                    "system.tenants.form.error." + e.getError().name(),
                    null,
                    "Registration failed",
                    locale);
            redirectAttrs.addFlashAttribute("flashErrorMessage", errorMessage);
            redirectAttrs.addFlashAttribute("registrationForm", form);
            return "redirect:/system/tenants/new";
        }

        log.info("テナントを新規登録しました: tenantId={}, name={}, operatorId={}",
                tenantId, form.getName(), operatorId);
        redirectAttrs.addFlashAttribute("flashSuccessKey",
                "system.tenants.register.success");
        return "redirect:/system/tenants/" + tenantId;
    }

    /**
     * テナント詳細画面を表示する。
     *
     * @param id テナントID
     * @param request HTTPリクエスト（スキーマ取得用）
     * @param model モデル
     * @param redirectAttrs リダイレクト属性（見つからない場合のリダイレクト用）
     * @return 詳細テンプレート、または一覧へのリダイレクト
     */
    @GetMapping("/{tenantId}")
    public String detail(
            @PathVariable("tenantId") String id,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttrs) {

        Optional<TenantDetailView> opt = tenantManagementService.findTenantDetail(id);
        if (opt.isEmpty()) {
            redirectAttrs.addFlashAttribute("flashErrorKey",
                    "system.tenants.error.notFound");
            return "redirect:/system/tenants";
        }

        model.addAttribute("activeMenu", "tenants");
        model.addAttribute("tenant", opt.get());
        // ログインURL組み立て用のスキーマ
        model.addAttribute("loginUrlScheme", request.getScheme());
        // テナントに登録されたOIDCプロバイダー一覧（display_name昇順）
        model.addAttribute("oidcProviders",
                tenantOidcProviderService.listAllByTenantId(id));

        if (!model.containsAttribute("statusChangeForm")) {
            model.addAttribute("statusChangeForm", new TenantStatusChangeForm());
        }
        if (!model.containsAttribute("hostAddForm")) {
            model.addAttribute("hostAddForm", new TenantHostAddForm());
        }
        if (!model.containsAttribute("hostStatusChangeForm")) {
            model.addAttribute("hostStatusChangeForm", new TenantHostStatusChangeForm());
        }
        return "system/tenants/detail";
    }

    /**
     * 編集フォーム画面を表示する。
     *
     * @param id テナントID
     * @param model モデル
     * @param redirectAttrs リダイレクト属性
     * @return 編集テンプレート、または一覧へのリダイレクト
     */
    @GetMapping("/{tenantId}/edit")
    public String editForm(
            @PathVariable("tenantId") String id,
            Model model,
            RedirectAttributes redirectAttrs) {

        Optional<TenantDetailView> opt = tenantManagementService.findTenantDetail(id);
        if (opt.isEmpty()) {
            redirectAttrs.addFlashAttribute("flashErrorKey",
                    "system.tenants.error.notFound");
            return "redirect:/system/tenants";
        }

        model.addAttribute("activeMenu", "tenants");
        model.addAttribute("tenant", opt.get());

        if (!model.containsAttribute("editForm")) {
            TenantEditForm form = new TenantEditForm();
            form.setName(opt.get().getName());
            model.addAttribute("editForm", form);
        }
        return "system/tenants/edit";
    }

    /**
     * テナントのnameを更新する。
     *
     * @param id テナントID
     * @param form フォーム
     * @param bindingResult 検証結果
     * @param principal 認証済みプリンシパル
     * @param locale ロケール
     * @param redirectAttrs リダイレクト属性
     * @return 詳細画面または編集フォームへのリダイレクト
     */
    @PostMapping("/{tenantId}")
    public String update(
            @PathVariable("tenantId") String id,
            @Valid @ModelAttribute("editForm") TenantEditForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            RedirectAttributes redirectAttrs) {

        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute(
                    "org.springframework.validation.BindingResult.editForm",
                    bindingResult);
            redirectAttrs.addFlashAttribute("editForm", form);
            return "redirect:/system/tenants/" + id + "/edit";
        }

        String operatorId = (principal != null) ? principal.getAccountId() : "system";
        try {
            tenantManagementService.updateTenantName(
                    id, form.getName(), form.getReason(), operatorId);
        } catch (TenantUpdateException e) {
            log.warn("テナントname更新に失敗: tenantId={}, error={}", id, e.getError());
            String errorMessage = messageSource.getMessage(
                    "system.tenants.form.error." + e.getError().name(),
                    null,
                    "Update failed",
                    locale);
            redirectAttrs.addFlashAttribute("flashErrorMessage", errorMessage);
            redirectAttrs.addFlashAttribute("editForm", form);
            return "redirect:/system/tenants/" + id + "/edit";
        }

        log.info("テナントnameを更新しました: tenantId={}, name={}, operatorId={}",
                id, form.getName(), operatorId);
        redirectAttrs.addFlashAttribute("flashSuccessKey",
                "system.tenants.update.success");
        return "redirect:/system/tenants/" + id;
    }

    /**
     * テナントのステータスを変更する。
     *
     * @param id テナントID
     * @param form フォーム
     * @param bindingResult 検証結果
     * @param principal 認証済みプリンシパル
     * @param locale ロケール
     * @param redirectAttrs リダイレクト属性
     * @return 詳細画面へリダイレクト
     */
    @PostMapping("/{tenantId}/status")
    public String changeStatus(
            @PathVariable("tenantId") String id,
            @Valid @ModelAttribute("statusChangeForm") TenantStatusChangeForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            RedirectAttributes redirectAttrs) {

        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute(
                    "org.springframework.validation.BindingResult.statusChangeForm",
                    bindingResult);
            redirectAttrs.addFlashAttribute("statusChangeForm", form);
            redirectAttrs.addFlashAttribute("openModal", "tenantStatusChange");
            return "redirect:/system/tenants/" + id;
        }

        String operatorId = (principal != null) ? principal.getAccountId() : "system";
        try {
            tenantManagementService.changeTenantStatus(
                    id, form.getTargetStatus(), form.getReason(), operatorId);
        } catch (TenantStatusChangeException e) {
            log.warn("テナントステータス変更に失敗: tenantId={}, error={}",
                    id, e.getError());
            String errorMessage = messageSource.getMessage(
                    "system.tenants.form.error." + e.getError().name(),
                    null,
                    "Status change failed",
                    locale);
            redirectAttrs.addFlashAttribute("flashErrorMessage", errorMessage);
            return "redirect:/system/tenants/" + id;
        }

        log.info("テナントステータスを変更しました: tenantId={}, newStatus={}, operatorId={}",
                id, form.getTargetStatus(), operatorId);
        redirectAttrs.addFlashAttribute("flashSuccessKey",
                "system.tenants.statusChange.success");
        return "redirect:/system/tenants/" + id;
    }

    /**
     * テナントに新しいhostを追加する。
     *
     * @param id テナントID
     * @param form フォーム
     * @param bindingResult 検証結果
     * @param principal 認証済みプリンシパル
     * @param locale ロケール
     * @param redirectAttrs リダイレクト属性
     * @return 詳細画面へリダイレクト
     */
    @PostMapping("/{tenantId}/hosts")
    public String addHost(
            @PathVariable("tenantId") String id,
            @Valid @ModelAttribute("hostAddForm") TenantHostAddForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            RedirectAttributes redirectAttrs) {

        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute(
                    "org.springframework.validation.BindingResult.hostAddForm",
                    bindingResult);
            redirectAttrs.addFlashAttribute("hostAddForm", form);
            redirectAttrs.addFlashAttribute("openModal", "hostAdd");
            return "redirect:/system/tenants/" + id;
        }

        String operatorId = (principal != null) ? principal.getAccountId() : "system";
        try {
            tenantManagementService.addHost(
                    id, form.getHost(), form.getReason(), operatorId);
        } catch (TenantHostException e) {
            log.warn("hostの追加に失敗: tenantId={}, host={}, error={}",
                    id, form.getHost(), e.getError());
            String errorMessage = messageSource.getMessage(
                    "system.tenants.form.error." + e.getError().name(),
                    null,
                    "Host add failed",
                    locale);
            redirectAttrs.addFlashAttribute("flashErrorMessage", errorMessage);
            redirectAttrs.addFlashAttribute("hostAddForm", form);
            redirectAttrs.addFlashAttribute("openModal", "hostAdd");
            return "redirect:/system/tenants/" + id;
        }

        log.info("テナントhostを追加しました: tenantId={}, host={}, operatorId={}",
                id, form.getHost(), operatorId);
        redirectAttrs.addFlashAttribute("flashSuccessKey",
                "system.tenants.hostAdd.success");
        return "redirect:/system/tenants/" + id;
    }

    /**
     * テナントhostのステータスを変更する（無効化/再有効化共用）
     *
     * @param id テナントID
     * @param hst ホスト名
     * @param form フォーム
     * @param bindingResult 検証結果
     * @param principal 認証済みプリンシパル
     * @param locale ロケール
     * @param redirectAttrs リダイレクト属性
     * @return 詳細画面へリダイレクト
     */
    @PostMapping("/{tenantId}/hosts/{host:.+}/status")
    public String changeHostStatus(
            @PathVariable("tenantId") String id,
            @PathVariable("host") String hst,
            @Valid @ModelAttribute("hostStatusChangeForm") TenantHostStatusChangeForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            RedirectAttributes redirectAttrs) {

        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute(
                    "org.springframework.validation.BindingResult.hostStatusChangeForm",
                    bindingResult);
            redirectAttrs.addFlashAttribute("hostStatusChangeForm", form);
            redirectAttrs.addFlashAttribute("openModal", "hostStatusChange");
            redirectAttrs.addFlashAttribute("openModalHost", hst);
            return "redirect:/system/tenants/" + id;
        }

        String operatorId = (principal != null) ? principal.getAccountId() : "system";
        try {
            if (form.getTargetStatus() == TenantHostStatusValue.INACTIVE) {
                tenantManagementService.disableHost(
                        id, hst, form.getReason(), operatorId);
            } else {
                tenantManagementService.enableHost(
                        id, hst, form.getReason(), operatorId);
            }
        } catch (TenantHostException e) {
            log.warn("hostステータス変更に失敗: tenantId={}, host={}, error={}",
                    id, hst, e.getError());
            String errorMessage = messageSource.getMessage(
                    "system.tenants.form.error." + e.getError().name(),
                    null,
                    "Host status change failed",
                    locale);
            redirectAttrs.addFlashAttribute("flashErrorMessage", errorMessage);
            return "redirect:/system/tenants/" + id;
        }

        log.info("hostステータスを変更しました: tenantId={}, host={}, "
                        + "newStatus={}, operatorId={}",
                id, hst, form.getTargetStatus(), operatorId);
        redirectAttrs.addFlashAttribute("flashSuccessKey",
                "system.tenants.hostStatusChange.success");
        return "redirect:/system/tenants/" + id;
    }

}
