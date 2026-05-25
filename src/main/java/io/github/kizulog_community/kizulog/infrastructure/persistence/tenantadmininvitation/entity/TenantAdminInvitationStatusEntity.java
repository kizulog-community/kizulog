package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantadmininvitation.entity;

import java.time.OffsetDateTime;

import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantInvitationStatusValue;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * テナント管理者招待ステータスEntity
 *
 * @author Jun Kobayashi
 */
@Entity
@Table(name = "tenant_admin_invitation_status")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TenantAdminInvitationStatusEntity {

    @EmbeddedId
    private TenantAdminInvitationStatusId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TenantInvitationStatusValue status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

}
