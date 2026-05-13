package io.github.kizulog_community.kizulog.domain.systemadmininvitation.port;

import java.util.List;
import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.InvitationStatusValue;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.SystemAdminInvitationStatus;

/**
 * システム管理者招待ステータスリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface SystemAdminInvitationStatusRepository {

    /**
     * invitation_idで最新バージョンのステータスを取得する。
     *
     * @param invitationId 招待ID
     * @return 最新バージョンのステータス
     */
    Optional<SystemAdminInvitationStatus> findLatestByInvitationId(String invitationId);

    /**
     * 指定ステータスの全招待IDを取得する（最新バージョンの判定）
     *
     * <p>「PENDING な全招待ID」を取得する用途で使用する。
     * 各invitation_idの最新versionが指定ステータスのものに絞り込む。</p>
     *
     * @param status 検索対象ステータス
     * @return 該当するinvitation_idのリスト
     */
    List<String> findInvitationIdsByLatestStatus(InvitationStatusValue status);

    /**
     * 招待ステータスを保存する。
     *
     * @param status 保存する招待ステータス
     */
    void save(SystemAdminInvitationStatus status);

}
