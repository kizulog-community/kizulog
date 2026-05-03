package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountRoleStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountRoleStatusRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleStatusId;
import lombok.RequiredArgsConstructor;

/**
 * システム管理アカウントロールステータスリポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class SystemAccountRoleStatusRepositoryImpl
        implements SystemAccountRoleStatusRepository {

    private final SystemAccountRoleStatusJpaRepository jpaRepository;

    @Override
    public Optional<SystemAccountRoleStatus> findLatestByRoleId(String roleId) {
        return jpaRepository.findLatestByRoleId(roleId)
                .map(this::toDomain);
    }

    @Override
    public void save(SystemAccountRoleStatus status) {
        jpaRepository.save(toEntity(status));
    }

    private SystemAccountRoleStatus toDomain(SystemAccountRoleStatusEntity entity) {
        return new SystemAccountRoleStatus(
                entity.getId().getRoleId(),
                entity.getId().getVersion(),
                entity.getStatus(),
                entity.getReason(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    private SystemAccountRoleStatusEntity toEntity(SystemAccountRoleStatus domain) {
        return new SystemAccountRoleStatusEntity(
                new SystemAccountRoleStatusId(domain.getRoleId(), domain.getVersion()),
                domain.getStatus(),
                domain.getReason(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
