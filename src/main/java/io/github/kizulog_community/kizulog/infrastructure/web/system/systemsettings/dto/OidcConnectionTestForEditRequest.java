package io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OIDC接続確認APIリクエスト
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
@NoArgsConstructor
public class OidcConnectionTestForEditRequest {

    /** プロバイダーID */
    private String providerId;

}
