package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountRole;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountRoleRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleId;
import lombok.RequiredArgsConstructor;

/**
 * システム管理アカウントロールリポジトリ実装クラス（アダプター）
 *
 * <p>ドメイン層のOutput Port（SystemAccountRoleRepository）の実装クラス。
 * Spring Data JPAを使用してPostgreSQLへのアクセスを提供する。</p>
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class SystemAccountRoleRepositoryImpl implements SystemAccountRoleRepository {

    /** JPAリポジトリ */
    private final SystemAccountRoleJpaRepository jpaRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SystemAccountRole> findLatestByAccountId(String accountId) {
        return jpaRepository.findLatestByAccountId(accountId).stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(SystemAccountRole systemAccountRole) {
        jpaRepository.save(toEntity(systemAccountRole));
    }

    /**
     * Entityをドメインモデルにマップする。
     *
     * @param entity Entity
     * @return ドメインモデル
     */
    private SystemAccountRole toDomain(SystemAccountRoleEntity entity) {
        return new SystemAccountRole(
                entity.getId().getAccountId(),
                entity.getId().getRole(),
                entity.getId().getVersion(),
                entity.getCreatedAt(),
                entity.getCreatedBy()
        );
    }

    /**
     * ドメインモデルをEntityにマップする。
     *
     * @param systemAccountRole ドメインモデル
     * @return Entity
     */
    private SystemAccountRoleEntity toEntity(SystemAccountRole systemAccountRole) {
        return new SystemAccountRoleEntity(
                new SystemAccountRoleId(
                        systemAccountRole.getAccountId(),
                        systemAccountRole.getRole(),
                        systemAccountRole.getVersion()
                ),
                systemAccountRole.getCreatedAt(),
                systemAccountRole.getCreatedBy()
        );
    }

}
