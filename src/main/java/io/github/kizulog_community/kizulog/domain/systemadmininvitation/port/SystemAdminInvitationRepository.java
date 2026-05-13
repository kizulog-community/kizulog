package io.github.kizulog_community.kizulog.domain.systemadmininvitation.port;

import java.util.List;
import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.SystemAdminInvitation;

/**
 * システム管理者招待リポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface SystemAdminInvitationRepository {

    /**
     * invitation_idで最新バージョンの招待を取得する。
     *
     * @param invitationId 招待ID
     * @return 最新バージョンの招待
     */
    Optional<SystemAdminInvitation> findLatestByInvitationId(String invitationId);

    /**
     * token_hash から該当バージョンの招待を取得する。
     *
     * <p>token_hash は UNIQUE 制約があるため、該当レコードは最大1件である。
     * バージョンに関わらず、その token_hash を持つレコードを返す。</p>
     *
     * <p>取得した招待が最新バージョンかどうかは、呼び出し側で
     * findLatestByInvitationId() の結果と比較して判定する。</p>
     *
     * @param tokenHash トークンのSHA-256ハッシュ（16進64文字）
     * @return 該当する招待
     */
    Optional<SystemAdminInvitation> findByTokenHash(String tokenHash);

    /**
     * 全招待の最新バージョンを取得する。
     *
     * @return 招待のリスト（最新バージョン）
     */
    List<SystemAdminInvitation> findAllLatest();

    /**
     * 招待を保存する。
     *
     * @param invitation 保存する招待
     */
    void save(SystemAdminInvitation invitation);

}
