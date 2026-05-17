package io.github.kizulog_community.kizulog.infrastructure.web.system.tenantoidc.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * テナントOIDCプロバイダー接続確認APIリクエスト
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
@NoArgsConstructor
public class TenantOidcConnectionTestRequest {

    /** 接続確認対象の OIDC Issuer URI */
    private String iss;

}
