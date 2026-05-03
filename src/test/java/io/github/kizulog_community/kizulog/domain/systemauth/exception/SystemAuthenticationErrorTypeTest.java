package io.github.kizulog_community.kizulog.domain.systemauth.exception;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SystemAuthenticationErrorTypeの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemAuthenticationErrorTypeTest {

    @Test
    @DisplayName("ACCOUNT_NOT_FOUNDが定義されている")
    void values_containsAccountNotFound() {
        assertThat(SystemAuthenticationErrorType.ACCOUNT_NOT_FOUND).isNotNull();
    }

    @Test
    @DisplayName("ACCOUNT_INACTIVEが定義されている")
    void values_containsAccountInactive() {
        assertThat(SystemAuthenticationErrorType.ACCOUNT_INACTIVE).isNotNull();
    }

    @Test
    @DisplayName("ROLE_NOT_GRANTEDが定義されている")
    void values_containsRoleNotGranted() {
        assertThat(SystemAuthenticationErrorType.ROLE_NOT_GRANTED).isNotNull();
    }

    @Test
    @DisplayName("values()は3件の要素を返す")
    void values_returnsThreeElements() {
        assertThat(SystemAuthenticationErrorType.values()).hasSize(3);
    }

    @Test
    @DisplayName("valueOf('ACCOUNT_NOT_FOUND')でACCOUNT_NOT_FOUNDを取得できる")
    void valueOf_accountNotFound() {
        assertThat(SystemAuthenticationErrorType.valueOf("ACCOUNT_NOT_FOUND"))
                .isEqualTo(SystemAuthenticationErrorType.ACCOUNT_NOT_FOUND);
    }

}
