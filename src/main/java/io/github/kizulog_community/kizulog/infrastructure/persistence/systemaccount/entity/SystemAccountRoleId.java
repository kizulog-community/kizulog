package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemRole;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author Jun Kobayashi
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SystemAccountRoleId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private SystemRole role;

    @Column(name = "version", nullable = false)
    private OffsetDateTime version;

}
