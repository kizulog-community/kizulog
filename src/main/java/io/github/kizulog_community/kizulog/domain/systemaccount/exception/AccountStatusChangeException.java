package io.github.kizulog_community.kizulog.domain.systemaccount.exception;

import lombok.Getter;

/**
 * システム管理アカウントのステータス変更エラー例外
 *
 * @author Jun Kobayashi
 */
@Getter
public class AccountStatusChangeException extends RuntimeException {

    /** シリアライズUID */
    private static final long serialVersionUID = 1L;

    /** エラー種別 */
    private final AccountStatusChangeError error;

    /**
     * コンストラクタ
     *
     * @param error エラー種別
     */
    public AccountStatusChangeException(AccountStatusChangeError error) {
        super("Account status change error: " + error.name());
        this.error = error;
    }

}
