package io.github.kizulog_community.kizulog.infrastructure.web.system.tenantadmininvitation.dto;

import java.time.OffsetDateTime;

import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantInvitationStatusValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * テナント管理者招待一覧の1行を表示するDTO
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantAdminInvitationListItem {

    /** 招待ID */
    private final String invitationId;

    /** 表示名 */
    private final String displayName;

    /** ステータス */
    private final TenantInvitationStatusValue status;

    /** 有効期限切れフラグ */
    private final boolean expired;

    /** 作成日時 */
    private final OffsetDateTime createdAt;

    /** 有効期限 */
    private final OffsetDateTime expiresAt;

}
