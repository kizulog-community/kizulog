package io.github.kizulog_community.kizulog.domain.tenantaccount.port;

import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountIdentityStatus;

/**
 * 業務テナントアカウント認証方法ステータスリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface TenantAccountIdentityStatusRepository {

    /**
     * identity_id で最新バージョンのステータスを取得する。
     *
     * @param identityId アイデンティティID
     * @return 最新バージョンのステータス。存在しない場合は空の Optional
     */
    Optional<TenantAccountIdentityStatus> findLatestByIdentityId(String identityId);

    /**
     * ステータスを保存する。
     *
     * @param status 保存するステータス
     */
    void save(TenantAccountIdentityStatus status);

}
