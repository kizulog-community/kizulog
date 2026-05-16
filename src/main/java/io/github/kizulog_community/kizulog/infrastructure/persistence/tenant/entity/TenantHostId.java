package io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 業務テナント識別ホストEntityの主キー
 *
 * @author Jun Kobayashi
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TenantHostId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "host", nullable = false)
    private String host;

    @Column(name = "version", nullable = false)
    private OffsetDateTime version;

}
