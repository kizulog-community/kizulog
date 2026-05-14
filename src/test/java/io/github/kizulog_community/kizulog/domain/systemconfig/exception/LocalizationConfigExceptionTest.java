package io.github.kizulog_community.kizulog.domain.systemconfig.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * LocalizationConfigExceptionの単体テスト
 *
 * @author Jun Kobayashi
 */
class LocalizationConfigExceptionTest {

    @Test
    @DisplayName("コンストラクタ（エラー種別のみ）でインスタンスを生成できる")
    void constructor_errorTypeOnly_createsInstance() {
        LocalizationConfigException ex = new LocalizationConfigException(
                LocalizationConfigError.DEFAULT_LANGUAGE_REQUIRED);
        assertThat(ex).isNotNull();
    }

    @Test
    @DisplayName("コンストラクタ（エラー種別のみ）でgetError()が正しい値を返す")
    void getError_errorTypeOnly_returnsError() {
        LocalizationConfigException ex = new LocalizationConfigException(
                LocalizationConfigError.DEFAULT_LANGUAGE_REQUIRED);
        assertThat(ex.getError())
                .isEqualTo(LocalizationConfigError.DEFAULT_LANGUAGE_REQUIRED);
    }

    @Test
    @DisplayName("コンストラクタ（エラー種別のみ）のメッセージはエラー種別名")
    void getMessage_errorTypeOnly_returnsErrorName() {
        LocalizationConfigException ex = new LocalizationConfigException(
                LocalizationConfigError.DEFAULT_LANGUAGE_REQUIRED);
        assertThat(ex.getMessage()).isEqualTo("DEFAULT_LANGUAGE_REQUIRED");
    }

    @Test
    @DisplayName("コンストラクタ（エラー種別のみ）のcauseはnull")
    void getCause_errorTypeOnly_returnsNull() {
        LocalizationConfigException ex = new LocalizationConfigException(
                LocalizationConfigError.DEFAULT_LANGUAGE_REQUIRED);
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("コンストラクタ（原因例外あり）でインスタンスを生成できる")
    void constructor_withCause_createsInstance() {
        Throwable cause = new RuntimeException("root cause");
        LocalizationConfigException ex = new LocalizationConfigException(
                LocalizationConfigError.SERIALIZATION_FAILED, cause);
        assertThat(ex).isNotNull();
    }

    @Test
    @DisplayName("コンストラクタ（原因例外あり）でgetError()が正しい値を返す")
    void getError_withCause_returnsError() {
        Throwable cause = new RuntimeException("root cause");
        LocalizationConfigException ex = new LocalizationConfigException(
                LocalizationConfigError.SERIALIZATION_FAILED, cause);
        assertThat(ex.getError())
                .isEqualTo(LocalizationConfigError.SERIALIZATION_FAILED);
    }

    @Test
    @DisplayName("コンストラクタ（原因例外あり）のメッセージはエラー種別名")
    void getMessage_withCause_returnsErrorName() {
        Throwable cause = new RuntimeException("root cause");
        LocalizationConfigException ex = new LocalizationConfigException(
                LocalizationConfigError.SERIALIZATION_FAILED, cause);
        assertThat(ex.getMessage()).isEqualTo("SERIALIZATION_FAILED");
    }

    @Test
    @DisplayName("コンストラクタ（原因例外あり）のcauseが正しく設定されている")
    void getCause_withCause_returnsOriginalCause() {
        Throwable cause = new RuntimeException("root cause");
        LocalizationConfigException ex = new LocalizationConfigException(
                LocalizationConfigError.SERIALIZATION_FAILED, cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("RuntimeExceptionを継承している")
    void isInstanceOfRuntimeException() {
        LocalizationConfigException ex = new LocalizationConfigException(
                LocalizationConfigError.DEFAULT_LANGUAGE_REQUIRED);
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

}
