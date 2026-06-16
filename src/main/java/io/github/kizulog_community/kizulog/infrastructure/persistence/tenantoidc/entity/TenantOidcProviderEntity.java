package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantoidc.entity;

import java.time.OffsetDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 業務テナントOIDCプロバイダーEntity
 *
 * @author Jun Kobayashi
 */
@Entity
@Table(name = "tenant_oidc_providers")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TenantOidcProviderEntity {

    @EmbeddedId
    private TenantOidcProviderId id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "iss", nullable = false)
    private String iss;

    @Column(name = "aud", nullable = false)
    private String aud;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "client_secret", nullable = false)
    private String clientSecret;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "claims_mapping", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> claimsMapping;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

}
