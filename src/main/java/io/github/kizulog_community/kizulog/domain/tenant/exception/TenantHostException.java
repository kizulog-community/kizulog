package io.github.kizulog_community.kizulog.domain.tenant.exception;

import lombok.Getter;

/**
 * テナントホスト操作（追加・無効化・再有効化）のエラー例外
 *
 * @author Jun Kobayashi
 */
@Getter
public class TenantHostException extends RuntimeException {

    /** シリアライズUID */
    private static final long serialVersionUID = 1L;

    /** エラー種別 */
    private final TenantHostError error;

    /**
     * コンストラクタ
     *
     * @param error エラー種別
     */
    public TenantHostException(TenantHostError error) {
        super("Tenant host operation error: " + error.name());
        this.error = error;
    }

}
