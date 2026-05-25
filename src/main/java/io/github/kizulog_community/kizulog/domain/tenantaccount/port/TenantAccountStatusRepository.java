package io.github.kizulog_community.kizulog.domain.tenantaccount.port;

import java.util.List;
import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountStatus;

/**
 * 業務テナントアカウントステータスリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface TenantAccountStatusRepository {

    /**
     * account_id で最新バージョンのステータスを取得する。
     *
     * @param accountId アカウントID
     * @return 最新バージョンのステータス。存在しない場合は空の Optional
     */
    Optional<TenantAccountStatus> findLatestByAccountId(String accountId);

    /**
     * account_id に紐付くステータス履歴を全件取得する。
     *
     * @param accountId アカウントID
     * @return 履歴リスト（version 降順、空リスト返却あり）
     */
    List<TenantAccountStatus> findAllByAccountIdOrderByVersionDesc(String accountId);

    /**
     * 業務テナントアカウントステータスを保存する。
     *
     * @param tenantAccountStatus 保存するステータス
     */
    void save(TenantAccountStatus tenantAccountStatus);

}
