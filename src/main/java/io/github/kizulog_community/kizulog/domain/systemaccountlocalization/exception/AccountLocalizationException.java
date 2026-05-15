package io.github.kizulog_community.kizulog.domain.systemaccountlocalization.exception;

import lombok.Getter;

/**
 * アカウント単位の言語・タイムゾーン設定操作時の例外
 *
 * @author Jun Kobayashi
 */
@Getter
public class AccountLocalizationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** エラー種別 */
    private final AccountLocalizationError error;

    /**
     * コンストラクタ
     *
     * @param error エラー種別
     */
    public AccountLocalizationException(AccountLocalizationError error) {
        super(error.name());
        this.error = error;
    }

    /**
     * 原因例外を含むコンストラクタ
     *
     * @param error エラー種別
     * @param cause 原因例外
     */
    public AccountLocalizationException(AccountLocalizationError error, Throwable cause) {
        super(error.name(), cause);
        this.error = error;
    }

}
