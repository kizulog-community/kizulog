package io.github.kizulog_community.kizulog.domain.systemconfig.exception;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * LocalizationConfigErrorの単体テスト
 *
 * @author Jun Kobayashi
 */
class LocalizationConfigErrorTest {

    @Test
    @DisplayName("DEFAULT_LANGUAGE_REQUIREDが定義されている")
    void values_containsDefaultLanguageRequired() {
        assertThat(LocalizationConfigError.DEFAULT_LANGUAGE_REQUIRED).isNotNull();
    }

    @Test
    @DisplayName("AVAILABLE_LANGUAGES_EMPTYが定義されている")
    void values_containsAvailableLanguagesEmpty() {
        assertThat(LocalizationConfigError.AVAILABLE_LANGUAGES_EMPTY).isNotNull();
    }

    @Test
    @DisplayName("DEFAULT_LANGUAGE_NOT_IN_AVAILABLEが定義されている")
    void values_containsDefaultLanguageNotInAvailable() {
        assertThat(LocalizationConfigError.DEFAULT_LANGUAGE_NOT_IN_AVAILABLE).isNotNull();
    }

    @Test
    @DisplayName("DEFAULT_TIMEZONE_REQUIREDが定義されている")
    void values_containsDefaultTimezoneRequired() {
        assertThat(LocalizationConfigError.DEFAULT_TIMEZONE_REQUIRED).isNotNull();
    }

    @Test
    @DisplayName("AVAILABLE_TIMEZONES_EMPTYが定義されている")
    void values_containsAvailableTimezonesEmpty() {
        assertThat(LocalizationConfigError.AVAILABLE_TIMEZONES_EMPTY).isNotNull();
    }

    @Test
    @DisplayName("DEFAULT_TIMEZONE_NOT_IN_AVAILABLEが定義されている")
    void values_containsDefaultTimezoneNotInAvailable() {
        assertThat(LocalizationConfigError.DEFAULT_TIMEZONE_NOT_IN_AVAILABLE).isNotNull();
    }

    @Test
    @DisplayName("SERIALIZATION_FAILEDが定義されている")
    void values_containsSerializationFailed() {
        assertThat(LocalizationConfigError.SERIALIZATION_FAILED).isNotNull();
    }

    @Test
    @DisplayName("DESERIALIZATION_FAILEDが定義されている")
    void values_containsDeserializationFailed() {
        assertThat(LocalizationConfigError.DESERIALIZATION_FAILED).isNotNull();
    }

    @Test
    @DisplayName("values()は8件の要素を返す")
    void values_returnsEightElements() {
        assertThat(LocalizationConfigError.values()).hasSize(8);
    }

    @Test
    @DisplayName("valueOf('DEFAULT_LANGUAGE_REQUIRED')でDEFAULT_LANGUAGE_REQUIREDを取得できる")
    void valueOf_defaultLanguageRequired() {
        assertThat(LocalizationConfigError.valueOf("DEFAULT_LANGUAGE_REQUIRED"))
                .isEqualTo(LocalizationConfigError.DEFAULT_LANGUAGE_REQUIRED);
    }

}
