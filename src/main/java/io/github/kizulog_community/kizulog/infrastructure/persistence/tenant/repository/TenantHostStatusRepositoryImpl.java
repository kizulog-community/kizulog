package io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostStatus;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostStatusValue;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantHostStatusRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantHostStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantHostStatusId;
import lombok.RequiredArgsConstructor;

/**
 * 業務テナント識別ホストステータスリポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class TenantHostStatusRepositoryImpl implements TenantHostStatusRepository {

    private final TenantHostStatusJpaRepository jpaRepository;

    @Override
    public Optional<TenantHostStatus> findLatestByTenantIdAndHost(
            String tenantId, String host) {
        return jpaRepository.findLatestByTenantIdAndHost(tenantId, host)
                .map(this::toDomain);
    }

    @Override
    public List<TenantHostStatus> findAllByTenantIdAndHostOrderByVersionDesc(
            String tenantId, String host) {
        return jpaRepository
                .findAllByTenantIdAndHostOrderByVersionDesc(tenantId, host)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void save(TenantHostStatus status) {
        jpaRepository.save(toEntity(status));
    }

    /**
     * Entity → ドメインモデル変換
     */
    private TenantHostStatus toDomain(TenantHostStatusEntity entity) {
        return new TenantHostStatus(
                entity.getId().getTenantId(),
                entity.getId().getHost(),
                entity.getId().getVersion(),
                TenantHostStatusValue.valueOf(entity.getStatus()),
                entity.getReason(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    /**
     * ドメインモデル → Entity変換
     */
    private TenantHostStatusEntity toEntity(TenantHostStatus domain) {
        return new TenantHostStatusEntity(
                new TenantHostStatusId(
                        domain.getTenantId(),
                        domain.getHost(),
                        domain.getVersion()),
                domain.getStatus().name(),
                domain.getReason(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
