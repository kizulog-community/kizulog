package io.github.kizulog_community.kizulog.domain.tenant.exception;

/**
 * テナント新規登録のエラー種別
 *
 * @author Jun Kobayashi
 */
public enum TenantRegistrationError {

    /** テナント名が不正（null/空/長すぎる） */
    NAME_INVALID,

    /** hostリストが空（最低1つのhostが必要） */
    HOST_EMPTY,

    /** host形式が不正（RFC1123違反、ポート/スキーマ含む等） */
    HOST_INVALID,

    /** host が同一テナントに重複（同時送信内で重複） */
    HOST_DUPLICATE_IN_REQUEST,

    /** 変更理由が不正（null/空/長すぎる） */
    REASON_INVALID,

    /** slug 生成に何度試行しても重複が解消できなかった（実質発生しない） */
    SLUG_GENERATION_FAILED,

}
