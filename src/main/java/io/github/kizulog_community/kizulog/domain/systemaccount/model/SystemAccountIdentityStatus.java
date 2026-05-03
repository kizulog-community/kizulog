package io.github.kizulog_community.kizulog.domain.systemaccount.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * システム管理アカウント認証方法ステータスドメインモデル
 *
 * <p>SystemAccountIdentityのステータスを履歴管理する。
 * バージョン管理：同一identity_idの中でversionが最大のレコードが有効値となる。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class SystemAccountIdentityStatus {

    /** アイデンティティID */
    private final String identityId;

    /** バージョン */
    private final OffsetDateTime version;

    /** ステータス */
    private final AccountStatus status;

    /** 理由 */
    private final String reason;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
