package io.github.kizulog_community.kizulog.infrastructure.persistence.systemoidc.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.systemoidc.model.OidcProviderStatusValue;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProviderStatus;
import io.github.kizulog_community.kizulog.domain.systemoidc.port.SystemOidcProviderStatusRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemoidc.entity.SystemOidcProviderStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemoidc.entity.SystemOidcProviderStatusId;
import lombok.RequiredArgsConstructor;

/**
 * システムOIDCプロバイダーステータスリポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class SystemOidcProviderStatusRepositoryImpl
        implements SystemOidcProviderStatusRepository {

    private final SystemOidcProviderStatusJpaRepository jpaRepository;

    @Override
    public Optional<SystemOidcProviderStatus> findLatestByProviderId(String providerId) {
        return jpaRepository.findLatestByProviderId(providerId)
                .map(this::toDomain);
    }

    @Override
    public List<String> findProviderIdsByLatestStatus(OidcProviderStatusValue status) {
        return jpaRepository.findProviderIdsByLatestStatus(status);
    }

    @Override
    public void save(SystemOidcProviderStatus status) {
        jpaRepository.save(toEntity(status));
    }

    private SystemOidcProviderStatus toDomain(SystemOidcProviderStatusEntity entity) {
        return new SystemOidcProviderStatus(
                entity.getId().getProviderId(),
                entity.getId().getVersion(),
                entity.getStatus(),
                entity.getReason(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    private SystemOidcProviderStatusEntity toEntity(SystemOidcProviderStatus domain) {
        return new SystemOidcProviderStatusEntity(
                new SystemOidcProviderStatusId(
                        domain.getProviderId(),
                        domain.getVersion()),
                domain.getStatus(),
                domain.getReason(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
