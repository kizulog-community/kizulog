package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountlocalization.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * system_account_localization テーブルの主キー
 *
 * @author Jun Kobayashi
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SystemAccountLocalizationId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "version", nullable = false)
    private OffsetDateTime version;

}
