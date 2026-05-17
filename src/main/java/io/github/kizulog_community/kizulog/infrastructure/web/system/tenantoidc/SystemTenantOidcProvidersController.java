package io.github.kizulog_community.kizulog.infrastructure.web.system.tenantoidc;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.kizulog_community.kizulog.domain.systemconfig.exception.OidcConnectionException;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.OidcProviderService;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantDetailView;
import io.github.kizulog_community.kizulog.domain.tenant.service.TenantManagementService;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderRegistrationException;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderStatusChangeException;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderUpdateException;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.EnabledTenantOidcProviderView;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderDetailView;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderListItemView;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderStatusValue;
import io.github.kizulog_community.kizulog.domain.tenantoidc.service.TenantOidcProviderService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto.OidcConnectionTestResponse;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenantoidc.dto.TenantOidcConnectionTestRequest;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenantoidc.dto.TenantOidcProviderEditForm;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenantoidc.dto.TenantOidcProviderRegistrationForm;
import io.github.kizulog_community.kizulog.infrastructure.web.system.tenantoidc.dto.TenantOidcProviderStatusChangeForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * システム管理側 業務テナントOIDCプロバイダー管理コントローラー
 *
 * <p>提供画面:</p>
 * <ul>
 * <li>GET  /system/tenants/{tenantId}/oidc-providers                       - 一覧</li>
 * <li>GET  /system/tenants/{tenantId}/oidc-providers/new                   - 新規登録フォーム</li>
 * <li>POST /system/tenants/{tenantId}/oidc-providers                       - 新規登録実行</li>
 * <li>GET  /system/tenants/{tenantId}/oidc-providers/{providerId}          - 詳細</li>
 * <li>GET  /system/tenants/{tenantId}/oidc-providers/{providerId}/edit     - 編集フォーム</li>
 * <li>POST /system/tenants/{tenantId}/oidc-providers/{providerId}          - 編集実行</li>
 * <li>POST /system/tenants/{tenantId}/oidc-providers/{providerId}/status   - ステータス変更</li>
 * </ul>
 *
 * <p>接続確認API（Ajax用、JSON返却）:</p>
 * <ul>
 * <li>POST /system/tenants/{tenantId}/oidc-providers/test-connection                       - 新規登録時</li>
 * <li>POST /system/tenants/{tenantId}/oidc-providers/{providerId}/test-connection-for-edit - 編集時</li>
 * </ul>
 *
 * @author Jun Kobayashi
 */
@Controller
@RequestMapping("/system/tenants/{tenantId}/oidc-providers")
@RequiredArgsConstructor
public class SystemTenantOidcProvidersController {

    private static final Logger log =
            LoggerFactory.getLogger(SystemTenantOidcProvidersController.class);

    private final TenantOidcProviderService tenantOidcProviderService;
    private final TenantManagementService tenantManagementService;
    private final OidcProviderService oidcProviderService;
    private final MessageSource messageSource;

    /**
     * テナントのOIDCプロバイダー一覧画面を表示する。
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

        List<TenantOidcProviderListItemView> items =
                tenantOidcProviderService.listAllByTenantId(tId);

        model.addAttribute("activeMenu", "tenants");
        model.addAttribute("tenant", tenantOpt.get());
        model.addAttribute("items", items);
        return "system/tenants/oidc-providers/list";
    }

    /**
     * 新規登録フォーム画面を表示する。
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

        model.addAttribute("activeMenu", "tenants");
        model.addAttribute("tenant", tenantOpt.get());
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm",
                    new TenantOidcProviderRegistrationForm());
        }
        return "system/tenants/oidc-providers/new";
    }

    /**
     * 新規プロバイダーを登録する。
     */
    @PostMapping
    public String register(
            @PathVariable("tenantId") String tId,
            @Valid @ModelAttribute("registrationForm") TenantOidcProviderRegistrationForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            RedirectAttributes redirectAttrs) {

        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute(
                    "org.springframework.validation.BindingResult.registrationForm",
                    bindingResult);
            redirectAttrs.addFlashAttribute("registrationForm", form);
            return "redirect:/system/tenants/" + tId + "/oidc-providers/new";
        }

