package io.github.kizulog_community.kizulog.infrastructure.persistence.systemconfig.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemconfig.entity.SystemConfigEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemconfig.entity.SystemConfigId;

public interface SystemConfigJpaRepository
        extends JpaRepository<SystemConfigEntity, SystemConfigId> {

    // 指定されたkeyの最新バージョンを取得
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

    // 指定されたkeyの指定バージョンを取得
    Optional<SystemConfigEntity> findByIdKeyAndIdVersion(String key, OffsetDateTime version);

    // 指定されたkeyの全バージョンを取得
    List<SystemConfigEntity> findByIdKey(String key);
    
}