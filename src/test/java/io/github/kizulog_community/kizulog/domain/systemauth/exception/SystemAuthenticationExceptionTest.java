package io.github.kizulog_community.kizulog.domain.systemauth.exception;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.AuthenticationException;

/**
 * SystemAuthenticationExceptionの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemAuthenticationExceptionTest {

    @Test
    @DisplayName("コンストラクタ(エラー種別のみ)でインスタンスを生成できる")
    void constructor_errorTypeOnly_createsInstance() {
        SystemAuthenticationException ex = new SystemAuthenticationException(
                SystemAuthenticationErrorType.ACCOUNT_NOT_FOUND);
        assertThat(ex).isNotNull();
    }

    @Test
    @DisplayName("コンストラクタ(エラー種別のみ)でgetErrorType()が正しい値を返す")
    void getErrorType_errorTypeOnly_returnsErrorType() {
        SystemAuthenticationException ex = new SystemAuthenticationException(
                SystemAuthenticationErrorType.ACCOUNT_NOT_FOUND);
        assertThat(ex.getErrorType())
                .isEqualTo(SystemAuthenticationErrorType.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("コンストラクタ(エラー種別のみ)のメッセージはエラー種別名")
    void getMessage_errorTypeOnly_returnsErrorTypeName() {
        SystemAuthenticationException ex = new SystemAuthenticationException(
                SystemAuthenticationErrorType.ACCOUNT_NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("ACCOUNT_NOT_FOUND");
    }

    @Test
    @DisplayName("コンストラクタ(エラー種別のみ)のcauseはnull")
    void getCause_errorTypeOnly_returnsNull() {
        SystemAuthenticationException ex = new SystemAuthenticationException(
                SystemAuthenticationErrorType.ACCOUNT_NOT_FOUND);
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("コンストラクタ(原因例外あり)でインスタンスを生成できる")
    void constructor_withCause_createsInstance() {
        Throwable cause = new RuntimeException("root cause");
        SystemAuthenticationException ex = new SystemAuthenticationException(
                SystemAuthenticationErrorType.ACCOUNT_INACTIVE, cause);
        assertThat(ex).isNotNull();
    }

    @Test
    @DisplayName("コンストラクタ(原因例外あり)でgetErrorType()が正しい値を返す")
    void getErrorType_withCause_returnsErrorType() {
        Throwable cause = new RuntimeException("root cause");
        SystemAuthenticationException ex = new SystemAuthenticationException(
                SystemAuthenticationErrorType.ACCOUNT_INACTIVE, cause);
        assertThat(ex.getErrorType())
                .isEqualTo(SystemAuthenticationErrorType.ACCOUNT_INACTIVE);
    }

    @Test
    @DisplayName("コンストラクタ(原因例外あり)のメッセージはエラー種別名")
    void getMessage_withCause_returnsErrorTypeName() {
        Throwable cause = new RuntimeException("root cause");
        SystemAuthenticationException ex = new SystemAuthenticationException(
                SystemAuthenticationErrorType.ACCOUNT_INACTIVE, cause);
        assertThat(ex.getMessage()).isEqualTo("ACCOUNT_INACTIVE");
    }

    @Test
    @DisplayName("コンストラクタ(原因例外あり)のcauseが正しく設定されている")
    void getCause_withCause_returnsOriginalCause() {
        Throwable cause = new RuntimeException("root cause");
        SystemAuthenticationException ex = new SystemAuthenticationException(
                SystemAuthenticationErrorType.ACCOUNT_INACTIVE, cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("AuthenticationExceptionを継承している")
    void isInstanceOfAuthenticationException() {
        SystemAuthenticationException ex = new SystemAuthenticationException(
                SystemAuthenticationErrorType.ROLE_NOT_GRANTED);
        assertThat(ex).isInstanceOf(AuthenticationException.class);
    }

}
