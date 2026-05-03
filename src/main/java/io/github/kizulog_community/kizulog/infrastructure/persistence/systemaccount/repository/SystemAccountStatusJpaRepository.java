package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountStatusId;

/**
 * @author Jun Kobayashi
 */
public interface SystemAccountStatusJpaRepository
        extends JpaRepository<SystemAccountStatusEntity, SystemAccountStatusId> {

    // 指定されたaccountIdの最新バージョンを取得
    @Query("""
            SELECT s FROM SystemAccountStatusEntity s
            WHERE s.id.accountId = :accountId
            AND s.id.version = (
                SELECT MAX(s2.id.version)
                FROM SystemAccountStatusEntity s2
                WHERE s2.id.accountId = :accountId
            )
            """)
    Optional<SystemAccountStatusEntity> findLatestByAccountId(
            @Param("accountId") String accountId);

}
