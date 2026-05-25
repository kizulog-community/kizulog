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
 * 業務テナントアカウントロールステータスEntity
 *
 * @author Jun Kobayashi
 */
@Entity
@Table(name = "tenant_account_role_status")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TenantAccountRoleStatusEntity {

    @EmbeddedId
    private TenantAccountRoleStatusId id;

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
