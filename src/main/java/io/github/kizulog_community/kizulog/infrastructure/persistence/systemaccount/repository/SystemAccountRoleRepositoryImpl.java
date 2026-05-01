package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import org.springframework.stereotype.Repository;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountRole;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountRoleRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleId;
import lombok.RequiredArgsConstructor;

/**
 * システム管理アカウントロールリポジトリ実装クラス（アダプター）
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
    public void save(SystemAccountRole systemAccountRole) {
        jpaRepository.save(toEntity(systemAccountRole));
    }

    /**
     * ドメインモデルをEntityにマップする.
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