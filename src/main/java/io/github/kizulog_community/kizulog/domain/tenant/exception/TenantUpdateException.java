package io.github.kizulog_community.kizulog.domain.tenant.exception;

import lombok.Getter;

/**
 * テナント編集のエラー例外
 *
 * @author Jun Kobayashi
 */
@Getter
public class TenantUpdateException extends RuntimeException {

    /** シリアライズUID */
    private static final long serialVersionUID = 1L;

    /** エラー種別 */
    private final TenantUpdateError error;

    /**
     * コンストラクタ
     *
     * @param error エラー種別
     */
    public TenantUpdateException(TenantUpdateError error) {
        super("Tenant update error: " + error.name());
        this.error = error;
    }

}
