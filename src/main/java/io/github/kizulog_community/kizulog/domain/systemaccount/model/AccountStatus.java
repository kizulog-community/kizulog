package io.github.kizulog_community.kizulog.domain.systemaccount.model;

/**
 * システム管理アカウントの有効性ステータス
 *
 * <p>system_account_status.status カラムに永続化される値を表す。
 * DBには "ACTIVE"等 が文字列として保存される。</p>
 *
 * @author Jun Kobayashi
 */
public enum AccountStatus {

    /** 有効: ログイン・認証が可能 */
    ACTIVE,

    /** 無効: 退職等で利用停止された状態 */
    INACTIVE,

    /** 一時停止中: セキュリティ違反等で一時的に停止された状態 */
    SUSPENDED;

    /**
     * このステータスのアカウントが認証可能かを判定する。
     *
     * <p>現状はACTIVEのみ認証を許可する。INACTIVEやSUSPENDEDは認証不可。</p>
     *
     * @return 認証可能ならtrue
     */
    public boolean isAuthenticatable() {
        return this == ACTIVE;
    }

}
