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
    @DisplayName("IDENTITY_INACTIVEが定義されている")
    void values_containsIdentityInactive() {
        assertThat(SystemAuthenticationErrorType.IDENTITY_INACTIVE).isNotNull();
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
    @DisplayName("values()は4件の要素を返す")
    void values_returnsFourElements() {
        assertThat(SystemAuthenticationErrorType.values()).hasSize(4);
    }

    @Test
    @DisplayName("valueOf('ACCOUNT_NOT_FOUND')でACCOUNT_NOT_FOUNDを取得できる")
    void valueOf_accountNotFound() {
        assertThat(SystemAuthenticationErrorType.valueOf("ACCOUNT_NOT_FOUND"))
                .isEqualTo(SystemAuthenticationErrorType.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("valueOf('IDENTITY_INACTIVE')でIDENTITY_INACTIVEを取得できる")
    void valueOf_identityInactive() {
        assertThat(SystemAuthenticationErrorType.valueOf("IDENTITY_INACTIVE"))
                .isEqualTo(SystemAuthenticationErrorType.IDENTITY_INACTIVE);
    }

}
