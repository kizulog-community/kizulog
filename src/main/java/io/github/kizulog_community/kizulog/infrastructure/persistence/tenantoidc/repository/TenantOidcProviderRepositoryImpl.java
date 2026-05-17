package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantoidc.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProvider;
import io.github.kizulog_community.kizulog.domain.tenantoidc.port.TenantOidcProviderRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantoidc.entity.TenantOidcProviderEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantoidc.entity.TenantOidcProviderId;
import lombok.RequiredArgsConstructor;

/**
 * 業務テナントOIDCプロバイダーリポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class TenantOidcProviderRepositoryImpl
        implements TenantOidcProviderRepository {

    private final TenantOidcProviderJpaRepository jpaRepository;

    @Override
    public Optional<TenantOidcProvider> findLatestByTenantIdAndProviderId(
            String tenantId, String providerId) {
        return jpaRepository.findLatestByTenantIdAndProviderId(tenantId, providerId)
                .map(this::toDomain);
    }

    @Override
    public List<TenantOidcProvider> findAllLatestByTenantId(String tenantId) {
        return jpaRepository.findAllLatestByTenantId(tenantId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<TenantOidcProvider> findAllLatestByIssAndAud(String iss, String aud) {
        return jpaRepository.findAllLatestByIssAndAud(iss, aud).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByTenantIdAndProviderId(String tenantId, String providerId) {
        return jpaRepository.existsByTenantIdAndProviderIdAcrossAllVersions(
                tenantId, providerId);
    }

    @Override
    public void save(TenantOidcProvider provider) {
        jpaRepository.save(toEntity(provider));
    }

    private TenantOidcProvider toDomain(TenantOidcProviderEntity entity) {
        return new TenantOidcProvider(
                entity.getId().getTenantId(),
                entity.getId().getProviderId(),
                entity.getId().getVersion(),
                entity.getDisplayName(),
                entity.getIss(),
                entity.getAud(),
                entity.getClientId(),
                entity.getClientSecret(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    private TenantOidcProviderEntity toEntity(TenantOidcProvider domain) {
        return new TenantOidcProviderEntity(
                new TenantOidcProviderId(
                        domain.getTenantId(),
                        domain.getProviderId(),
                        domain.getVersion()),
                domain.getDisplayName(),
                domain.getIss(),
                domain.getAud(),
                domain.getClientId(),
                domain.getClientSecret(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