        String operatorId = (principal != null) ? principal.getAccountId() : "system";
        try {
            tenantOidcProviderService.registerProvider(
                    tId,
                    form.getProviderId(),
                    form.getDisplayName(),
                    form.getIss(),
                    form.getAud(),
                    form.getClientId(),
                    form.getClientSecret(),
                    form.getReason(),
                    operatorId);
        } catch (TenantOidcProviderRegistrationException e) {
            log.warn("テナントOIDCプロバイダー登録に失敗: tenantId={}, error={}",
                    tId, e.getError());
            String errorMessage = messageSource.getMessage(
                    "system.tenants.oidc.form.error." + e.getError().name(),
                    null,
                    "OIDC provider registration failed",
                    locale);
            redirectAttrs.addFlashAttribute("flashErrorMessage", errorMessage);
            redirectAttrs.addFlashAttribute("registrationForm", form);
            return "redirect:/system/tenants/" + tId + "/oidc-providers/new";
        }

        log.info("テナントOIDCプロバイダーを登録しました: tenantId={}, providerId={}, operatorId={}",
                tId, form.getProviderId(), operatorId);
        redirectAttrs.addFlashAttribute("flashSuccessKey",
                "system.tenants.oidc.register.success");
        return "redirect:/system/tenants/" + tId
                + "/oidc-providers/" + form.getProviderId();
    }

    /**
     * プロバイダー詳細画面を表示する。
     *
     * <p>「最後のENABLED」警告判定のため、現在のプロバイダーがENABLEDの場合のみ
     * 他のENABLEDプロバイダー数を計算し、isLastEnabled をModelに追加する。</p>
     */
    @GetMapping("/{providerId}")
    public String detail(
            @PathVariable("tenantId") String tId,
            @PathVariable("providerId") String pId,
            Model model,
            RedirectAttributes redirectAttrs) {

        Optional<TenantDetailView> tenantOpt =
                tenantManagementService.findTenantDetail(tId);
        if (tenantOpt.isEmpty()) {
            redirectAttrs.addFlashAttribute("flashErrorKey",
                    "system.tenants.error.notFound");
            return "redirect:/system/tenants";
        }

        Optional<TenantOidcProviderDetailView> providerOpt =
                tenantOidcProviderService.findDetail(tId, pId);
        if (providerOpt.isEmpty()) {
            redirectAttrs.addFlashAttribute("flashErrorKey",
                    "system.tenants.oidc.error.notFound");
            return "redirect:/system/tenants/" + tId + "/oidc-providers";
        }

        // 「最後のENABLED」警告判定:
        // 現在のプロバイダーがENABLEDで、かつ他にENABLEDが存在しない場合のみ true
        boolean isLastEnabled = false;
        TenantOidcProviderDetailView provider = providerOpt.get();
        if (provider.getCurrentStatus() == TenantOidcProviderStatusValue.ENABLED) {
            List<EnabledTenantOidcProviderView> enabledList =
                    tenantOidcProviderService.findAllEnabledByTenantId(tId);
            long othersEnabled = enabledList.stream()
                    .filter(p -> !p.getProviderId().equals(pId))
                    .count();
            isLastEnabled = (othersEnabled == 0);
        }

        model.addAttribute("activeMenu", "tenants");
        model.addAttribute("tenant", tenantOpt.get());
        model.addAttribute("provider", provider);
        model.addAttribute("isLastEnabled", isLastEnabled);
        if (!model.containsAttribute("statusChangeForm")) {
            model.addAttribute("statusChangeForm",
                    new TenantOidcProviderStatusChangeForm());
        }
        return "system/tenants/oidc-providers/detail";
    }

    /**
     * 編集フォーム画面を表示する。
     */
    @GetMapping("/{providerId}/edit")
    public String editForm(
            @PathVariable("tenantId") String tId,
            @PathVariable("providerId") String pId,
            Model model,
            RedirectAttributes redirectAttrs) {

        Optional<TenantDetailView> tenantOpt =
                tenantManagementService.findTenantDetail(tId);
        if (tenantOpt.isEmpty()) {
            redirectAttrs.addFlashAttribute("flashErrorKey",
                    "system.tenants.error.notFound");
            return "redirect:/system/tenants";
        }

        Optional<TenantOidcProviderDetailView> providerOpt =
                tenantOidcProviderService.findDetail(tId, pId);
        if (providerOpt.isEmpty()) {
            redirectAttrs.addFlashAttribute("flashErrorKey",
                    "system.tenants.oidc.error.notFound");
            return "redirect:/system/tenants/" + tId + "/oidc-providers";
        }

        model.addAttribute("activeMenu", "tenants");
        model.addAttribute("tenant", tenantOpt.get());
        model.addAttribute("provider", providerOpt.get());

        if (!model.containsAttribute("editForm")) {
            TenantOidcProviderEditForm form = new TenantOidcProviderEditForm();
            form.setDisplayName(providerOpt.get().getDisplayName());
            form.setClientId(providerOpt.get().getClientId());
            // clientSecret は表示しない（空のまま）
            model.addAttribute("editForm", form);
        }
        return "system/tenants/oidc-providers/edit";
    }

    /**
     * プロバイダーを更新する。
     */
    @PostMapping("/{providerId}")
    public String update(
            @PathVariable("tenantId") String tId,
            @PathVariable("providerId") String pId,
            @Valid @ModelAttribute("editForm") TenantOidcProviderEditForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            RedirectAttributes redirectAttrs) {

        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute(
                    "org.springframework.validation.BindingResult.editForm",
                    bindingResult);
            redirectAttrs.addFlashAttribute("editForm", form);
            return "redirect:/system/tenants/" + tId
                    + "/oidc-providers/" + pId + "/edit";
        }

        String operatorId = (principal != null) ? principal.getAccountId() : "system";
        try {
            tenantOidcProviderService.updateProvider(
                    tId,
                    pId,
                    form.getDisplayName(),
                    form.getClientId(),
                    form.getClientSecret(),
                    form.getReason(),
                    operatorId);
        } catch (TenantOidcProviderUpdateException e) {
            log.warn("テナントOIDCプロバイダー更新に失敗: tenantId={}, providerId={}, error={}",
                    tId, pId, e.getError());
            String errorMessage = messageSource.getMessage(
                    "system.tenants.oidc.form.error." + e.getError().name(),
                    null,
                    "OIDC provider update failed",
                    locale);
            redirectAttrs.addFlashAttribute("flashErrorMessage", errorMessage);
            redirectAttrs.addFlashAttribute("editForm", form);
            return "redirect:/system/tenants/" + tId
                    + "/oidc-providers/" + pId + "/edit";
        }

        log.info("テナントOIDCプロバイダーを更新: tenantId={}, providerId={}, operatorId={}",
                tId, pId, operatorId);
        redirectAttrs.addFlashAttribute("flashSuccessKey",
                "system.tenants.oidc.update.success");
        return "redirect:/system/tenants/" + tId
                + "/oidc-providers/" + pId;
    }

    /**
     * プロバイダーのステータスを変更する。
     */
    @PostMapping("/{providerId}/status")
    public String changeStatus(
            @PathVariable("tenantId") String tId,
            @PathVariable("providerId") String pId,
            @Valid @ModelAttribute("statusChangeForm") TenantOidcProviderStatusChangeForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            RedirectAttributes redirectAttrs) {

        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute(
                    "org.springframework.validation.BindingResult.statusChangeForm",
                    bindingResult);
            redirectAttrs.addFlashAttribute("statusChangeForm", form);
            redirectAttrs.addFlashAttribute("openModal", "tenantOidcStatusChange");
            return "redirect:/system/tenants/" + tId
                    + "/oidc-providers/" + pId;
        }

        String operatorId = (principal != null) ? principal.getAccountId() : "system";
        try {
            tenantOidcProviderService.changeStatus(
                    tId,
                    pId,
                    form.getTargetStatus(),
                    form.getReason(),
                    operatorId);
        } catch (TenantOidcProviderStatusChangeException e) {
            log.warn("テナントOIDCプロバイダー ステータス変更に失敗: "
                            + "tenantId={}, providerId={}, error={}",
                    tId, pId, e.getError());
            String errorMessage = messageSource.getMessage(
                    "system.tenants.oidc.form.error." + e.getError().name(),
                    null,
                    "OIDC provider status change failed",
                    locale);
            redirectAttrs.addFlashAttribute("flashErrorMessage", errorMessage);
            return "redirect:/system/tenants/" + tId
                    + "/oidc-providers/" + pId;
        }

        log.info("テナントOIDCプロバイダー ステータスを変更: "
                        + "tenantId={}, providerId={}, newStatus={}, operatorId={}",
                tId, pId, form.getTargetStatus(), operatorId);
        redirectAttrs.addFlashAttribute("flashSuccessKey",
                "system.tenants.oidc.statusChange.success");
        return "redirect:/system/tenants/" + tId
                + "/oidc-providers/" + pId;
    }

    /**
     * 新規登録時の接続確認 API。
     *
     * @param tId テナントID（URL検証用、本処理では利用しない）
     * @param request 接続確認リクエスト
     * @param locale エラーメッセージ翻訳用ロケール
     * @return 接続結果（成功 or 失敗）
     */
    @PostMapping("/test-connection")
    @ResponseBody
    public ResponseEntity<OidcConnectionTestResponse> testConnection(
            @PathVariable("tenantId") String tId,
            @RequestBody TenantOidcConnectionTestRequest request,
            Locale locale) {
        try {
            oidcProviderService.verify(request.getIss());
            return ResponseEntity.ok(OidcConnectionTestResponse.success());
        } catch (OidcConnectionException e) {
            log.warn("テナントOIDC接続確認に失敗: tenantId={}, errorType={}",
                    tId, e.getErrorType());
            String errorMessage = messageSource.getMessage(
                    "system.oidcProviders.connection.error." + e.getErrorType().name(),
                    null,
                    "Connection failed",
                    locale);
            return ResponseEntity.ok(
                    OidcConnectionTestResponse.failure(
                            e.getErrorType().name(), errorMessage));
        }
    }

    /**
     * 編集時の接続確認 API。
     *
     * @param tId テナントID
     * @param pId プロバイダー識別子
     * @param locale エラーメッセージ翻訳用ロケール
     * @return 接続結果（成功 or 失敗）
     */
    @PostMapping("/{providerId}/test-connection-for-edit")
    @ResponseBody
    public ResponseEntity<OidcConnectionTestResponse> testConnectionForEdit(
            @PathVariable("tenantId") String tId,
            @PathVariable("providerId") String pId,
            Locale locale) {

        Optional<TenantOidcProviderDetailView> providerOpt =
                tenantOidcProviderService.findDetail(tId, pId);
        if (providerOpt.isEmpty()) {
            String errorMessage = messageSource.getMessage(
                    "system.tenants.oidc.error.notFound",
                    null,
                    "Provider not found",
                    locale);
            return ResponseEntity.ok(
                    OidcConnectionTestResponse.failure(
                            "PROVIDER_NOT_FOUND", errorMessage));
        }

        String iss = providerOpt.get().getIss();
        try {
            oidcProviderService.verify(iss);
            return ResponseEntity.ok(OidcConnectionTestResponse.success());
        } catch (OidcConnectionException e) {
            log.warn("テナントOIDC編集時接続確認に失敗: "
                            + "tenantId={}, providerId={}, errorType={}",
                    tId, pId, e.getErrorType());
            String errorMessage = messageSource.getMessage(
                    "system.oidcProviders.connection.error." + e.getErrorType().name(),
                    null,
                    "Connection failed",
                    locale);
            return ResponseEntity.ok(
                    OidcConnectionTestResponse.failure(
                            e.getErrorType().name(), errorMessage));
        }
    }

}