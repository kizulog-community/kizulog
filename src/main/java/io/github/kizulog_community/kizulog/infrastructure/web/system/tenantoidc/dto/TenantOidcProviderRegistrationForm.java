package io.github.kizulog_community.kizulog.infrastructure.web.system.tenantoidc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 業務テナントOIDCプロバイダー新規登録フォーム
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
@NoArgsConstructor
public class TenantOidcProviderRegistrationForm {

    /** プロバイダー識別子（[a-z0-9-]+ 1-32文字、不変） */
    @NotBlank(message = "{system.tenants.oidc.form.error.providerId.required}")
    @Size(min = 1, max = 32,
            message = "{system.tenants.oidc.form.error.providerId.size}")
    @Pattern(regexp = "^[a-z0-9-]+$",
            message = "{system.tenants.oidc.form.error.providerId.pattern}")
    private String providerId;

    /** 表示名（編集可） */
    @NotBlank(message = "{system.tenants.oidc.form.error.displayName.required}")
    @Size(max = 100,
            message = "{system.tenants.oidc.form.error.displayName.tooLong}")
    private String displayName;

    /** OIDC Issuer URI */
    @NotBlank(message = "{system.tenants.oidc.form.error.iss.required}")
    @Size(max = 500, message = "{system.tenants.oidc.form.error.iss.tooLong}")
    @Pattern(regexp = "^https?://.+$",
            message = "{system.tenants.oidc.form.error.iss.pattern}")
    private String iss;

    /** Audience */
    @NotBlank(message = "{system.tenants.oidc.form.error.aud.required}")
    @Size(max = 255, message = "{system.tenants.oidc.form.error.aud.tooLong}")
    private String aud;

    /** クライアントID（編集可） */
    @NotBlank(message = "{system.tenants.oidc.form.error.clientId.required}")
    @Size(max = 255,
            message = "{system.tenants.oidc.form.error.clientId.tooLong}")
    private String clientId;

    /** クライアントシークレット（編集可、平文入力） */
    @NotBlank(message = "{system.tenants.oidc.form.error.clientSecret.required}")
    private String clientSecret;

    /** 登録理由 */
    @NotBlank(message = "{system.tenants.oidc.form.error.reason.required}")
    @Size(max = 1000,
            message = "{system.tenants.oidc.form.error.reason.tooLong}")
    private String reason;

}
