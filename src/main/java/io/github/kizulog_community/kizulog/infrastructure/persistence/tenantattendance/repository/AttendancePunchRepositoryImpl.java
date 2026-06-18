package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantattendance.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.tenantattendance.model.AttendancePunch;
import io.github.kizulog_community.kizulog.domain.tenantattendance.model.PunchType;
import io.github.kizulog_community.kizulog.domain.tenantattendance.port.AttendancePunchRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantattendance.entity.AttendancePunchEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantattendance.entity.AttendancePunchId;
import lombok.RequiredArgsConstructor;

/**
 * 打刻イベントリポジトリ実装クラス（アダプター）
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class AttendancePunchRepositoryImpl implements AttendancePunchRepository {

    /** JPAリポジトリ */
    private final AttendancePunchJpaRepository jpaRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(AttendancePunch punch) {
        jpaRepository.save(toEntity(punch));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<OffsetDateTime> findLatestPunchedAtByAccountIdAndType(
            String accountId, PunchType punchType) {
        return jpaRepository.findLatestPunchedAtByAccountIdAndType(accountId, punchType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AttendancePunch> findByAccountIdSince(String accountId, OffsetDateTime since) {
        return jpaRepository.findByAccountIdSince(accountId, since).stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * Entityをドメインモデルにマップする。
     *
     * @param entity Entity
     * @return ドメインモデル
     */
    private AttendancePunch toDomain(AttendancePunchEntity entity) {
        return new AttendancePunch(
                entity.getId().getPunchId(),
                entity.getId().getVersion(),
                entity.getAccountId(),
                entity.getTenantId(),
                entity.getPunchType(),
                entity.getPunchedAt(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    /**
     * ドメインモデルをEntityにマップする。
     *
     * @param punch ドメインモデル
     * @return Entity
     */
    private AttendancePunchEntity toEntity(AttendancePunch punch) {
        return new AttendancePunchEntity(
                new AttendancePunchId(punch.getPunchId(), punch.getVersion()),
                punch.getAccountId(),
                punch.getTenantId(),
                punch.getPunchType(),
                punch.getPunchedAt(),
                punch.getCreatedAt(),
                punch.getCreatedBy());
    }

}
