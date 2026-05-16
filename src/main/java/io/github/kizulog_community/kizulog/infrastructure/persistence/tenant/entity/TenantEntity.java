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
 * 業務テナントEntity
 *
 * @author Jun Kobayashi
 */
@Entity
@Table(name = "tenants")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TenantEntity {

    @EmbeddedId
    private TenantId id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false)
    private String slug;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

}
