package io.github.kizulog_community.kizulog.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SupportedLanguageの単体テスト.
 *
 * @author Jun Kobayashi
 */
class SupportedLanguageTest {

    @Test
    @DisplayName("JAはLocale.JAPANESEを返す")
    void getLocale_ja_returnsJapanese() {
        assertThat(SupportedLanguage.JA.getLocale()).isEqualTo(Locale.JAPANESE);
    }

    @Test
    @DisplayName("ENはLocale.ENGLISHを返す")
    void getLocale_en_returnsEnglish() {
        assertThat(SupportedLanguage.EN.getLocale()).isEqualTo(Locale.ENGLISH);
    }

    @Test
    @DisplayName("JAは日本語を返す")
    void getDisplayName_ja_returnsJapaneseDisplayName() {
        assertThat(SupportedLanguage.JA.getDisplayName()).isEqualTo("日本語");
    }

    @Test
    @DisplayName("ENはEnglishを返す")
    void getDisplayName_en_returnsEnglishDisplayName() {
        assertThat(SupportedLanguage.EN.getDisplayName()).isEqualTo("English");
    }

    @Test
    @DisplayName("JAはjaを返す")
    void getCode_ja_returnsJaCode() {
        assertThat(SupportedLanguage.JA.getCode()).isEqualTo("ja");
    }

    @Test
    @DisplayName("ENはenを返す")
    void getCode_en_returnsEnCode() {
        assertThat(SupportedLanguage.EN.getCode()).isEqualTo("en");
    }

    @Test
    @DisplayName("values()はJAとENの2件を返す")
    void values_returnsAllValues() {
        assertThat(SupportedLanguage.values())
                .containsExactly(SupportedLanguage.JA, SupportedLanguage.EN);
    }

    @Test
    @DisplayName("valueOf('JA')でJAを取得できる")
    void valueOf_ja() {
        assertThat(SupportedLanguage.valueOf("JA")).isEqualTo(SupportedLanguage.JA);
    }

    @Test
    @DisplayName("valueOf('EN')でENを取得できる")
    void valueOf_en() {
        assertThat(SupportedLanguage.valueOf("EN")).isEqualTo(SupportedLanguage.EN);
    }

}