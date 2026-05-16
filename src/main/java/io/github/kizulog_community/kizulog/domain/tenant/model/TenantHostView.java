package io.github.kizulog_community.kizulog.domain.tenant.model;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * テナント詳細画面のhost一覧ビュー
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantHostView {

    /** ホスト名 */
    private final String host;

    /** host追加日時 */
    private final OffsetDateTime addedAt;

    /** host追加者 */
    private final String addedBy;

    /** 現在のステータス */
    private final TenantHostStatusValue currentStatus;

    /** 現在ステータスの理由 */
    private final String currentStatusReason;

    /** 現在ステータスのversion */
    private final OffsetDateTime currentStatusVersion;

    /** 現在ステータスの設定者 */
    private final String currentStatusUpdatedBy;

    /** ステータス変更履歴（新しい順） */
    private final List<TenantHostStatusHistoryEntry> statusHistory;

}
