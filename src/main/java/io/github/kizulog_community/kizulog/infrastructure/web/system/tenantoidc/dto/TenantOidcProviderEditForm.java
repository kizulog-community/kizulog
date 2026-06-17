package io.github.kizulog_community.kizulog.infrastructure.web.system.tenantoidc.dto;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.kizulog_community.kizulog.domain.tenantoidc.model.ClaimsMappingTarget;
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
    @Size(max = 100, message = "{system.tenants.oidc.form.error.displayName.tooLong}")
    private String displayName;

    /** クライアントID */
    @NotBlank(message = "{system.tenants.oidc.form.error.clientId.required}")
    @Size(max = 255, message = "{system.tenants.oidc.form.error.clientId.tooLong}")
    private String clientId;

    /** クライアントシークレット */
    private String clientSecret;

    /** 変更理由 */
    @NotBlank(message = "{system.tenants.oidc.form.error.reason.required}")
    @Size(max = 1000, message = "{system.tenants.oidc.form.error.reason.tooLong}")
    private String reason;

    /** クレームマッピング設定（key=ClaimsMappingTarget#getKey, value=クレームキー） */
    private Map<String, String> claimsMapping = new LinkedHashMap<>();

    /**
     * 既存のマッピングからフォーム値を設定する（編集画面表示時に使用）。
     *
     * @param existing 既存のマッピング（key=ClaimsMappingTarget#getKey）
     */
    public void populateFromExisting(Map<String, String> existing) {
        if (existing == null) {
            return;
        }
        for (ClaimsMappingTarget target : ClaimsMappingTarget.values()) {
            String value = existing.get(target.getKey());
            if (value != null) {
                claimsMapping.put(target.getKey(), value);
            }
        }
    }

}
