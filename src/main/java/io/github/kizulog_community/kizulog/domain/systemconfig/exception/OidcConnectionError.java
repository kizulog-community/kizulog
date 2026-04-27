package io.github.kizulog_community.kizulog.domain.systemconfig.exception;

/**
 * OIDC接続エラー種別
 *
 * <p>OIDCプロバイダーへの接続で発生しうるエラーを定義する。</p>
 *
 * @author Jun Kobayashi
 */
public enum OidcConnectionError {

    /** 入力値エラー（IssuerUriが未入力・不正） */
    INPUT_ERROR,

    /** 接続エラー（ホスト名解決失敗・タイムアウト等） */
    CONNECTION_ERROR,

    /** 不正レスポンス（OIDC仕様に準拠していない） */
    INVALID_RESPONSE,

    /** 予期しないエラー. */
    UNEXPECTED_ERROR

}