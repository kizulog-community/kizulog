package io.github.kizulog_community.kizulog.infrastructure.persistence.systemadmininvitation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.systemadmininvitation.entity.SystemAdminInvitationEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemadmininvitation.entity.SystemAdminInvitationId;

/**
 * システム管理者招待JPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface SystemAdminInvitationJpaRepository
        extends JpaRepository<SystemAdminInvitationEntity, SystemAdminInvitationId> {

    /**
     * invitation_idで最新バージョンの招待を取得する。
     *
     * <p>同一invitation_idを持つレコードの中で、versionが最大のものを返す。</p>
     */
    @Query("SELECT e FROM SystemAdminInvitationEntity e "
            + "WHERE e.id.invitationId = :invitationId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemAdminInvitationEntity e2 "
            + "    WHERE e2.id.invitationId = :invitationId"
            + ")")
    Optional<SystemAdminInvitationEntity> findLatestByInvitationId(
            @Param("invitationId") String invitationId);

    /**
     * token_hash から該当バージョンの招待を取得する。
     *
     * <p>token_hash は UNIQUE 制約があるため、該当レコードは最大1件。
     * バージョン違いでも token_hash は常に異なる設計のため、
     * 最新版かどうかの判定は呼び出し側で行う。</p>
     */
    @Query("SELECT e FROM SystemAdminInvitationEntity e "
            + "WHERE e.tokenHash = :tokenHash")
    Optional<SystemAdminInvitationEntity> findByTokenHash(
            @Param("tokenHash") String tokenHash);

    /**
     * 全招待の最新バージョンを取得する。
     *
     * <p>各invitation_idの最新versionのレコードを返す。</p>
     */
    @Query("SELECT e FROM SystemAdminInvitationEntity e "
            + "WHERE e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemAdminInvitationEntity e2 "
            + "    WHERE e2.id.invitationId = e.id.invitationId"
            + ")")
    List<SystemAdminInvitationEntity> findAllLatest();

}
