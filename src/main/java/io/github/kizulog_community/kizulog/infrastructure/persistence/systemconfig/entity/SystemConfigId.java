package io.github.kizulog_community.kizulog.infrastructure.persistence.systemconfig.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SystemConfigId implements Serializable {

    private static final long serialVersionUID = 1L;

@Column(name = "key", nullable = false)
    private String key;

    @Column(name = "version", nullable = false)
    private OffsetDateTime version;
    
}
