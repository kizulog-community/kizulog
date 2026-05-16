package io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.tenant.model.TenantStatus;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantStatusValue;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantStatusRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantStatusId;
import lombok.RequiredArgsConstructor;

/**
 * 業務テナントステータスリポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class TenantStatusRepositoryImpl implements TenantStatusRepository {

    private final TenantStatusJpaRepository jpaRepository;

    @Override
    public Optional<TenantStatus> findLatestByTenantId(String tenantId) {
        return jpaRepository.findLatestByTenantId(tenantId).map(this::toDomain);
    }

    @Override
    public List<TenantStatus> findAllByTenantIdOrderByVersionDesc(String tenantId) {
        return jpaRepository.findAllByTenantIdOrderByVersionDesc(tenantId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void save(TenantStatus status) {
        jpaRepository.save(toEntity(status));
    }

    /**
     * Entity → ドメインモデル変換
     */
    private TenantStatus toDomain(TenantStatusEntity entity) {
        return new TenantStatus(
                entity.getId().getTenantId(),
                entity.getId().getVersion(),
                TenantStatusValue.valueOf(entity.getStatus()),
                entity.getReason(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    /**
     * ドメインモデル → Entity変換
     */
    private TenantStatusEntity toEntity(TenantStatus domain) {
        return new TenantStatusEntity(
                new TenantStatusId(domain.getTenantId(), domain.getVersion()),
                domain.getStatus().name(),
                domain.getReason(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
