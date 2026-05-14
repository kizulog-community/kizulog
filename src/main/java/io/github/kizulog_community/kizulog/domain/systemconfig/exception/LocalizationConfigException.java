package io.github.kizulog_community.kizulog.domain.systemconfig.exception;

import lombok.Getter;

/**
 * 言語・タイムゾーン設定操作時の例外
 *
 * @author Jun Kobayashi
 */
@Getter
public class LocalizationConfigException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** エラー種別 */
    private final LocalizationConfigError error;

    /**
     * コンストラクタ
     *
     * @param error エラー種別
     */
    public LocalizationConfigException(LocalizationConfigError error) {
        super(error.name());
        this.error = error;
    }

    /**
     * 原因例外を含むコンストラクタ
     *
     * @param error エラー種別
     * @param cause 原因例外
     */
    public LocalizationConfigException(LocalizationConfigError error, Throwable cause) {
        super(error.name(), cause);
        this.error = error;
    }

}
