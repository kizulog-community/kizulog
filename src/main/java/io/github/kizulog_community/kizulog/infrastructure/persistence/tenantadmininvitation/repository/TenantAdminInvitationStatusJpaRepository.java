package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantadmininvitation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantInvitationStatusValue;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantadmininvitation.entity.TenantAdminInvitationStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantadmininvitation.entity.TenantAdminInvitationStatusId;

/**
 * テナント管理者招待ステータスJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface TenantAdminInvitationStatusJpaRepository
        extends JpaRepository<TenantAdminInvitationStatusEntity, TenantAdminInvitationStatusId> {

    /**
     * invitation_id で最新バージョンのステータスを取得する。
     */
    @Query("SELECT e FROM TenantAdminInvitationStatusEntity e "
            + "WHERE e.id.invitationId = :invitationId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantAdminInvitationStatusEntity e2 "
            + "    WHERE e2.id.invitationId = :invitationId"
            + ")")
    Optional<TenantAdminInvitationStatusEntity> findLatestByInvitationId(
            @Param("invitationId") String invitationId);

    /**
     * 指定ステータスを持つ全招待IDを取得する（最新バージョンの判定）
     */
    @Query("SELECT e.id.invitationId FROM TenantAdminInvitationStatusEntity e "
            + "WHERE e.status = :status "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantAdminInvitationStatusEntity e2 "
            + "    WHERE e2.id.invitationId = e.id.invitationId"
            + ")")
    List<String> findInvitationIdsByLatestStatus(
            @Param("status") TenantInvitationStatusValue status);

}
