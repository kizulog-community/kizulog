package io.github.kizulog_community.kizulog.domain.systemoidc.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * システムOIDCプロバイダードメインモデル
 *
 * <p>システム管理者がログインに使用するOIDCプロバイダーの設定を表現する。
 * 1つのKizuLogシステムに複数のOIDCプロバイダーを登録できる。</p>
 *
 * <p>バージョン管理: 同一provider_idの中でversionが
 * 最大のレコードが有効値となる。</p>
 *
 * <p>provider_idは識別子として不変。Issuer URI(uri)も不変。
 * 編集可能な項目は display_name, client_id, client_secret のみ。</p>
 *
 * <p>client_secretは AES 暗号化済みの値が保持される。
 * 暗号化・復号は CryptoPort 経由で行う。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class SystemOidcProvider {

    /** プロバイダーID */
    private final String providerId;

    /** バージョン */
    private final OffsetDateTime version;

    /** 表示名 */
    private final String displayName;

    /** OIDC Issuer URI */
    private final String uri;

    /** OAuth2 Client ID */
    private final String clientId;

    /** OAuth2 Client Secret（AES 暗号化済み） */
    private final String clientSecret;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
