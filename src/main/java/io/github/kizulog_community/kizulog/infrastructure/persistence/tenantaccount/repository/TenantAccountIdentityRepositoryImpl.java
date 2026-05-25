package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountIdentity;
import io.github.kizulog_community.kizulog.domain.tenantaccount.port.TenantAccountIdentityRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountIdentityEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountIdentityId;
import lombok.RequiredArgsConstructor;

/**
 * 業務テナントアカウント認証方法リポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class TenantAccountIdentityRepositoryImpl
        implements TenantAccountIdentityRepository {

    private final TenantAccountIdentityJpaRepository jpaRepository;

    @Override
    public Optional<TenantAccountIdentity> findLatestByTenantIdAndIssAndAudAndSub(
            String tenantId, String iss, String aud, String sub) {
        return jpaRepository.findLatestByTenantIdAndIssAndAudAndSub(tenantId, iss, aud, sub)
                .map(this::toDomain);
    }

    @Override
    public Optional<TenantAccountIdentity> findLatestByIdentityId(String identityId) {
        return jpaRepository.findLatestByIdentityId(identityId)
                .map(this::toDomain);
    }

    @Override
    public List<TenantAccountIdentity> findLatestByAccountId(String accountId) {
        return jpaRepository.findLatestByAccountId(accountId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public int countActiveByTenantIdAndIss(String tenantId, String iss) {
        return jpaRepository.countActiveByTenantIdAndIss(tenantId, iss);
    }

    @Override
    public void save(TenantAccountIdentity identity) {
        jpaRepository.save(toEntity(identity));
    }

    private TenantAccountIdentity toDomain(TenantAccountIdentityEntity entity) {
        return new TenantAccountIdentity(
                entity.getId().getIdentityId(),
                entity.getId().getVersion(),
                entity.getAccountId(),
                entity.getTenantId(),
                entity.getIss(),
                entity.getAud(),
                entity.getSub(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    private TenantAccountIdentityEntity toEntity(TenantAccountIdentity domain) {
        return new TenantAccountIdentityEntity(
                new TenantAccountIdentityId(
                        domain.getIdentityId(),
                        domain.getVersion()),
                domain.getAccountId(),
                domain.getTenantId(),
                domain.getIss(),
                domain.getAud(),
                domain.getSub(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
