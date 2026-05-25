package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity;

import java.time.OffsetDateTime;

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountStatusValue;
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
 * 業務テナントアカウントステータスEntity
 *
 * @author Jun Kobayashi
 */
@Entity
@Table(name = "tenant_account_status")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TenantAccountStatusEntity {

    @EmbeddedId
    private TenantAccountStatusId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TenantAccountStatusValue status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

}
