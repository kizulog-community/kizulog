package io.github.kizulog_community.kizulog.domain.systemaccount.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * システム管理アカウント認証方法ドメインモデル
 *
 * <p>システム管理者の認証手段（OIDC接続情報）を表現する。
 * 同一のユーザーが複数のOIDCプロバイダーでログインできる。
 * バージョン管理：同一identity_idの中でversionが
 * 最大のレコードが有効値となる。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class SystemAccountIdentity {

    /** アイデンティティID（UUID） */
    private final String identityId;

    /** バージョン */
    private final OffsetDateTime version;

    /** 紐付くアカウントID */
    private final String accountId;

    /** OIDC Issuer URI */
    private final String iss;

    /** OIDC Client ID */
    private final String aud;

    /** OIDC Subject（プロバイダー内で一意のユーザーID） */
    private final String sub;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
