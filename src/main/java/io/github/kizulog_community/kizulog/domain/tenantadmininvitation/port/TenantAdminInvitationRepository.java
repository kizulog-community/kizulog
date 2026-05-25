package io.github.kizulog_community.kizulog.domain.tenantadmininvitation.port;

import java.util.List;
import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantAdminInvitation;

/**
 * テナント管理者招待リポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface TenantAdminInvitationRepository {

    /**
     * invitation_id で最新バージョンの招待を取得する。
     *
     * @param invitationId 招待ID
     * @return 最新バージョンの招待
     */
    Optional<TenantAdminInvitation> findLatestByInvitationId(String invitationId);

    /**
     * token_hash から該当バージョンの招待を取得する。
     *
     * @param tokenHash トークンのSHA-256ハッシュ（16進64文字）
     * @return 該当する招待
     */
    Optional<TenantAdminInvitation> findByTokenHash(String tokenHash);

    /**
     * 指定テナントの全招待の最新バージョンを取得する。
     *
     * @param tenantId テナントID
     * @return 招待のリスト（最新バージョン、空リスト返却あり）
     */
    List<TenantAdminInvitation> findAllLatestByTenantId(String tenantId);

    /**
     * 招待を保存する。
     *
     * @param invitation 保存する招待
     */
    void save(TenantAdminInvitation invitation);

}
