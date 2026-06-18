package io.github.kizulog_community.kizulog.domain.shared;

/**
 * テナント利用者の実効タイムゾーンを解決する抽象クラス
 *
 * <p>打刻などの業務時刻（UTC保存）を画面表示する際の表示タイムゾーンを解決する。
 * 解決結果はドメインの値オブジェクト SupportedTimezone で返す。</p>
 *
 * @author Jun Kobayashi
 */
public interface TenantTimezoneResolver {

    /**
     * 指定テナント・指定アカウントの実効タイムゾーンを解決する。
     *
     * @param tenantId テナントID
     * @param accountId アカウントID
     * @return 実効タイムゾーン（必ず非null）
     */
    SupportedTimezone resolve(String tenantId, String accountId);

}
