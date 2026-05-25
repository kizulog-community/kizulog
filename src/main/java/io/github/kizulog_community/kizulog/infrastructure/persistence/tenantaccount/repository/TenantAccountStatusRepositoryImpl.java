package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountStatus;
import io.github.kizulog_community.kizulog.domain.tenantaccount.port.TenantAccountStatusRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountStatusId;
import lombok.RequiredArgsConstructor;

/**
 * 業務テナントアカウントステータスリポジトリ実装クラス（アダプター）
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class TenantAccountStatusRepositoryImpl implements TenantAccountStatusRepository {

    /** JPAリポジトリ */
    private final TenantAccountStatusJpaRepository jpaRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<TenantAccountStatus> findLatestByAccountId(String accountId) {
        return jpaRepository.findLatestByAccountId(accountId)
                .map(this::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TenantAccountStatus> findAllByAccountIdOrderByVersionDesc(String accountId) {
        return jpaRepository.findAllByAccountIdOrderByVersionDesc(accountId).stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(TenantAccountStatus tenantAccountStatus) {
        jpaRepository.save(toEntity(tenantAccountStatus));
    }

    /**
     * Entity をドメインモデルにマップする。
     */
    private TenantAccountStatus toDomain(TenantAccountStatusEntity entity) {
        return new TenantAccountStatus(
                entity.getId().getAccountId(),
                entity.getId().getVersion(),
                entity.getStatus(),
                entity.getReason(),
                entity.getCreatedAt(),
                entity.getCreatedBy()
        );
    }

    /**
     * ドメインモデルを Entity にマップする。
     */
    private TenantAccountStatusEntity toEntity(TenantAccountStatus tenantAccountStatus) {
        return new TenantAccountStatusEntity(
                new TenantAccountStatusId(
                        tenantAccountStatus.getAccountId(),
                        tenantAccountStatus.getVersion()
                ),
                tenantAccountStatus.getStatus(),
                tenantAccountStatus.getReason(),
                tenantAccountStatus.getCreatedAt(),
                tenantAccountStatus.getCreatedBy()
        );
    }

}
