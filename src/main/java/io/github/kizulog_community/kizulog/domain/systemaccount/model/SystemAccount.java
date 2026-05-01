package io.github.kizulog_community.kizulog.domain.systemaccount.model;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * システム管理アカウントドメインモデル
 *
 * <p>システム管理者（master）の認証情報を表現する。
 * iss・aud・subのみ保持し、個人情報は別管理とする。</p>
 *
 * <p>バージョン管理：同一account_idの中でversionが最大のレコードが有効値となる。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class SystemAccount {

    /** アカウントID（UUID） */
    private final String accountId;

    /** バージョン */
    private final OffsetDateTime version;

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