package io.github.kizulog_community.kizulog.domain.systemaccountprofile.exception;

import lombok.Getter;

/**
 * アカウントプロファイル操作時の例外
 *
 * @author Jun Kobayashi
 */
@Getter
public class AccountProfileException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** エラー種別 */
    private final AccountProfileError error;

    /**
     * コンストラクタ
     *
     * @param error エラー種別
     */
    public AccountProfileException(AccountProfileError error) {
        super(error.name());
        this.error = error;
    }

    /**
     * 原因例外を含むコンストラクタ
     *
     * @param error エラー種別
     * @param cause 原因例外
     */
    public AccountProfileException(AccountProfileError error, Throwable cause) {
        super(error.name(), cause);
        this.error = error;
    }

}
