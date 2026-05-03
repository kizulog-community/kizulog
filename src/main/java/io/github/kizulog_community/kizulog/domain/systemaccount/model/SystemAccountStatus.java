package io.github.kizulog_community.kizulog.domain.systemaccount.model;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * システム管理アカウントステータスドメインモデル
 *
 * <p>システム管理アカウントの有効/無効状態を表現する。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class SystemAccountStatus {

    /** アカウントID */
    private final String accountId;

    /** バージョン */
    private final OffsetDateTime version;

    /** ステータス（ACTIVE / INACTIVE / SUSPENDED） */
    private final AccountStatus status;

    /** 変更理由（任意） */
    private final String reason;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
