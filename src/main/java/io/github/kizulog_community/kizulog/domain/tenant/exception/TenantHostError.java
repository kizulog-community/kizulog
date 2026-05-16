package io.github.kizulog_community.kizulog.domain.tenant.exception;

/**
 * テナントホスト操作（追加・無効化・再有効化）のエラー種別
 *
 * @author Jun Kobayashi
 */
public enum TenantHostError {

    /** 対象テナントが存在しない */
    TENANT_NOT_FOUND,

    /** host形式が不正（RFC1123違反、ポート/スキーマ含む等） */
    HOST_INVALID,

    /** 既に同一テナントへ同一hostが登録されている（追加時） */
    HOST_DUPLICATE,

    /** 指定 (tenantId, host) のhost定義が存在しない（無効化/再有効化時） */
    HOST_NOT_FOUND,

    /** 既に同一ステータスのため変更不要（無効化/再有効化時） */
    ALREADY_IN_TARGET_STATUS,

    /** 変更理由が不正（null/空/長すぎる） */
    REASON_INVALID,

}
