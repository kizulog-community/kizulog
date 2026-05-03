package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentityStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityStatusRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountIdentityStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountIdentityStatusId;
import lombok.RequiredArgsConstructor;

/**
 * システム管理アカウント認証方法ステータスリポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class SystemAccountIdentityStatusRepositoryImpl
        implements SystemAccountIdentityStatusRepository {

    private final SystemAccountIdentityStatusJpaRepository jpaRepository;

    @Override
    public Optional<SystemAccountIdentityStatus> findLatestByIdentityId(String identityId) {
        return jpaRepository.findLatestByIdentityId(identityId)
                .map(this::toDomain);
    }

    @Override
    public void save(SystemAccountIdentityStatus status) {
        jpaRepository.save(toEntity(status));
    }

    private SystemAccountIdentityStatus toDomain(SystemAccountIdentityStatusEntity entity) {
        return new SystemAccountIdentityStatus(
                entity.getId().getIdentityId(),
                entity.getId().getVersion(),
                entity.getStatus(),
                entity.getReason(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    private SystemAccountIdentityStatusEntity toEntity(SystemAccountIdentityStatus domain) {
        return new SystemAccountIdentityStatusEntity(
                new SystemAccountIdentityStatusId(
                        domain.getIdentityId(),
                        domain.getVersion()),
                domain.getStatus(),
                domain.getReason(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
