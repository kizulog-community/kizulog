package io.github.kizulog_community.kizulog.domain.tenant.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * テナントステータス変更履歴
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantStatusHistoryEntry {

    /** バージョン（変更日時に相当） */
    private final OffsetDateTime version;

    /** ステータス値 */
    private final TenantStatusValue status;

    /** 変更理由 */
    private final String reason;

    /** 作成日時 */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
