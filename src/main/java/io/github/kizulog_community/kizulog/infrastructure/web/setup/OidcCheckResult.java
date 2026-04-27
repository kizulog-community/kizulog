package io.github.kizulog_community.kizulog.infrastructure.web.setup;

import lombok.Getter;
import lombok.Setter;

/**
 * OIDC接続確認結果
 *
 * <p>セットアップウィザードのOIDC接続確認APIのレスポンスを表現する。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
public class OidcCheckResult {

    /** 接続確認成功フラグ */
    private boolean success;

    /** エラー種別（成功時はnull） */
    private String errorType;

    /** 結果メッセージ */
    private String message;

    /**
     * コンストラクタ
     *
     * @param success 接続確認成功フラグ
     * @param errorType エラー種別（成功時はnull）
     * @param message 結果メッセージ
     */
    public OidcCheckResult(boolean success, String errorType, String message) {
        this.success = success;
        this.errorType = errorType;
        this.message = message;
    }

}