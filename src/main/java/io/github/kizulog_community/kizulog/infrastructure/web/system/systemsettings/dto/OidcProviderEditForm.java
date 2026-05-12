package io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * OIDCプロバイダー編集用フォームDTO
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "clientSecret")
public class OidcProviderEditForm {

    /** プロバイダーID */
    private String providerId;

    /** OIDC Issuer URI */
    private String uri;

    /** 表示名 */
    @NotBlank(message = "{system.oidcProviders.form.error.displayName.required}")
    @Size(min = 1, max = 100,
            message = "{system.oidcProviders.form.error.displayName.size}")
    private String displayName;

    /** OAuth2 Client ID */
    @NotBlank(message = "{system.oidcProviders.form.error.clientId.required}")
    @Size(max = 255,
            message = "{system.oidcProviders.form.error.clientId.size}")
    private String clientId;

    /** OAuth2 Client Secret */
    @Size(max = 500,
            message = "{system.oidcProviders.form.error.clientSecret.size}")
    private String clientSecret;

}
