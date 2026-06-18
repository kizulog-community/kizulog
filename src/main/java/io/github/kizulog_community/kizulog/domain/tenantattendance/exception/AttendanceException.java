package io.github.kizulog_community.kizulog.domain.tenantattendance.exception;

import lombok.Getter;

/**
 * 打刻操作時の例外
 *
 * @author Jun Kobayashi
 */
@Getter
public class AttendanceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** エラー種別 */
    private final AttendanceError error;

    /**
     * コンストラクタ
     *
     * @param error エラー種別
     */
    public AttendanceException(AttendanceError error) {
        super(error.name());
        this.error = error;
    }

    /**
     * 原因例外を含むコンストラクタ
     *
     * @param error エラー種別
     * @param cause 原因例外
     */
    public AttendanceException(AttendanceError error, Throwable cause) {
        super(error.name(), cause);
        this.error = error;
    }

}
