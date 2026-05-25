package io.github.kizulog_community.kizulog.domain.tenantadmininvitation.port;

import java.util.List;
import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantAdminInvitationStatus;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantInvitationStatusValue;

/**
 * テナント管理者招待ステータスリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface TenantAdminInvitationStatusRepository {

    /**
     * invitation_id で最新バージョンのステータスを取得する。
     *
     * @param invitationId 招待ID
     * @return 最新バージョンのステータス
     */
    Optional<TenantAdminInvitationStatus> findLatestByInvitationId(String invitationId);

    /**
     * 指定ステータスの全招待IDを取得する（最新バージョンの判定）
     *
     * @param status 検索対象ステータス
     * @return 該当する invitation_id のリスト
     */
    List<String> findInvitationIdsByLatestStatus(TenantInvitationStatusValue status);

    /**
     * 招待ステータスを保存する。
     *
     * @param status 保存する招待ステータス
     */
    void save(TenantAdminInvitationStatus status);

}
