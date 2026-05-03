package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountRole;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountRoleRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleId;
import lombok.RequiredArgsConstructor;

/**
 * システム管理アカウントロールリポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class SystemAccountRoleRepositoryImpl implements SystemAccountRoleRepository {

    private final SystemAccountRoleJpaRepository jpaRepository;

    @Override
    public List<SystemAccountRole> findLatestByAccountId(String accountId) {
        return jpaRepository.findLatestByAccountId(accountId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void save(SystemAccountRole systemAccountRole) {
        jpaRepository.save(toEntity(systemAccountRole));
    }

    private SystemAccountRole toDomain(SystemAccountRoleEntity entity) {
        return new SystemAccountRole(
                entity.getId().getRoleId(),
                entity.getId().getVersion(),
                entity.getAccountId(),
                entity.getRole(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    private SystemAccountRoleEntity toEntity(SystemAccountRole domain) {
        return new SystemAccountRoleEntity(
                new SystemAccountRoleId(domain.getRoleId(), domain.getVersion()),
                domain.getAccountId(),
                domain.getRole(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
