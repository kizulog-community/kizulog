package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantadmininvitation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantAdminInvitation;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.port.TenantAdminInvitationRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantadmininvitation.entity.TenantAdminInvitationEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantadmininvitation.entity.TenantAdminInvitationId;
import lombok.RequiredArgsConstructor;

/**
 * テナント管理者招待リポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class TenantAdminInvitationRepositoryImpl implements TenantAdminInvitationRepository {

    private final TenantAdminInvitationJpaRepository jpaRepository;

    @Override
    public Optional<TenantAdminInvitation> findLatestByInvitationId(String invitationId) {
        return jpaRepository.findLatestByInvitationId(invitationId)
                .map(this::toDomain);
    }

    @Override
    public Optional<TenantAdminInvitation> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash)
                .map(this::toDomain);
    }

    @Override
    public List<TenantAdminInvitation> findAllLatestByTenantId(String tenantId) {
        return jpaRepository.findAllLatestByTenantId(tenantId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void save(TenantAdminInvitation invitation) {
        jpaRepository.save(toEntity(invitation));
    }

    private TenantAdminInvitation toDomain(TenantAdminInvitationEntity entity) {
        return new TenantAdminInvitation(
                entity.getId().getInvitationId(),
                entity.getId().getVersion(),
                entity.getTenantId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getDisplayName(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    private TenantAdminInvitationEntity toEntity(TenantAdminInvitation domain) {
        return new TenantAdminInvitationEntity(
                new TenantAdminInvitationId(
                        domain.getInvitationId(),
                        domain.getVersion()),
                domain.getTenantId(),
                domain.getTokenHash(),
                domain.getExpiresAt(),
                domain.getDisplayName(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
