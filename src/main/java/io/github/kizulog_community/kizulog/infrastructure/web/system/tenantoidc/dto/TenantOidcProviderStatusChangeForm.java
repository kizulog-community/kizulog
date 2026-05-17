package io.github.kizulog_community.kizulog.infrastructure.web.system.tenantoidc.dto;

import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderStatusValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 業務テナントOIDCプロバイダー ステータス変更フォーム
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
@NoArgsConstructor
public class TenantOidcProviderStatusChangeForm {

    /** 変更先ステータス */
    @NotNull(message = "{system.tenants.oidc.form.error.status.required}")
    private TenantOidcProviderStatusValue targetStatus;

    /** 変更理由 */
    @NotBlank(message = "{system.tenants.oidc.form.error.reason.required}")
    @Size(max = 1000,
            message = "{system.tenants.oidc.form.error.reason.tooLong}")
    private String reason;

}
