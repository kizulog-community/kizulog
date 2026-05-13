package io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityRepository;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.OidcConnectionException;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.OidcProviderService;
import io.github.kizulog_community.kizulog.domain.systemoidc.exception.OidcProviderError;
import io.github.kizulog_community.kizulog.domain.systemoidc.exception.OidcProviderException;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.OidcProviderStatusValue;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.ProviderWithStatus;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProvider;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProviderStatus;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.SystemOidcProviderService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto.OidcConnectionTestForEditRequest;
import io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto.OidcConnectionTestRequest;
import io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto.OidcConnectionTestResponse;
import io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto.OidcProviderDetailView;
import io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto.OidcProviderEditForm;
import io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto.OidcProviderForm;
import io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto.OidcProviderListItem;
import io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto.OidcProviderStatusChangeForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * システムOIDCプロバイダー管理画面のコントローラー
 *
 * @author Jun Kobayashi
 */
@Controller
@RequestMapping("/system/system-settings/oidc-providers")
@RequiredArgsConstructor
public class OidcProvidersController {

    /** ロガー */
    private static final Logger log = LoggerFactory.getLogger(OidcProvidersController.class);

    /** Client Secret 表示用マスク文字列 */
    private static final String SECRET_MASK = "••••••••••••••••";

    /** OIDCプロバイダーサービス */
    private final SystemOidcProviderService systemOidcProviderService;

    /** OIDCディスカバリサービス */
    private final OidcProviderService oidcProviderService;

    /** identityリポジトリ */
    private final SystemAccountIdentityRepository systemAccountIdentityRepository;

