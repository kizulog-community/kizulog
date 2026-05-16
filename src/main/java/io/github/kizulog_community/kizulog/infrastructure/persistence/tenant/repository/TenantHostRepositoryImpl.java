package io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHost;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantHostRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantHostEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantHostId;
import lombok.RequiredArgsConstructor;

/**
 * 業務テナント識別ホストリポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class TenantHostRepositoryImpl implements TenantHostRepository {

    private final TenantHostJpaRepository jpaRepository;

    @Override
    public Optional<TenantHost> findLatestByTenantIdAndHost(String tenantId, String host) {
        return jpaRepository.findLatestByTenantIdAndHost(tenantId, host)
                .map(this::toDomain);
    }

    @Override
    public List<TenantHost> findAllLatestByTenantId(String tenantId) {
        return jpaRepository.findAllLatestByTenantId(tenantId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<TenantHost> findAllLatestByHost(String host) {
        return jpaRepository.findAllLatestByHost(host).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByTenantIdAndHost(String tenantId, String host) {
        return jpaRepository.existsByTenantIdAndHostAcrossAllVersions(tenantId, host);
    }

    @Override
    public void save(TenantHost tenantHost) {
        jpaRepository.save(toEntity(tenantHost));
    }

    /**
     * Entity → ドメインモデル変換
     */
    private TenantHost toDomain(TenantHostEntity entity) {
        return new TenantHost(
                entity.getId().getTenantId(),
                entity.getId().getHost(),
                entity.getId().getVersion(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    /**
     * ドメインモデル → Entity変換
     */
    private TenantHostEntity toEntity(TenantHost domain) {
        return new TenantHostEntity(
                new TenantHostId(
                        domain.getTenantId(),
                        domain.getHost(),
                        domain.getVersion()),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
