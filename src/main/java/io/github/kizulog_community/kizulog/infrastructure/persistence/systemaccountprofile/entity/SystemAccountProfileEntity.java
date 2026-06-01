package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountprofile.entity;

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
 * system_account_profiles テーブルのJPA Entity
 *
 * @author Jun Kobayashi
 */
@Entity
@Table(name = "system_account_profiles")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SystemAccountProfileEntity {

    /** 複合主キー */
    @EmbeddedId
    private SystemAccountProfileId id;

    /** OIDCクレームのスナップショット（JSONB列） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "claims", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> claims;

    /** 作成日時（UTC） */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /** 作成者 */
    @Column(name = "created_by", nullable = false)
    private String createdBy;

}
