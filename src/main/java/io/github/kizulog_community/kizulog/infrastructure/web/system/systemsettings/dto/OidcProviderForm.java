package io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.kizulog_community.kizulog.domain.systemoidc.model.ClaimsMappingTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * OIDCプロバイダー追加・編集用フォームDTO
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "clientSecret")
public class OidcProviderForm {

    /** プロバイダーID（[a-z0-9-]+ 1-32文字） */
    @NotBlank(message = "{system.oidcProviders.form.error.providerId.required}")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "{system.oidcProviders.form.error.providerId.format}")
    @Size(min = 1, max = 32, message = "{system.oidcProviders.form.error.providerId.size}")
    private String providerId;

    /** 表示名 */
    @NotBlank(message = "{system.oidcProviders.form.error.displayName.required}")
    @Size(min = 1, max = 100, message = "{system.oidcProviders.form.error.displayName.size}")
    private String displayName;

    /** OIDC Issuer URI */
    @NotBlank(message = "{system.oidcProviders.form.error.uri.required}")
    @Size(max = 500, message = "{system.oidcProviders.form.error.uri.size}")
    private String uri;

    /** OAuth2 Client ID */
    @NotBlank(message = "{system.oidcProviders.form.error.clientId.required}")
    @Size(max = 255, message = "{system.oidcProviders.form.error.clientId.size}")
    private String clientId;

    /** OAuth2 Client Secret */
    @NotBlank(message = "{system.oidcProviders.form.error.clientSecret.required}")
    @Size(max = 500, message = "{system.oidcProviders.form.error.clientSecret.size}")
    private String clientSecret;

    /** クレームマッピング設定 */
    private Map<String, String> claimsMapping = new LinkedHashMap<>();

    /**
     * デフォルトマッピングで初期化する（新規画面表示時等で使用）
     */
    public void ensureDefaults() {
        Map<String, String> defaults = ClaimsMappingTarget.defaultMapping();
        for (Map.Entry<String, String> e : defaults.entrySet()) {
            claimsMapping.putIfAbsent(e.getKey(), e.getValue());
        }
    }

}
