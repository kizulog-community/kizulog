package io.github.kizulog_community.kizulog.infrastructure.persistence.systemoidc.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * システムOIDCプロバイダーEntity
 *
 * @author Jun Kobayashi
 */
@Entity
@Table(name = "system_oidc_providers")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SystemOidcProviderEntity {

    @EmbeddedId
    private SystemOidcProviderId id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "uri", nullable = false)
    private String uri;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "client_secret", nullable = false)
    private String clientSecret;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

}
