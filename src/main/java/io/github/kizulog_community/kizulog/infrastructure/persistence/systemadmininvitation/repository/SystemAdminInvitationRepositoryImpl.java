package io.github.kizulog_community.kizulog.infrastructure.persistence.systemadmininvitation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.SystemAdminInvitation;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.port.SystemAdminInvitationRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemadmininvitation.entity.SystemAdminInvitationEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemadmininvitation.entity.SystemAdminInvitationId;
import lombok.RequiredArgsConstructor;

/**
 * システム管理者招待リポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class SystemAdminInvitationRepositoryImpl implements SystemAdminInvitationRepository {

    private final SystemAdminInvitationJpaRepository jpaRepository;

    @Override
    public Optional<SystemAdminInvitation> findLatestByInvitationId(String invitationId) {
        return jpaRepository.findLatestByInvitationId(invitationId)
                .map(this::toDomain);
    }

    @Override
    public Optional<SystemAdminInvitation> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash)
                .map(this::toDomain);
    }

    @Override
    public List<SystemAdminInvitation> findAllLatest() {
        return jpaRepository.findAllLatest().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void save(SystemAdminInvitation invitation) {
        jpaRepository.save(toEntity(invitation));
    }

    private SystemAdminInvitation toDomain(SystemAdminInvitationEntity entity) {
        return new SystemAdminInvitation(
                entity.getId().getInvitationId(),
                entity.getId().getVersion(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getDisplayName(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    private SystemAdminInvitationEntity toEntity(SystemAdminInvitation domain) {
        return new SystemAdminInvitationEntity(
                new SystemAdminInvitationId(
                        domain.getInvitationId(),
                        domain.getVersion()),
                domain.getTokenHash(),
                domain.getExpiresAt(),
                domain.getDisplayName(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
