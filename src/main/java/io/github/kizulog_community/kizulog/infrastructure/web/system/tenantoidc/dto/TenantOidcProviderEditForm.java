package io.github.kizulog_community.kizulog.infrastructure.web.system.tenantoidc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 業務テナントOIDCプロバイダー編集フォーム
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
@NoArgsConstructor
public class TenantOidcProviderEditForm {

    /** 表示名 */
    @NotBlank(message = "{system.tenants.oidc.form.error.displayName.required}")
    @Size(max = 100,
            message = "{system.tenants.oidc.form.error.displayName.tooLong}")
    private String displayName;

    /** クライアントID */
    @NotBlank(message = "{system.tenants.oidc.form.error.clientId.required}")
    @Size(max = 255,
            message = "{system.tenants.oidc.form.error.clientId.tooLong}")
    private String clientId;

    /** クライアントシークレット */
    private String clientSecret;

    /** 変更理由 */
    @NotBlank(message = "{system.tenants.oidc.form.error.reason.required}")
    @Size(max = 1000,
            message = "{system.tenants.oidc.form.error.reason.tooLong}")
    private String reason;

}
