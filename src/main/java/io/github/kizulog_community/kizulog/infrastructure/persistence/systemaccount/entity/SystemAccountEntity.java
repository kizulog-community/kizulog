package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity;

import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author Jun Kobayashi
 */
@Entity
@Table(name = "system_accounts")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SystemAccountEntity {

    @EmbeddedId
    private SystemAccountId id;

    @Column(name = "iss", nullable = false)
    private String iss;

    @Column(name = "aud", nullable = false)
    private String aud;

    @Column(name = "sub", nullable = false)
    private String sub;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

}