package io.github.kizulog_community.kizulog.domain.tenant.model;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * テナント詳細画面表示用ビュー
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantDetailView {

    /** テナントID */
    private final String tenantId;

    /** テナント名 */
    private final String name;

    /** URL用slug */
    private final String slug;

    /** テナント作成日時 */
    private final OffsetDateTime createdAt;

    /** テナント作成者 */
    private final String createdBy;

    /** 現在のステータス */
    private final TenantStatusValue currentStatus;

    /** 現在ステータスの理由 */
    private final String currentStatusReason;

    /** 現在ステータスのversion */
    private final OffsetDateTime currentStatusVersion;

    /** 現在ステータスの設定者 */
    private final String currentStatusUpdatedBy;

    /** ステータス変更履歴 */
    private final List<TenantStatusHistoryEntry> statusHistory;

    /** host一覧（現在ACTIVE/INACTIVE両方含む、host単位） */
    private final List<TenantHostView> hosts;

}