package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantattendance.entity;

import java.time.OffsetDateTime;

import io.github.kizulog_community.kizulog.domain.tenantattendance.model.PunchType;
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
 * tenant_attendance_punches テーブルのJPA Entity
 *
 * @author Jun Kobayashi
 */
@Entity
@Table(name = "tenant_attendance_punches")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AttendancePunchEntity {

    @EmbeddedId
    private AttendancePunchId id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "punch_type", nullable = false)
    private PunchType punchType;

    @Column(name = "punched_at", nullable = false)
    private OffsetDateTime punchedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

}
