package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantoidc.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderStatus;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderStatusValue;
import io.github.kizulog_community.kizulog.domain.tenantoidc.port.TenantOidcProviderStatusRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantoidc.entity.TenantOidcProviderStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantoidc.entity.TenantOidcProviderStatusId;
import lombok.RequiredArgsConstructor;

/**
 * 業務テナントOIDCプロバイダーステータスリポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class TenantOidcProviderStatusRepositoryImpl
        implements TenantOidcProviderStatusRepository {

    private final TenantOidcProviderStatusJpaRepository jpaRepository;

    @Override
    public Optional<TenantOidcProviderStatus> findLatestByTenantIdAndProviderId(
            String tenantId, String providerId) {
        return jpaRepository.findLatestByTenantIdAndProviderId(tenantId, providerId)
                .map(this::toDomain);
    }

    @Override
    public List<TenantOidcProviderStatus> findAllByTenantIdAndProviderIdOrderByVersionDesc(
            String tenantId, String providerId) {
        return jpaRepository.findAllByTenantIdAndProviderIdOrderByVersionDesc(
                tenantId, providerId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<TenantOidcProviderStatus> findAllLatestEnabledByTenantId(String tenantId) {
        return jpaRepository.findAllLatestByTenantIdAndStatus(
                tenantId, TenantOidcProviderStatusValue.ENABLED).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void save(TenantOidcProviderStatus status) {
        jpaRepository.save(toEntity(status));
    }

    private TenantOidcProviderStatus toDomain(TenantOidcProviderStatusEntity entity) {
        return new TenantOidcProviderStatus(
                entity.getId().getTenantId(),
                entity.getId().getProviderId(),
                entity.getId().getVersion(),
                entity.getStatus(),
                entity.getReason(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    private TenantOidcProviderStatusEntity toEntity(TenantOidcProviderStatus domain) {
        return new TenantOidcProviderStatusEntity(
                new TenantOidcProviderStatusId(
                        domain.getTenantId(),
                        domain.getProviderId(),
                        domain.getVersion()),
                domain.getStatus(),
                domain.getReason(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
