package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity;

import java.time.OffsetDateTime;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author Jun Kobayashi
 */
@Entity
@Table(name = "system_account_status")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SystemAccountStatusEntity {

    @EmbeddedId
    private SystemAccountStatusId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

}
