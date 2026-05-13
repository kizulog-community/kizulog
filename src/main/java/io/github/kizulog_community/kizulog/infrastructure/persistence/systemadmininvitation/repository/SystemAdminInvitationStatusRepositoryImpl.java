package io.github.kizulog_community.kizulog.infrastructure.persistence.systemadmininvitation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.InvitationStatusValue;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.SystemAdminInvitationStatus;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.port.SystemAdminInvitationStatusRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemadmininvitation.entity.SystemAdminInvitationStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemadmininvitation.entity.SystemAdminInvitationStatusId;
import lombok.RequiredArgsConstructor;

/**
 * システム管理者招待ステータスリポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class SystemAdminInvitationStatusRepositoryImpl
        implements SystemAdminInvitationStatusRepository {

    private final SystemAdminInvitationStatusJpaRepository jpaRepository;

    @Override
    public Optional<SystemAdminInvitationStatus> findLatestByInvitationId(String invitationId) {
        return jpaRepository.findLatestByInvitationId(invitationId)
                .map(this::toDomain);
    }

    @Override
    public List<String> findInvitationIdsByLatestStatus(InvitationStatusValue status) {
        return jpaRepository.findInvitationIdsByLatestStatus(status);
    }

    @Override
    public void save(SystemAdminInvitationStatus status) {
        jpaRepository.save(toEntity(status));
    }

    private SystemAdminInvitationStatus toDomain(SystemAdminInvitationStatusEntity entity) {
        return new SystemAdminInvitationStatus(
                entity.getId().getInvitationId(),
                entity.getId().getVersion(),
                entity.getStatus(),
                entity.getReason(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    private SystemAdminInvitationStatusEntity toEntity(SystemAdminInvitationStatus domain) {
        return new SystemAdminInvitationStatusEntity(
                new SystemAdminInvitationStatusId(
                        domain.getInvitationId(),
                        domain.getVersion()),
                domain.getStatus(),
                domain.getReason(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
