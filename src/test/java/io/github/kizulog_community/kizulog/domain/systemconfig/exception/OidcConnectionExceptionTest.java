package io.github.kizulog_community.kizulog.domain.systemconfig.exception;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * OidcConnectionExceptionの単体テスト
 *
 * @author Jun Kobayashi
 */
class OidcConnectionExceptionTest {

    @Test
    @DisplayName("コンストラクタ（エラー種別のみ）でインスタンスを生成できる")
    void constructor_errorTypeOnly_createsInstance() {
        OidcConnectionException ex =
                new OidcConnectionException(OidcConnectionError.INPUT_ERROR);
        assertThat(ex).isNotNull();
    }

    @Test
    @DisplayName("コンストラクタ（エラー種別のみ）でgetErrorType()が正しい値を返す")
    void getErrorType_errorTypeOnly_returnsErrorType() {
        OidcConnectionException ex =
                new OidcConnectionException(OidcConnectionError.INPUT_ERROR);
        assertThat(ex.getErrorType()).isEqualTo(OidcConnectionError.INPUT_ERROR);
    }

    @Test
    @DisplayName("コンストラクタ（エラー種別のみ）のメッセージはエラー種別名")
    void getMessage_errorTypeOnly_returnsErrorTypeName() {
        OidcConnectionException ex =
                new OidcConnectionException(OidcConnectionError.INPUT_ERROR);
        assertThat(ex.getMessage()).isEqualTo("INPUT_ERROR");
    }

    @Test
    @DisplayName("コンストラクタ（エラー種別のみ）のcauseはnull")
    void getCause_errorTypeOnly_returnsNull() {
        OidcConnectionException ex =
                new OidcConnectionException(OidcConnectionError.INPUT_ERROR);
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("コンストラクタ（原因例外あり）でインスタンスを生成できる")
    void constructor_withCause_createsInstance() {
        Throwable cause = new RuntimeException("root cause");
        OidcConnectionException ex = new OidcConnectionException(
                OidcConnectionError.CONNECTION_ERROR, cause);
        assertThat(ex).isNotNull();
    }

    @Test
    @DisplayName("コンストラクタ（原因例外あり）でgetErrorType()が正しい値を返す")
    void getErrorType_withCause_returnsErrorType() {
        Throwable cause = new RuntimeException("root cause");
        OidcConnectionException ex = new OidcConnectionException(
                OidcConnectionError.CONNECTION_ERROR, cause);
        assertThat(ex.getErrorType()).isEqualTo(OidcConnectionError.CONNECTION_ERROR);
    }

    @Test
    @DisplayName("コンストラクタ（原因例外あり）のメッセージはエラー種別名")
    void getMessage_withCause_returnsErrorTypeName() {
        Throwable cause = new RuntimeException("root cause");
        OidcConnectionException ex = new OidcConnectionException(
                OidcConnectionError.CONNECTION_ERROR, cause);
        assertThat(ex.getMessage()).isEqualTo("CONNECTION_ERROR");
    }

    @Test
    @DisplayName("コンストラクタ（原因例外あり）のcauseが正しく設定されている")
    void getCause_withCause_returnsOriginalCause() {
        Throwable cause = new RuntimeException("root cause");
        OidcConnectionException ex = new OidcConnectionException(
                OidcConnectionError.CONNECTION_ERROR, cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("RuntimeExceptionを継承している")
    void isInstanceOfRuntimeException() {
        OidcConnectionException ex =
                new OidcConnectionException(OidcConnectionError.UNEXPECTED_ERROR);
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

}