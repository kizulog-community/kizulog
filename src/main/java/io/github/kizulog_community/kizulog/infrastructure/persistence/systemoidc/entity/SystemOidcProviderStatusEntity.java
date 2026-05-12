package io.github.kizulog_community.kizulog.infrastructure.persistence.systemoidc.entity;

import java.time.OffsetDateTime;

import io.github.kizulog_community.kizulog.domain.systemoidc.model.OidcProviderStatusValue;
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
 * システムOIDCプロバイダーステータスEntity
 *
 * <p>system_oidc_provider_statusテーブルにマップする。</p>
 *
 * @author Jun Kobayashi
 */
@Entity
@Table(name = "system_oidc_provider_status")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SystemOidcProviderStatusEntity {

    @EmbeddedId
    private SystemOidcProviderStatusId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OidcProviderStatusValue status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

}
