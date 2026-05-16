package io.github.kizulog_community.kizulog.domain.tenant.exception;

/**
 * テナントステータス変更のエラー種別
 *
 * @author Jun Kobayashi
 */
public enum TenantStatusChangeError {

    /** 対象テナントが存在しない */
    TENANT_NOT_FOUND,

    /** 既に同一ステータスのため変更不要 */
    ALREADY_IN_TARGET_STATUS,

    /** 変更理由が不正（null/空/長すぎる） */
    REASON_INVALID,

}
