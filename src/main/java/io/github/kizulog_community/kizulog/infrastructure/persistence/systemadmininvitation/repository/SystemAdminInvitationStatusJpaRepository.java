package io.github.kizulog_community.kizulog.infrastructure.persistence.systemadmininvitation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.InvitationStatusValue;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemadmininvitation.entity.SystemAdminInvitationStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemadmininvitation.entity.SystemAdminInvitationStatusId;

/**
 * システム管理者招待ステータスJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface SystemAdminInvitationStatusJpaRepository
        extends JpaRepository<SystemAdminInvitationStatusEntity, SystemAdminInvitationStatusId> {

    /**
     * invitation_idで最新バージョンのステータスを取得する。
     */
    @Query("SELECT e FROM SystemAdminInvitationStatusEntity e "
            + "WHERE e.id.invitationId = :invitationId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemAdminInvitationStatusEntity e2 "
            + "    WHERE e2.id.invitationId = :invitationId"
            + ")")
    Optional<SystemAdminInvitationStatusEntity> findLatestByInvitationId(
            @Param("invitationId") String invitationId);

    /**
     * 指定ステータスを持つ全招待IDを取得する（最新バージョンの判定）
     *
     * <p>各invitation_idの最新versionが指定ステータスのものに絞り込んで、
     * invitation_idのリストを返す。</p>
     */
    @Query("SELECT e.id.invitationId FROM SystemAdminInvitationStatusEntity e "
            + "WHERE e.status = :status "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemAdminInvitationStatusEntity e2 "
            + "    WHERE e2.id.invitationId = e.id.invitationId"
            + ")")
    List<String> findInvitationIdsByLatestStatus(
            @Param("status") InvitationStatusValue status);

}
