package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccount;
import io.github.kizulog_community.kizulog.domain.tenantaccount.port.TenantAccountRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountId;
import lombok.RequiredArgsConstructor;

/**
 * 業務テナントアカウントリポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class TenantAccountRepositoryImpl implements TenantAccountRepository {

    private final TenantAccountJpaRepository jpaRepository;

    @Override
    public Optional<TenantAccount> findLatestByAccountId(String accountId) {
        return jpaRepository.findLatestByAccountId(accountId)
                .map(this::toDomain);
    }

    @Override
    public List<TenantAccount> findAllLatestByTenantId(String tenantId) {
        return jpaRepository.findAllLatestByTenantId(tenantId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void save(TenantAccount tenantAccount) {
        jpaRepository.save(toEntity(tenantAccount));
    }

    /**
     * Entity から DomainModel に変換する。
     */
    private TenantAccount toDomain(TenantAccountEntity entity) {
        return new TenantAccount(
                entity.getId().getAccountId(),
                entity.getId().getVersion(),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    /**
     * DomainModel から Entity に変換する。
     */
    private TenantAccountEntity toEntity(TenantAccount domain) {
        return new TenantAccountEntity(
                new TenantAccountId(domain.getAccountId(), domain.getVersion()),
                domain.getTenantId(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
