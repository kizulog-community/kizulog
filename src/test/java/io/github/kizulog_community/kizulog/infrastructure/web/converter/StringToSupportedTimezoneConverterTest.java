package io.github.kizulog_community.kizulog.infrastructure.web.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.zone.ZoneRulesException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;

/**
 * StringToSupportedTimezoneConverterの単体テスト
 *
 * @author Jun Kobayashi
 */
class StringToSupportedTimezoneConverterTest {

    private StringToSupportedTimezoneConverter converter;

    @BeforeEach
    void setUp() {
        converter = new StringToSupportedTimezoneConverter();
    }

    @Test
    @DisplayName("convert: 有効なIANA タイムゾーンID（Asia/Tokyo）はSupportedTimezoneに変換される")
    void convert_validId_returnsSupportedTimezone() {
        SupportedTimezone result = converter.convert("Asia/Tokyo");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("Asia/Tokyo");
    }

    @Test
    @DisplayName("convert: 有効なIANA タイムゾーンID（UTC）はSupportedTimezoneに変換される")
    void convert_utc_returnsSupportedTimezone() {
        SupportedTimezone result = converter.convert("UTC");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("UTC");
    }

    @Test
    @DisplayName("convert: 有効なIANA タイムゾーンID（America/New_York）はSupportedTimezoneに変換される")
    void convert_americaNewYork_returnsSupportedTimezone() {
        SupportedTimezone result = converter.convert("America/New_York");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("America/New_York");
    }

    @Test
    @DisplayName("convert: nullはnullを返す（フォーム未入力扱い）")
    void convert_null_returnsNull() {
        SupportedTimezone result = converter.convert(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("convert: 空文字列はnullを返す")
    void convert_empty_returnsNull() {
        SupportedTimezone result = converter.convert("");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("convert: 空白のみ文字列はnullを返す")
    void convert_blank_returnsNull() {
        SupportedTimezone result = converter.convert("   ");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("convert: タブ・改行のみ文字列はnullを返す")
    void convert_whitespaceChars_returnsNull() {
        SupportedTimezone result = converter.convert("\t\n  ");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("convert: 不正なIDはZoneRulesExceptionを投げる")
    void convert_invalidId_throwsZoneRulesException() {
        assertThatThrownBy(() -> converter.convert("Invalid/Zone"))
                .isInstanceOf(ZoneRulesException.class);
    }

    @Test
    @DisplayName("convert: 存在しないIDはZoneRulesExceptionを投げる")
    void convert_nonExistentId_throwsZoneRulesException() {
        assertThatThrownBy(() -> converter.convert("XYZ/Nonexistent"))
                .isInstanceOf(ZoneRulesException.class);
    }

    @Test
    @DisplayName("convert: 完全なゴミ文字列はZoneRulesExceptionを投げる")
    void convert_garbage_throwsZoneRulesException() {
        assertThatThrownBy(() -> converter.convert("garbage-input-12345"))
                .isInstanceOf(ZoneRulesException.class);
    }

}
