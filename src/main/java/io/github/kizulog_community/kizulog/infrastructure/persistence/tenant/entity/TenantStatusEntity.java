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
 * 業務テナントステータスEntity
 *
 * @author Jun Kobayashi
 */
@Entity
@Table(name = "tenant_status")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TenantStatusEntity {

    @EmbeddedId
    private TenantStatusId id;

    /** ステータス値 (ACTIVE / INACTIVE / SUSPENDED) を文字列で保持 */
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

}
