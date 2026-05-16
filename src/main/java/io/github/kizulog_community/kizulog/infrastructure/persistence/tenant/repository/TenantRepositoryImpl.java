package io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.tenant.model.Tenant;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantId;
import lombok.RequiredArgsConstructor;

/**
 * 業務テナントリポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class TenantRepositoryImpl implements TenantRepository {

    private final TenantJpaRepository jpaRepository;

    @Override
    public Optional<Tenant> findLatestByTenantId(String tenantId) {
        return jpaRepository.findLatestByTenantId(tenantId).map(this::toDomain);
    }

    @Override
    public Optional<Tenant> findLatestBySlug(String slug) {
        return jpaRepository.findLatestBySlug(slug).map(this::toDomain);
    }

    @Override
    public List<Tenant> findAllLatest() {
        return jpaRepository.findAllLatest().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsBySlug(String slug) {
        return jpaRepository.existsBySlugAcrossAllVersions(slug);
    }

    @Override
    public void save(Tenant tenant) {
        jpaRepository.save(toEntity(tenant));
    }

    /**
     * Entity → ドメインモデル変換
     */
    private Tenant toDomain(TenantEntity entity) {
        return new Tenant(
                entity.getId().getTenantId(),
                entity.getId().getVersion(),
                entity.getName(),
                entity.getSlug(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    /**
     * ドメインモデル → Entity変換
     */
    private TenantEntity toEntity(Tenant domain) {
        return new TenantEntity(
                new TenantId(domain.getTenantId(), domain.getVersion()),
                domain.getName(),
                domain.getSlug(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
