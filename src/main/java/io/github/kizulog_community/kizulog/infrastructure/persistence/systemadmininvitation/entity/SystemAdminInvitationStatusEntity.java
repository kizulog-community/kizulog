package io.github.kizulog_community.kizulog.infrastructure.persistence.systemadmininvitation.entity;

import java.time.OffsetDateTime;

import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.InvitationStatusValue;
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
 * システム管理者招待ステータスEntity
 *
 * @author Jun Kobayashi
 */
@Entity
@Table(name = "system_admin_invitation_status")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SystemAdminInvitationStatusEntity {

    @EmbeddedId
    private SystemAdminInvitationStatusId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvitationStatusValue status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

}
