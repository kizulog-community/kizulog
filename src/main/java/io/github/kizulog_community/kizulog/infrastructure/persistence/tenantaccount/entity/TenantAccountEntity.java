package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 業務テナントアカウントEntity
 *
 * @author Jun Kobayashi
 */
@Entity
@Table(name = "tenant_accounts")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TenantAccountEntity {

    @EmbeddedId
    private TenantAccountId id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

}
