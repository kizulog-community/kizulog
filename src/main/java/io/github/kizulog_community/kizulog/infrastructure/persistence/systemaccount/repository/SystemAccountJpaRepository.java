package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountId;

/**
 * @author Jun Kobayashi
 */
public interface SystemAccountJpaRepository
        extends JpaRepository<SystemAccountEntity, SystemAccountId> {

    // 指定された(iss, aud, sub)の最新バージョンを取得
    @Query("""
            SELECT s FROM SystemAccountEntity s
            WHERE s.iss = :iss
            AND s.aud = :aud
            AND s.sub = :sub
            AND s.id.version = (
                SELECT MAX(s2.id.version)
                FROM SystemAccountEntity s2
                WHERE s2.iss = :iss
                AND s2.aud = :aud
                AND s2.sub = :sub
            )
            """)
    Optional<SystemAccountEntity> findLatestByIssAndAudAndSub(
            @Param("iss") String iss,
            @Param("aud") String aud,
            @Param("sub") String sub);

}
