package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantattendance.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.domain.tenantattendance.model.PunchType;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantattendance.entity.AttendancePunchEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantattendance.entity.AttendancePunchId;

/**
 * 打刻イベントJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface AttendancePunchJpaRepository
        extends JpaRepository<AttendancePunchEntity, AttendancePunchId> {

    /**
     * 指定アカウント・指定打刻種別における最新（最大）の業務時刻を取得する。
     */
    @Query("""
            SELECT MAX(e.punchedAt) FROM AttendancePunchEntity e
            WHERE e.accountId = :accountId
            AND e.punchType = :punchType
            AND e.id.version = (
                SELECT MAX(e2.id.version) FROM AttendancePunchEntity e2
                WHERE e2.id.punchId = e.id.punchId
            )
            """)
    Optional<OffsetDateTime> findLatestPunchedAtByAccountIdAndType(
            @Param("accountId") String accountId,
            @Param("punchType") PunchType punchType);

    /**
     * 指定アカウントの、指定業務時刻以降（境界含む）の打刻を業務時刻昇順で取得する。
     */
    @Query("""
            SELECT e FROM AttendancePunchEntity e
            WHERE e.accountId = :accountId
            AND e.punchedAt >= :since
            AND e.id.version = (
                SELECT MAX(e2.id.version) FROM AttendancePunchEntity e2
                WHERE e2.id.punchId = e.id.punchId
            )
            ORDER BY e.punchedAt ASC, e.id.version ASC
            """)
    List<AttendancePunchEntity> findByAccountIdSince(
            @Param("accountId") String accountId,
            @Param("since") OffsetDateTime since);

}
