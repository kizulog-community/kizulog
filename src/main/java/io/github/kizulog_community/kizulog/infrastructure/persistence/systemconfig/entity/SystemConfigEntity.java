package io.github.kizulog_community.kizulog.infrastructure.persistence.systemconfig.entity;

import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "system_config")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigEntity {

    @EmbeddedId
    private SystemConfigId id;

    @Column(name = "value", nullable = false, columnDefinition = "jsonb")
    private String value;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

}