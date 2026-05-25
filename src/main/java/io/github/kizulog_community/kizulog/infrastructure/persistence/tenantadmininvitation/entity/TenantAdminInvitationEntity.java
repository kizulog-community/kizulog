package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantadmininvitation.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * テナント管理者招待Entity
 *
 * @author Jun Kobayashi
 */
@Entity
@Table(name = "tenant_admin_invitations")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TenantAdminInvitationEntity {

    @EmbeddedId
    private TenantAdminInvitationId id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

}
