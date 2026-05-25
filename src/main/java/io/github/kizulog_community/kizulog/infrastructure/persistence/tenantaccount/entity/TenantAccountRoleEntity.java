package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity;

import java.time.OffsetDateTime;

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantRole;
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
 * 業務テナントアカウントロールEntity
 *
 * @author Jun Kobayashi
 */
@Entity
@Table(name = "tenant_account_roles")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TenantAccountRoleEntity {

    @EmbeddedId
    private TenantAccountRoleId id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private TenantRole role;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

}
