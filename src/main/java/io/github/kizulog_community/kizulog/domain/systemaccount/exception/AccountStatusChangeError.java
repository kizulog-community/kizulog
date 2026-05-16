package io.github.kizulog_community.kizulog.domain.systemaccount.exception;

/**
 * システム管理アカウントのステータス変更エラー種別
 *
 * @author Jun Kobayashi
 */
public enum AccountStatusChangeError {

    /** 対象のアカウントが存在しない */
    ACCOUNT_NOT_FOUND,

    /** 自分自身のステータスは変更できない（自爆禁止） */
    CANNOT_CHANGE_SELF,

    /** 既に同一ステータスのため変更不要 */
    ALREADY_IN_TARGET_STATUS,

    /** 最後のACTIVEなシステム管理者を無効化（INACTIVE/SUSPENDED）にしようとした場合 */
    CANNOT_DEACTIVATE_LAST_ACTIVE_ADMIN,

}