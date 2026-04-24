package io.github.kizulog_community.kizulog.infrastructure.persistence.systemconfig.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemconfig.entity.SystemConfigEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemconfig.entity.SystemConfigId;

public interface SystemConfigJpaRepository
        extends JpaRepository<SystemConfigEntity, SystemConfigId> {

    // 指定されたkeyの最新バージョン（versionの最大値）を取得
    @Query("""
            SELECT s FROM SystemConfigEntity s
            WHERE s.id.key = :key
            AND s.id.version = (
                SELECT MAX(s2.id.version)
                FROM SystemConfigEntity s2
                WHERE s2.id.key = :key
            )
            """)
    Optional<SystemConfigEntity> findLatestByKey(@Param("key") String key);
}