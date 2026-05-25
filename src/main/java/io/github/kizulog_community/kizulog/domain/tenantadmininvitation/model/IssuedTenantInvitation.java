package io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 発行されたテナント管理者招待情報を表現するオブジェクト
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class IssuedTenantInvitation {

    /** 招待ID */
    private final String invitationId;

    /** 平文トークン */
    private final String plainToken;

    /** 有効期限 */
    private final OffsetDateTime expiresAt;

}
