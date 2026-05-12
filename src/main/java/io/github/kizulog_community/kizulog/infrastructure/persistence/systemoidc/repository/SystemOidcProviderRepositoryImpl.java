package io.github.kizulog_community.kizulog.infrastructure.persistence.systemoidc.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProvider;
import io.github.kizulog_community.kizulog.domain.systemoidc.port.SystemOidcProviderRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemoidc.entity.SystemOidcProviderEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemoidc.entity.SystemOidcProviderId;
import lombok.RequiredArgsConstructor;

/**
 * システムOIDCプロバイダーリポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class SystemOidcProviderRepositoryImpl implements SystemOidcProviderRepository {

    private final SystemOidcProviderJpaRepository jpaRepository;

    @Override
    public Optional<SystemOidcProvider> findLatestByProviderId(String providerId) {
        return jpaRepository.findLatestByProviderId(providerId)
                .map(this::toDomain);
    }

    @Override
    public List<SystemOidcProvider> findAllLatest() {
        return jpaRepository.findAllLatest().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByProviderId(String providerId) {
        return jpaRepository.existsByProviderIdAcrossAllVersions(providerId);
    }

    @Override
    public boolean existsAny() {
        return jpaRepository.count() > 0;
    }

    @Override
    public void save(SystemOidcProvider provider) {
        jpaRepository.save(toEntity(provider));
    }

    private SystemOidcProvider toDomain(SystemOidcProviderEntity entity) {
        return new SystemOidcProvider(
                entity.getId().getProviderId(),
                entity.getId().getVersion(),
                entity.getDisplayName(),
                entity.getUri(),
                entity.getClientId(),
                entity.getClientSecret(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    private SystemOidcProviderEntity toEntity(SystemOidcProvider domain) {
        return new SystemOidcProviderEntity(
                new SystemOidcProviderId(
                        domain.getProviderId(),
                        domain.getVersion()),
                domain.getDisplayName(),
                domain.getUri(),
                domain.getClientId(),
                domain.getClientSecret(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
