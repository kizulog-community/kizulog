package io.github.kizulog_community.kizulog.infrastructure.web.setup;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Step2フォームデータ
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
public class Step2FormData {

    /** ホスト名 */
    @NotBlank(message = "{setup.step2.error.host.required}")
    private String host;

    /** OIDC設定 */
    @Valid
    private OidcSetting oidcSetting = new OidcSetting();

}