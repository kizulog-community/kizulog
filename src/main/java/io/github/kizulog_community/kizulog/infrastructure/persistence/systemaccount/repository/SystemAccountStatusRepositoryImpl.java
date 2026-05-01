package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import org.springframework.stereotype.Repository;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountStatusRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountStatusId;
import lombok.RequiredArgsConstructor;

/**
 * システム管理アカウントステータスリポジトリ実装クラス（アダプター）
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
    public void save(SystemAccountStatus systemAccountStatus) {
        jpaRepository.save(toEntity(systemAccountStatus));
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