package io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * テナント管理者招待と最新ステータスのペアを表現するオブジェクト
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantInvitationWithStatus {

    /** 招待本体 */
    private final TenantAdminInvitation invitation;

    /** 最新ステータス（存在しない場合は null） */
    private final TenantAdminInvitationStatus status;

}
