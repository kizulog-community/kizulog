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
 * 業務テナントアカウント認証方法Entity
 *
 * @author Jun Kobayashi
 */
@Entity
@Table(name = "tenant_account_identities")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TenantAccountIdentityEntity {

    @EmbeddedId
    private TenantAccountIdentityId id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "iss", nullable = false)
    private String iss;

    @Column(name = "aud", nullable = false)
    private String aud;

    @Column(name = "sub", nullable = false)
    private String sub;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

}
