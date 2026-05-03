package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountStatusRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountStatusId;
import lombok.RequiredArgsConstructor;

/**
 * システム管理アカウントステータスリポジトリ実装クラス（アダプター）
 *
 * <p>ドメイン層のOutput Port（SystemAccountStatusRepository）の実装クラス。
 * Spring Data JPAを使用してPostgreSQLへのアクセスを提供する。</p>
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class SystemAccountStatusRepositoryImpl implements SystemAccountStatusRepository {

    /** JPAリポジトリ */
    private final SystemAccountStatusJpaRepository jpaRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<SystemAccountStatus> findLatestByAccountId(String accountId) {
        return jpaRepository.findLatestByAccountId(accountId)
                .map(this::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(SystemAccountStatus systemAccountStatus) {
        jpaRepository.save(toEntity(systemAccountStatus));
    }

    /**
     * Entityをドメインモデルにマップする。
     *
     * @param entity Entity
     * @return ドメインモデル
     */
    private SystemAccountStatus toDomain(SystemAccountStatusEntity entity) {
        return new SystemAccountStatus(
                entity.getId().getAccountId(),
                entity.getId().getVersion(),
                entity.getStatus(),
                entity.getReason(),
                entity.getCreatedAt(),
                entity.getCreatedBy()
        );
    }

    /**
     * ドメインモデルをEntityにマップする。
     *
     * @param systemAccountStatus ドメインモデル
     * @return Entity
     */
    private SystemAccountStatusEntity toEntity(SystemAccountStatus systemAccountStatus) {
        return new SystemAccountStatusEntity(
                new SystemAccountStatusId(
                        systemAccountStatus.getAccountId(),
                        systemAccountStatus.getVersion()
                ),
                systemAccountStatus.getStatus(),
                systemAccountStatus.getReason(),
                systemAccountStatus.getCreatedAt(),
                systemAccountStatus.getCreatedBy()
        );
    }

}
