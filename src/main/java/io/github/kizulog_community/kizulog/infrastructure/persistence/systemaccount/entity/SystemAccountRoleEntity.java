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
@Table(name = "system_account_roles")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SystemAccountRoleEntity {

    @EmbeddedId
    private SystemAccountRoleId id;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

}