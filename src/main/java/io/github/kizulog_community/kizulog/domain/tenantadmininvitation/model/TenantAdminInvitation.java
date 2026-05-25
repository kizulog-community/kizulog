package io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * テナント管理者招待ドメインモデル
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantAdminInvitation {

    /** 招待ID（UUID v4） */
    private final String invitationId;

    /** バージョン */
    private final OffsetDateTime version;

    /** 招待先テナントID */
    private final String tenantId;

    /** トークンのSHA-256ハッシュ（16進64文字） */
    private final String tokenHash;

    /** 有効期限 */
    private final OffsetDateTime expiresAt;

    /** 招待先表示名（管理用ラベル） */
    private final String displayName;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
