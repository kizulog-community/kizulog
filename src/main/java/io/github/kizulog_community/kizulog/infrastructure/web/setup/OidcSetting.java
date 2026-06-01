package io.github.kizulog_community.kizulog.infrastructure.web.setup;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.kizulog_community.kizulog.domain.systemoidc.model.ClaimsMappingTarget;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * OIDC設定データ
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
public class OidcSetting implements Serializable {

    private static final long serialVersionUID = 1L;

    /** OIDC識別子（必須） */
    private String id;

    /** Issuer URI（必須） */
    @NotBlank(message = "{setup.step2.error.uri.required}")
    private String uri;

    /** Client ID（必須） */
    @NotBlank(message = "{setup.step2.error.clientId.required}")
    private String clientId;

    /** Client Secret（必須） */
    @NotBlank(message = "{setup.step2.error.clientSecret.required}")
    private String clientSecret;

    /** クレームマッピング設定 */
    private Map<String, String> claimsMapping =
            new LinkedHashMap<>(ClaimsMappingTarget.defaultMapping());

}
