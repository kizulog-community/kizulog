package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountRoleStatus;
import io.github.kizulog_community.kizulog.domain.tenantaccount.port.TenantAccountRoleStatusRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountRoleStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountRoleStatusId;
import lombok.RequiredArgsConstructor;

/**
 * 業務テナントアカウントロールステータスリポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class TenantAccountRoleStatusRepositoryImpl
        implements TenantAccountRoleStatusRepository {

    private final TenantAccountRoleStatusJpaRepository jpaRepository;

    @Override
    public Optional<TenantAccountRoleStatus> findLatestByRoleId(String roleId) {
        return jpaRepository.findLatestByRoleId(roleId)
                .map(this::toDomain);
    }

    @Override
    public void save(TenantAccountRoleStatus status) {
        jpaRepository.save(toEntity(status));
    }

    private TenantAccountRoleStatus toDomain(TenantAccountRoleStatusEntity entity) {
        return new TenantAccountRoleStatus(
                entity.getId().getRoleId(),
                entity.getId().getVersion(),
                entity.getStatus(),
                entity.getReason(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    private TenantAccountRoleStatusEntity toEntity(TenantAccountRoleStatus domain) {
        return new TenantAccountRoleStatusEntity(
                new TenantAccountRoleStatusId(domain.getRoleId(), domain.getVersion()),
                domain.getStatus(),
                domain.getReason(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
