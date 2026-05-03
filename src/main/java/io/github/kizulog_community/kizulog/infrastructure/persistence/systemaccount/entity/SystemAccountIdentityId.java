package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * システム管理アカウント認証方法の主キー
 *
 * @author Jun Kobayashi
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SystemAccountIdentityId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "identity_id", nullable = false)
    private String identityId;

    @Column(name = "version", nullable = false)
    private OffsetDateTime version;

}
