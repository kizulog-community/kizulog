package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountIdentityStatus;
import io.github.kizulog_community.kizulog.domain.tenantaccount.port.TenantAccountIdentityStatusRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountIdentityStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountIdentityStatusId;
import lombok.RequiredArgsConstructor;

/**
 * 業務テナントアカウント認証方法ステータスリポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class TenantAccountIdentityStatusRepositoryImpl
        implements TenantAccountIdentityStatusRepository {

    private final TenantAccountIdentityStatusJpaRepository jpaRepository;

    @Override
    public Optional<TenantAccountIdentityStatus> findLatestByIdentityId(String identityId) {
        return jpaRepository.findLatestByIdentityId(identityId)
                .map(this::toDomain);
    }

    @Override
    public void save(TenantAccountIdentityStatus status) {
        jpaRepository.save(toEntity(status));
    }

    private TenantAccountIdentityStatus toDomain(TenantAccountIdentityStatusEntity entity) {
        return new TenantAccountIdentityStatus(
                entity.getId().getIdentityId(),
                entity.getId().getVersion(),
                entity.getStatus(),
                entity.getReason(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    private TenantAccountIdentityStatusEntity toEntity(TenantAccountIdentityStatus domain) {
        return new TenantAccountIdentityStatusEntity(
                new TenantAccountIdentityStatusId(
                        domain.getIdentityId(),
                        domain.getVersion()),
                domain.getStatus(),
                domain.getReason(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