    /** メッセージソース */
    private final MessageSource messageSource;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "all") String filter,
            Model model) {
        List<ProviderWithStatus> all = systemOidcProviderService.listAll();

        List<OidcProviderListItem> items = all.stream()
                .filter(pws -> matchesFilter(pws, filter))
                .map(this::toListItem)
                .toList();

        model.addAttribute("activeMenu", "system-settings");
        model.addAttribute("items", items);
        model.addAttribute("filter", filter);
        return "system/system-settings/oidc-providers/list";
    }

    @GetMapping("/{providerId}")
    public String detail(
            @PathVariable String providerId,
            Model model,
            RedirectAttributes redirectAttrs) {
        Optional<ProviderWithStatus> opt =
                systemOidcProviderService.findDetailByProviderId(providerId);
        if (opt.isEmpty()) {
            redirectAttrs.addFlashAttribute("flashErrorKey",
                    "system.oidcProviders.error.notFound");
            return "redirect:/system/system-settings/oidc-providers";
        }

        OidcProviderDetailView view = toDetailView(opt.get());
        model.addAttribute("activeMenu", "system-settings");
        model.addAttribute("provider", view);

        // G.6: 「最低1つENABLED」UI事前ガード用フラグ
        // 現状ENABLEDで、他にENABLEDなプロバイダーが存在しない場合は無効化不可
        int otherEnabledCount = systemOidcProviderService.countOtherEnabled(providerId);
        model.addAttribute("canDisable", otherEnabledCount > 0);

        if (!model.containsAttribute("statusChangeForm")) {
            model.addAttribute("statusChangeForm", new OidcProviderStatusChangeForm());
        }
        return "system/system-settings/oidc-providers/detail";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("oidcProviderForm")) {
            model.addAttribute("oidcProviderForm", new OidcProviderForm());
        }
        model.addAttribute("activeMenu", "system-settings");
        return "system/system-settings/oidc-providers/new";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("oidcProviderForm") OidcProviderForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            RedirectAttributes redirectAttrs,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "system-settings");
            return "system/system-settings/oidc-providers/new";
        }

        try {
            oidcProviderService.verify(form.getUri());
        } catch (OidcConnectionException e) {
            String errorMessage = messageSource.getMessage(
                    "system.oidcProviders.connection.error." + e.getErrorType().name(),
                    null,
                    "Connection failed",
                    locale);
            bindingResult.reject("oidcConnectionError", errorMessage);
            model.addAttribute("activeMenu", "system-settings");
            return "system/system-settings/oidc-providers/new";
        }

        try {
            String createdBy = (principal != null) ? principal.getAccountId() : "system";
            systemOidcProviderService.registerWithValidation(
                    form.getProviderId(),
                    form.getDisplayName(),
                    form.getUri(),
                    form.getClientId(),
                    form.getClientSecret(),
                    createdBy);
        } catch (OidcProviderException e) {
            String errorMessage = messageSource.getMessage(
                    "system.oidcProviders.form.error." + e.getError().name(),
                    null,
                    "Validation failed",
                    locale);
            String fieldName = (e.getError() == OidcProviderError.PROVIDER_ID_INVALID_FORMAT
                    || e.getError() == OidcProviderError.PROVIDER_ID_DUPLICATE)
                    ? "providerId" : null;
            if (fieldName != null) {
                bindingResult.rejectValue(fieldName, "providerError", errorMessage);
            } else {
                bindingResult.reject("providerError", errorMessage);
            }
            model.addAttribute("activeMenu", "system-settings");
            return "system/system-settings/oidc-providers/new";
        }

        log.info("OIDCプロバイダーを登録しました: providerId={}", form.getProviderId());
        redirectAttrs.addFlashAttribute("flashSuccessKey",
                "system.oidcProviders.form.success.created");
        redirectAttrs.addFlashAttribute("flashSuccessParam", form.getProviderId());
        return "redirect:/system/system-settings/oidc-providers";
    }

    @GetMapping("/{providerId}/edit")
    public String editForm(
            @PathVariable String providerId,
            Model model,
            RedirectAttributes redirectAttrs) {
        Optional<ProviderWithStatus> opt =
                systemOidcProviderService.findDetailByProviderId(providerId);
        if (opt.isEmpty()) {
            redirectAttrs.addFlashAttribute("flashErrorKey",
                    "system.oidcProviders.error.notFound");
            return "redirect:/system/system-settings/oidc-providers";
        }

        if (!model.containsAttribute("oidcProviderEditForm")) {
            SystemOidcProvider p = opt.get().getProvider();
            OidcProviderEditForm form = new OidcProviderEditForm();
            form.setProviderId(p.getProviderId());
            form.setUri(p.getUri());
            form.setDisplayName(p.getDisplayName());
            form.setClientId(p.getClientId());
            model.addAttribute("oidcProviderEditForm", form);
        }
        model.addAttribute("activeMenu", "system-settings");
        model.addAttribute("clientSecretMasked", SECRET_MASK);
        return "system/system-settings/oidc-providers/edit";
    }

    @PostMapping("/{providerId}")
    public String update(
            @PathVariable String providerId,
            @Valid @ModelAttribute("oidcProviderEditForm") OidcProviderEditForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            RedirectAttributes redirectAttrs,
            Model model) {

        if (!providerId.equals(form.getProviderId())) {
            redirectAttrs.addFlashAttribute("flashErrorKey",
                    "system.oidcProviders.error.notFound");
            return "redirect:/system/system-settings/oidc-providers";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "system-settings");
            model.addAttribute("clientSecretMasked", SECRET_MASK);
            return "system/system-settings/oidc-providers/edit";
        }

        Optional<ProviderWithStatus> currentOpt =
                systemOidcProviderService.findDetailByProviderId(providerId);
        if (currentOpt.isEmpty()) {
            redirectAttrs.addFlashAttribute("flashErrorKey",
                    "system.oidcProviders.error.notFound");
            return "redirect:/system/system-settings/oidc-providers";
        }
        String dbUri = currentOpt.get().getProvider().getUri();

        try {
            oidcProviderService.verify(dbUri);
        } catch (OidcConnectionException e) {
            String errorMessage = messageSource.getMessage(
                    "system.oidcProviders.connection.error." + e.getErrorType().name(),
                    null,
                    "Connection failed",
                    locale);
            bindingResult.reject("oidcConnectionError", errorMessage);
            model.addAttribute("activeMenu", "system-settings");
            model.addAttribute("clientSecretMasked", SECRET_MASK);
            return "system/system-settings/oidc-providers/edit";
        }

        try {
            String updatedBy = (principal != null) ? principal.getAccountId() : "system";
            String secretInput = form.getClientSecret();
            String secretToPass = (secretInput != null && !secretInput.isEmpty())
                    ? secretInput : null;

            systemOidcProviderService.updateMutableFields(
                    providerId,
                    form.getDisplayName(),
                    form.getClientId(),
                    secretToPass,
                    updatedBy);
        } catch (OidcProviderException e) {
            String errorMessage = messageSource.getMessage(
                    "system.oidcProviders.form.error." + e.getError().name(),
                    null,
                    "Update failed",
                    locale);
            bindingResult.reject("providerError", errorMessage);
            model.addAttribute("activeMenu", "system-settings");
            model.addAttribute("clientSecretMasked", SECRET_MASK);
            return "system/system-settings/oidc-providers/edit";
        }

        log.info("OIDCプロバイダーを更新しました: providerId={}", providerId);
        redirectAttrs.addFlashAttribute("flashSuccessKey",
                "system.oidcProviders.form.success.updated");
        redirectAttrs.addFlashAttribute("flashSuccessParam", providerId);
        return "redirect:/system/system-settings/oidc-providers/" + providerId;
    }

    /**
     * OIDCプロバイダーを有効化する。
     */
    @PostMapping("/{providerId}/enable")
    public String enable(
            @PathVariable String providerId,
            @Valid @ModelAttribute("statusChangeForm") OidcProviderStatusChangeForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            RedirectAttributes redirectAttrs) {

        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute(
                    "org.springframework.validation.BindingResult.statusChangeForm",
                    bindingResult);
            redirectAttrs.addFlashAttribute("statusChangeForm", form);
            redirectAttrs.addFlashAttribute("openModal", "enable");
            return "redirect:/system/system-settings/oidc-providers/" + providerId;
        }

        try {
            String updatedBy = (principal != null) ? principal.getAccountId() : "system";
            systemOidcProviderService.enable(providerId, form.getReason(), updatedBy);
        } catch (OidcProviderException e) {
            String errorMessage = messageSource.getMessage(
                    "system.oidcProviders.form.error." + e.getError().name(),
                    null,
                    "Enable failed",
                    locale);
            redirectAttrs.addFlashAttribute("flashErrorMessage", errorMessage);
            return "redirect:/system/system-settings/oidc-providers/" + providerId;
        }

        log.info("OIDCプロバイダーを有効化しました: providerId={}", providerId);
        redirectAttrs.addFlashAttribute("flashSuccessKey",
                "system.oidcProviders.statusChange.success.enabled");
        redirectAttrs.addFlashAttribute("flashSuccessParam", providerId);
        return "redirect:/system/system-settings/oidc-providers/" + providerId;
    }

    /**
     * OIDCプロバイダーを無効化する。
     */
    @PostMapping("/{providerId}/disable")
    public String disable(
            @PathVariable String providerId,
            @Valid @ModelAttribute("statusChangeForm") OidcProviderStatusChangeForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserPrincipal principal,
            Locale locale,
            RedirectAttributes redirectAttrs) {

        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute(
                    "org.springframework.validation.BindingResult.statusChangeForm",
                    bindingResult);
            redirectAttrs.addFlashAttribute("statusChangeForm", form);
            redirectAttrs.addFlashAttribute("openModal", "disable");
            return "redirect:/system/system-settings/oidc-providers/" + providerId;
        }

        try {
            String updatedBy = (principal != null) ? principal.getAccountId() : "system";
            systemOidcProviderService.disable(providerId, form.getReason(), updatedBy);
        } catch (OidcProviderException e) {
            String errorMessage = messageSource.getMessage(
                    "system.oidcProviders.form.error." + e.getError().name(),
                    null,
                    "Disable failed",
                    locale);
            redirectAttrs.addFlashAttribute("flashErrorMessage", errorMessage);
            return "redirect:/system/system-settings/oidc-providers/" + providerId;
        }

        log.info("OIDCプロバイダーを無効化しました: providerId={}", providerId);
        redirectAttrs.addFlashAttribute("flashSuccessKey",
                "system.oidcProviders.statusChange.success.disabled");
        redirectAttrs.addFlashAttribute("flashSuccessParam", providerId);
        return "redirect:/system/system-settings/oidc-providers/" + providerId;
    }

    @PostMapping("/test-connection")
    @ResponseBody
    public ResponseEntity<OidcConnectionTestResponse> testConnection(
            @RequestBody OidcConnectionTestRequest request,
            Locale locale) {
        try {
            oidcProviderService.verify(request.getUri());
            return ResponseEntity.ok(OidcConnectionTestResponse.success());
        } catch (OidcConnectionException e) {
            String errorMessage = messageSource.getMessage(
                    "system.oidcProviders.connection.error." + e.getErrorType().name(),
                    null,
                    "Connection failed",
                    locale);
            return ResponseEntity.ok(
                    OidcConnectionTestResponse.failure(e.getErrorType().name(), errorMessage));
        }
    }

    @PostMapping("/test-connection-for-edit")
    @ResponseBody
    public ResponseEntity<OidcConnectionTestResponse> testConnectionForEdit(
            @RequestBody OidcConnectionTestForEditRequest request,
            Locale locale) {
        Optional<ProviderWithStatus> opt =
                systemOidcProviderService.findDetailByProviderId(request.getProviderId());
        if (opt.isEmpty()) {
            String errorMessage = messageSource.getMessage(
                    "system.oidcProviders.error.notFound",
                    null,
                    "Provider not found",
                    locale);
            return ResponseEntity.ok(
                    OidcConnectionTestResponse.failure("PROVIDER_NOT_FOUND", errorMessage));
        }

        String dbUri = opt.get().getProvider().getUri();
        try {
            oidcProviderService.verify(dbUri);
            return ResponseEntity.ok(OidcConnectionTestResponse.success());
        } catch (OidcConnectionException e) {
            String errorMessage = messageSource.getMessage(
                    "system.oidcProviders.connection.error." + e.getErrorType().name(),
                    null,
                    "Connection failed",
                    locale);
            return ResponseEntity.ok(
                    OidcConnectionTestResponse.failure(e.getErrorType().name(), errorMessage));
        }
    }

    private boolean matchesFilter(ProviderWithStatus pws, String filter) {
        if ("all".equalsIgnoreCase(filter)) {
            return true;
        }
        if (pws.getStatus() == null) {
            return false;
        }
        if ("enabled".equalsIgnoreCase(filter)) {
            return pws.getStatus().getStatus() == OidcProviderStatusValue.ENABLED;
        }
        if ("disabled".equalsIgnoreCase(filter)) {
            return pws.getStatus().getStatus() == OidcProviderStatusValue.DISABLED;
        }
        return true;
    }

    private OidcProviderListItem toListItem(ProviderWithStatus pws) {
        SystemOidcProvider p = pws.getProvider();
        SystemOidcProviderStatus s = pws.getStatus();
        OidcProviderStatusValue statusValue =
                (s != null) ? s.getStatus() : OidcProviderStatusValue.DISABLED;
        int count = systemAccountIdentityRepository.countActiveByIss(p.getUri());
        return new OidcProviderListItem(
                p.getProviderId(),
                p.getDisplayName(),
                statusValue,
                count);
    }

    private OidcProviderDetailView toDetailView(ProviderWithStatus pws) {
        SystemOidcProvider p = pws.getProvider();
        SystemOidcProviderStatus s = pws.getStatus();
        OidcProviderStatusValue statusValue =
                (s != null) ? s.getStatus() : OidcProviderStatusValue.DISABLED;
        String reason = (s != null) ? s.getReason() : null;
        int count = systemAccountIdentityRepository.countActiveByIss(p.getUri());
        return new OidcProviderDetailView(
                p.getProviderId(),
                p.getDisplayName(),
                p.getUri(),
                p.getClientId(),
                SECRET_MASK,
                statusValue,
                reason,
                count,
                p.getCreatedAt(),
                p.getCreatedBy());
    }

}