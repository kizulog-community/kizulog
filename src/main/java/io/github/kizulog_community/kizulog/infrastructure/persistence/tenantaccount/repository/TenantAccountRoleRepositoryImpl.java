package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountRole;
import io.github.kizulog_community.kizulog.domain.tenantaccount.port.TenantAccountRoleRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountRoleEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountRoleId;
import lombok.RequiredArgsConstructor;

/**
 * 業務テナントアカウントロールリポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class TenantAccountRoleRepositoryImpl implements TenantAccountRoleRepository {

    private final TenantAccountRoleJpaRepository jpaRepository;

    @Override
    public List<TenantAccountRole> findLatestByAccountId(String accountId) {
        return jpaRepository.findLatestByAccountId(accountId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void save(TenantAccountRole tenantAccountRole) {
        jpaRepository.save(toEntity(tenantAccountRole));
    }

    private TenantAccountRole toDomain(TenantAccountRoleEntity entity) {
        return new TenantAccountRole(
                entity.getId().getRoleId(),
                entity.getId().getVersion(),
                entity.getAccountId(),
                entity.getRole(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    private TenantAccountRoleEntity toEntity(TenantAccountRole domain) {
        return new TenantAccountRoleEntity(
                new TenantAccountRoleId(domain.getRoleId(), domain.getVersion()),
                domain.getAccountId(),
                domain.getRole(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
