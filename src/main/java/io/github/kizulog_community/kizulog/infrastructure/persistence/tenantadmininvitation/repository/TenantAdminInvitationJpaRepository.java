package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantadmininvitation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantadmininvitation.entity.TenantAdminInvitationEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantadmininvitation.entity.TenantAdminInvitationId;

/**
 * テナント管理者招待JPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface TenantAdminInvitationJpaRepository
        extends JpaRepository<TenantAdminInvitationEntity, TenantAdminInvitationId> {

    /**
     * invitation_id で最新バージョンの招待を取得する。
     */
    @Query("SELECT e FROM TenantAdminInvitationEntity e "
            + "WHERE e.id.invitationId = :invitationId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantAdminInvitationEntity e2 "
            + "    WHERE e2.id.invitationId = :invitationId"
            + ")")
    Optional<TenantAdminInvitationEntity> findLatestByInvitationId(
            @Param("invitationId") String invitationId);

    /**
     * token_hash から該当バージョンの招待を取得する。
     */
    @Query("SELECT e FROM TenantAdminInvitationEntity e "
            + "WHERE e.tokenHash = :tokenHash")
    Optional<TenantAdminInvitationEntity> findByTokenHash(
            @Param("tokenHash") String tokenHash);

    /**
     * 指定テナントの全招待の最新バージョンを取得する。
     */
    @Query("SELECT e FROM TenantAdminInvitationEntity e "
            + "WHERE e.tenantId = :tenantId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantAdminInvitationEntity e2 "
            + "    WHERE e2.id.invitationId = e.id.invitationId"
            + ")")
    List<TenantAdminInvitationEntity> findAllLatestByTenantId(
            @Param("tenantId") String tenantId);

}
