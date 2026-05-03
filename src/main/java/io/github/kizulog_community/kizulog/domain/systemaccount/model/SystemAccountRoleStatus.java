package io.github.kizulog_community.kizulog.domain.systemaccount.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * システム管理アカウントロールステータスドメインモデル
 *
 * <p>SystemAccountRoleのステータスを履歴管理する。
 * ロールの有効化/無効化を履歴として残しつつ運用するために使用する。
 * バージョン管理：同一role_idの中でversionが最大のレコードが有効値となる。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class SystemAccountRoleStatus {

    /** ロールID */
    private final String roleId;

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
