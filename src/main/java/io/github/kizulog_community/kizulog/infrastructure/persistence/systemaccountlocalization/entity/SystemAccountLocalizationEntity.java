package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountlocalization.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * system_account_localization テーブルのJPA Entity
 *
 * @author Jun Kobayashi
 */
@Entity
@Table(name = "system_account_localization")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SystemAccountLocalizationEntity {

    @EmbeddedId
    private SystemAccountLocalizationId id;

    @Column(name = "language_code", nullable = false)
    private String languageCode;

    @Column(name = "timezone_id", nullable = false)
    private String timezoneId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

}
