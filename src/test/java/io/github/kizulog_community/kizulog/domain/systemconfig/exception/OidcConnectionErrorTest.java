package io.github.kizulog_community.kizulog.domain.systemconfig.exception;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * OidcConnectionErrorの単体テスト
 *
 * @author Jun Kobayashi
 */
class OidcConnectionErrorTest {

    @Test
    @DisplayName("INPUT_ERRORが定義されている")
    void values_containsInputError() {
        assertThat(OidcConnectionError.INPUT_ERROR).isNotNull();
    }

    @Test
    @DisplayName("CONNECTION_ERRORが定義されている")
    void values_containsConnectionError() {
        assertThat(OidcConnectionError.CONNECTION_ERROR).isNotNull();
    }

    @Test
    @DisplayName("INVALID_RESPONSEが定義されている")
    void values_containsInvalidResponse() {
        assertThat(OidcConnectionError.INVALID_RESPONSE).isNotNull();
    }

    @Test
    @DisplayName("UNEXPECTED_ERRORが定義されている")
    void values_containsUnexpectedError() {
        assertThat(OidcConnectionError.UNEXPECTED_ERROR).isNotNull();
    }

    @Test
    @DisplayName("values()は4件の要素を返す")
    void values_returnsFourElements() {
        assertThat(OidcConnectionError.values()).hasSize(4);
    }

    @Test
    @DisplayName("valueOf('INPUT_ERROR')でINPUT_ERRORを取得できる")
    void valueOf_inputError() {
        assertThat(OidcConnectionError.valueOf("INPUT_ERROR"))
                .isEqualTo(OidcConnectionError.INPUT_ERROR);
    }

}