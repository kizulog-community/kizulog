package io.github.kizulog_community.kizulog.infrastructure.web.system.tenantadmininvitation.dto;

import java.time.OffsetDateTime;

import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantInvitationStatusValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * テナント管理者招待詳細を表示するDTO
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantAdminInvitationDetailView {

    /** 招待ID */
    private final String invitationId;

    /** 表示名 */
    private final String displayName;

    /** ステータス */
    private final TenantInvitationStatusValue status;

    /** 有効期限切れフラグ */
    private final boolean expired;

    /** 取消可能フラグ */
    private final boolean cancellable;

    /** 理由 */
    private final String reason;

    /** 作成日時 */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

    /** 有効期限 */
    private final OffsetDateTime expiresAt;

    /** ステータス最終更新日時 */
    private final OffsetDateTime statusUpdatedAt;

    /** ステータス最終更新者 */
    private final String statusUpdatedBy;

}
