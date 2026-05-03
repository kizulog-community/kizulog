package io.github.kizulog_community.kizulog.domain.systemauth.exception;

import org.springframework.security.core.AuthenticationException;

import lombok.Getter;

/**
 * システム管理認証例外
 *
 * <p>システム管理者の認証処理で発生した異常を表現する。
 * Spring SecurityのAuthenticationExceptionを継承することで、
 * 認証フィルタチェーンのAuthenticationFailureHandlerに伝播される。</p>
 *
 * <p>エラーの種別は SystemAuthenticationErrorType で識別する。
 * ユーザー向け表示メッセージはController層でMessageSourceを介して変換する。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
public class SystemAuthenticationException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    /** エラー種別 */
    private final SystemAuthenticationErrorType errorType;

    /**
     * コンストラクタ
     *
     * @param errorType エラー種別
     */
    public SystemAuthenticationException(SystemAuthenticationErrorType errorType) {
        super(errorType == null ? "errorType=null" : errorType.name());
        this.errorType = errorType;
    }

    /**
     * コンストラクタ（原因例外あり）
     *
     * @param errorType エラー種別
     * @param cause 原因例外
     */
    public SystemAuthenticationException(
            SystemAuthenticationErrorType errorType, Throwable cause) {
        super(errorType == null ? "errorType=null" : errorType.name(), cause);
        this.errorType = errorType;
    }

}
