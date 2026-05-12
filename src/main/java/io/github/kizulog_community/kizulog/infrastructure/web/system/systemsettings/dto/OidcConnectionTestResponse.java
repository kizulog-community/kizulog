package io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OIDC接続確認APIレスポンス
 *
 * <p>接続確認の結果をJSONでフロントに返却する不変オブジェクト。
 * 成功時は success=true、失敗時は success=false + 翻訳済みエラーメッセージ。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@RequiredArgsConstructor
public final class OidcConnectionTestResponse {

    /** 接続確認に成功したかどうか */
    private final boolean success;

    /** エラーコード（失敗時のみ） */
    private final String errorCode;

    /** エラーメッセージ（失敗時のみ） */
    private final String errorMessage;

    /**
     * 成功レスポンスを生成する
     */
    public static OidcConnectionTestResponse success() {
        return new OidcConnectionTestResponse(true, null, null);
    }

    /**
     * 失敗レスポンスを生成する
     */
    public static OidcConnectionTestResponse failure(String errorCode, String errorMessage) {
        return new OidcConnectionTestResponse(false, errorCode, errorMessage);
    }

}
