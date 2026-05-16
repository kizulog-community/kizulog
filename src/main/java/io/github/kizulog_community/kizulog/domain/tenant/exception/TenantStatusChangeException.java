package io.github.kizulog_community.kizulog.domain.tenant.exception;

import lombok.Getter;

/**
 * テナントステータス変更のエラー例外
 *
 * @author Jun Kobayashi
 */
@Getter
public class TenantStatusChangeException extends RuntimeException {

    /** シリアライズUID */
    private static final long serialVersionUID = 1L;

    /** エラー種別 */
    private final TenantStatusChangeError error;

    /**
     * コンストラクタ
     *
     * @param error エラー種別
     */
    public TenantStatusChangeException(TenantStatusChangeError error) {
        super("Tenant status change error: " + error.name());
        this.error = error;
    }

}
