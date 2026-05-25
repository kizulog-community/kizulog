package io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * テナント管理者招待ステータスドメインモデル
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantAdminInvitationStatus {

    /** 招待ID */
    private final String invitationId;

    /** バージョン */
    private final OffsetDateTime version;

    /** ステータス */
    private final TenantInvitationStatusValue status;

    /** 理由 */
    private final String reason;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
