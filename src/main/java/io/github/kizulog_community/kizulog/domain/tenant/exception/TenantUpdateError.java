package io.github.kizulog_community.kizulog.domain.tenant.exception;

/**
 * テナント編集のエラー種別
 *
 * @author Jun Kobayashi
 */
public enum TenantUpdateError {

    /** 対象テナントが存在しない */
    TENANT_NOT_FOUND,

    /** テナント名が不正（null/空/長すぎる） */
    NAME_INVALID,

    /** 変更理由が不正（null/空/長すぎる） */
    REASON_INVALID,

}
