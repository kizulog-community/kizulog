package io.github.kizulog_community.kizulog.domain.tenantaccount.model;

/**
 * 業務テナントアカウントの有効性ステータス
 *
 * @author Jun Kobayashi
 */
public enum TenantAccountStatusValue {

    /** 有効: ログイン・認証が可能 */
    ACTIVE,

    /** 無効: 退職等で利用停止された状態 */
    INACTIVE,

    /** 一時停止中: セキュリティ違反等で一時的に停止された状態 */
    SUSPENDED;

    /**
     * このステータスのアカウントが認証可能かを判定する。
     *
     * @return 認証可能なら true
     */
    public boolean isAuthenticatable() {
        return this == ACTIVE;
    }

}
