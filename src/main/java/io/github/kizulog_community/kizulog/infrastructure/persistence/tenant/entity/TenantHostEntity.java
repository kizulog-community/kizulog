package io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 業務テナント識別ホストEntity
 *
 * @author Jun Kobayashi
 */
@Entity
@Table(name = "tenant_hosts")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TenantHostEntity {

    @EmbeddedId
    private TenantHostId id;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

}
