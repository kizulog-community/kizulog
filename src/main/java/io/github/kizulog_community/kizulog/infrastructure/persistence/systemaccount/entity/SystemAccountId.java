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
 * @author jun kobayashi
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SystemAccountId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "version", nullable = false)
    private OffsetDateTime version;

}