package io.github.kizulog_community.kizulog.domain.systemoidc.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * OIDCプロバイダーと最新ステータスのペアを表現するオブジェクト
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class ProviderWithStatus {

    /** プロバイダー本体 */
    private final SystemOidcProvider provider;

    /** 最新ステータス（存在しない場合は null） */
    private final SystemOidcProviderStatus status;

}
