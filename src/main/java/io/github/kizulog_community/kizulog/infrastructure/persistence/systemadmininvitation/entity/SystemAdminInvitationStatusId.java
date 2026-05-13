package io.github.kizulog_community.kizulog.infrastructure.persistence.systemadmininvitation.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * システム管理者招待ステータスの主キー
 *
 * @author Jun Kobayashi
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SystemAdminInvitationStatusId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "invitation_id", nullable = false)
    private String invitationId;

    @Column(name = "version", nullable = false)
    private OffsetDateTime version;

}
