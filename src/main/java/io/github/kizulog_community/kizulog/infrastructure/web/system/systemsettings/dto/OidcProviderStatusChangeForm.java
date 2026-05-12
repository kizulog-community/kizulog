package io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OIDCプロバイダー状態変更フォームDTO
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
@NoArgsConstructor
public class OidcProviderStatusChangeForm {

    /** 理由 */
    @NotBlank(message = "{system.oidcProviders.statusChange.error.reason.required}")
    @Size(min = 1, max = 500,
            message = "{system.oidcProviders.statusChange.error.reason.size}")
    private String reason;

}
