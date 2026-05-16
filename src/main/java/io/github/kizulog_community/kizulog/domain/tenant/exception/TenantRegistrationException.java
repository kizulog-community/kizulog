package io.github.kizulog_community.kizulog.domain.tenant.exception;

import lombok.Getter;

/**
 * テナント新規登録のエラー例外
 *
 * @author Jun Kobayashi
 */
@Getter
public class TenantRegistrationException extends RuntimeException {

    /** シリアライズUID */
    private static final long serialVersionUID = 1L;

    /** エラー種別 */
    private final TenantRegistrationError error;

    /**
     * コンストラクタ
     *
     * @param error エラー種別
     */
    public TenantRegistrationException(TenantRegistrationError error) {
        super("Tenant registration error: " + error.name());
        this.error = error;
    }

}
