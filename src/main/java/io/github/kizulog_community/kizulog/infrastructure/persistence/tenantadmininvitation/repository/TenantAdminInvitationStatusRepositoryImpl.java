package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantadmininvitation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantAdminInvitationStatus;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantInvitationStatusValue;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.port.TenantAdminInvitationStatusRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantadmininvitation.entity.TenantAdminInvitationStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantadmininvitation.entity.TenantAdminInvitationStatusId;
import lombok.RequiredArgsConstructor;

/**
 * テナント管理者招待ステータスリポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class TenantAdminInvitationStatusRepositoryImpl
        implements TenantAdminInvitationStatusRepository {

    private final TenantAdminInvitationStatusJpaRepository jpaRepository;

    @Override
    public Optional<TenantAdminInvitationStatus> findLatestByInvitationId(String invitationId) {
        return jpaRepository.findLatestByInvitationId(invitationId)
                .map(this::toDomain);
    }

    @Override
    public List<String> findInvitationIdsByLatestStatus(TenantInvitationStatusValue status) {
        return jpaRepository.findInvitationIdsByLatestStatus(status);
    }

    @Override
    public void save(TenantAdminInvitationStatus status) {
        jpaRepository.save(toEntity(status));
    }

    private TenantAdminInvitationStatus toDomain(TenantAdminInvitationStatusEntity entity) {
        return new TenantAdminInvitationStatus(
                entity.getId().getInvitationId(),
                entity.getId().getVersion(),
                entity.getStatus(),
                entity.getReason(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    private TenantAdminInvitationStatusEntity toEntity(TenantAdminInvitationStatus domain) {
        return new TenantAdminInvitationStatusEntity(
                new TenantAdminInvitationStatusId(
                        domain.getInvitationId(),
                        domain.getVersion()),
                domain.getStatus(),
                domain.getReason(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
