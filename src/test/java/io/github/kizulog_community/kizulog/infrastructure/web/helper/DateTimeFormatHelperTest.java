package io.github.kizulog_community.kizulog.infrastructure.web.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext;

/**
 * DateTimeFormatHelperの単体テスト
 *
 * @author Jun Kobayashi
 */
class DateTimeFormatHelperTest {

    /** 基準時刻: 2026-05-15T13:00:15Z (UTC) */
    private static final OffsetDateTime UTC_TIME =
            OffsetDateTime.of(2026, 5, 15, 13, 0, 15, 0, ZoneOffset.UTC);

    private DateTimeFormatHelper helper;

    @BeforeEach
    void setUp() {
        helper = new DateTimeFormatHelper();
        // デフォルト: Asia/Tokyo + ja_JP
        LocaleContextHolder.setLocaleContext(new SimpleTimeZoneAwareLocaleContext(
                Locale.JAPAN, TimeZone.getTimeZone("Asia/Tokyo")));
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("format: Asia/Tokyo TZで UTC 13:00:15 → 22:00:15 JST (Asia/Tokyo) に変換される")
    void format_jstTimezone_convertsAndFormatsCorrectly() {
        String result = helper.format(UTC_TIME);

        assertThat(result).isEqualTo("2026-05-15 22:00:15 JST (Asia/Tokyo)");
    }

    @Test
    @DisplayName("format: UTC TZで UTC 13:00:15 → 13:00:15 UTC (Etc/UTC) に変換される")
    void format_utcTimezone_formatsAsUtc() {
        LocaleContextHolder.setLocaleContext(new SimpleTimeZoneAwareLocaleContext(
                Locale.US, TimeZone.getTimeZone("Etc/UTC")));

        String result = helper.format(UTC_TIME);

        // 'z' for UTC is "UTC" in en_US locale; 'VV' shows the zone ID
        assertThat(result).isEqualTo("2026-05-15 13:00:15 UTC (Etc/UTC)");
    }

    @Test
    @DisplayName("format: America/New_York TZ (EST=UTC-5) で UTC 13:00:15 → 09:00:15 EDT/EST")
    void format_americaNewYork_convertsAndFormats() {
        LocaleContextHolder.setLocaleContext(new SimpleTimeZoneAwareLocaleContext(
                Locale.US, TimeZone.getTimeZone("America/New_York")));

        // 2026-05-15 は夏時間(EDT, UTC-4) なので 13:00:15 UTC → 09:00:15 EDT
        String result = helper.format(UTC_TIME);

        assertThat(result).isEqualTo("2026-05-15 09:00:15 EDT (America/New_York)");
    }

    @Test
    @DisplayName("format: nullを渡したら空文字を返す")
    void format_null_returnsEmptyString() {
        String result = helper.format(null);

        assertThat(result).isEqualTo("");
    }

    @Test
    @DisplayName("format: 既にAsia/TokyoオフセットのOffsetDateTimeも正しく変換される")
    void format_offsetDateTimeAlreadyInJst_formatsCorrectly() {
        OffsetDateTime jstTime = OffsetDateTime.of(
                2026, 5, 15, 22, 0, 15, 0, ZoneOffset.ofHours(9));

        String result = helper.format(jstTime);

        // 内容は同じ瞬間なので結果も同じ
        assertThat(result).isEqualTo("2026-05-15 22:00:15 JST (Asia/Tokyo)");
    }

    @Test
    @DisplayName("formatDateTime: Asia/Tokyo TZで UTC 13:00:15 → 2026-05-15 22:00:15")
    void formatDateTime_jstTimezone_formatsWithSeconds() {
        String result = helper.formatDateTime(UTC_TIME);

        assertThat(result).isEqualTo("2026-05-15 22:00:15");
    }

    @Test
    @DisplayName("formatDateTime: UTC TZでフォーマットされる")
    void formatDateTime_utc_formatsAsUtc() {
        LocaleContextHolder.setLocaleContext(new SimpleTimeZoneAwareLocaleContext(
                Locale.US, TimeZone.getTimeZone("Etc/UTC")));

        String result = helper.formatDateTime(UTC_TIME);

        assertThat(result).isEqualTo("2026-05-15 13:00:15");
    }

    @Test
    @DisplayName("formatDateTime: nullを渡したら空文字を返す")
    void formatDateTime_null_returnsEmptyString() {
        String result = helper.formatDateTime(null);

        assertThat(result).isEqualTo("");
    }

    @Test
    @DisplayName("formatDateTimeShort: Asia/Tokyo TZで UTC 13:00:15 → 2026-05-15 22:00")
    void formatDateTimeShort_jstTimezone_formatsWithMinutes() {
        String result = helper.formatDateTimeShort(UTC_TIME);

        assertThat(result).isEqualTo("2026-05-15 22:00");
    }

    @Test
    @DisplayName("formatDateTimeShort: UTC TZでフォーマットされる")
    void formatDateTimeShort_utc_formatsAsUtc() {
        LocaleContextHolder.setLocaleContext(new SimpleTimeZoneAwareLocaleContext(
                Locale.US, TimeZone.getTimeZone("Etc/UTC")));

        String result = helper.formatDateTimeShort(UTC_TIME);

        assertThat(result).isEqualTo("2026-05-15 13:00");
    }

    @Test
    @DisplayName("formatDateTimeShort: nullを渡したら空文字を返す")
    void formatDateTimeShort_null_returnsEmptyString() {
        String result = helper.formatDateTimeShort(null);

        assertThat(result).isEqualTo("");
    }

    @Test
    @DisplayName("format: LocaleContextHolderにTZ未設定でもデフォルトTZにフォールバックして正常動作")
    void format_noLocaleContextSet_fallbacksToDefaultTimezone() {
        LocaleContextHolder.resetLocaleContext();

        // 例外を投げずに何らかの文字列を返すこと
        String result = helper.format(UTC_TIME);

        assertThat(result).isNotEmpty();
        assertThat(result).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} .+ \\(.+\\)");
    }

    @Test
    @DisplayName("formatDateTime: LocaleContextHolderにTZ未設定でもデフォルトTZにフォールバック")
    void formatDateTime_noLocaleContextSet_fallbacksToDefaultTimezone() {
        LocaleContextHolder.resetLocaleContext();

        String result = helper.formatDateTime(UTC_TIME);

        assertThat(result).isNotEmpty();
        assertThat(result).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    @DisplayName("format: 日付またぎ（UTC 23:00 → JST 翌日 08:00）が正しく処理される")
    void format_dayBoundary_correctlyShiftsDate() {
        OffsetDateTime nightUtc = OffsetDateTime.of(
                2026, 5, 15, 23, 0, 0, 0, ZoneOffset.UTC);

        String result = helper.format(nightUtc);

        // JST = UTC+9 なので 翌日 08:00:00
        assertThat(result).isEqualTo("2026-05-16 08:00:00 JST (Asia/Tokyo)");
    }

    @Test
    @DisplayName("format: 日付戻し（UTC 02:00 → Honolulu 前日 16:00）が正しく処理される")
    void format_dayBackward_correctlyShiftsDate() {
        OffsetDateTime morningUtc = OffsetDateTime.of(
                2026, 5, 15, 2, 0, 0, 0, ZoneOffset.UTC);
        LocaleContextHolder.setLocaleContext(new SimpleTimeZoneAwareLocaleContext(
                Locale.US, TimeZone.getTimeZone("Pacific/Honolulu")));

        String result = helper.format(morningUtc);

        // HST = UTC-10 なので 前日 16:00:00
        assertThat(result).isEqualTo("2026-05-14 16:00:00 HST (Pacific/Honolulu)");
    }

    @Test
    @DisplayName("各メソッドは独立して動作する：同一インスタンスで連続呼び出ししても問題なし")
    void multipleCallsWithSameInstance_workIndependently() {
        String r1 = helper.format(UTC_TIME);
        String r2 = helper.formatDateTime(UTC_TIME);
        String r3 = helper.formatDateTimeShort(UTC_TIME);

        assertThat(r1).isEqualTo("2026-05-15 22:00:15 JST (Asia/Tokyo)");
        assertThat(r2).isEqualTo("2026-05-15 22:00:15");
        assertThat(r3).isEqualTo("2026-05-15 22:00");
    }

    @Test
    @DisplayName("VVパターンの確認: ZoneIdが正しく Asia/Tokyo として出力される")
    void format_zoneIdRepresentationIsCorrect() {
        String result = helper.format(UTC_TIME);

        // 末尾に "(Asia/Tokyo)" が含まれる
        assertThat(result).endsWith("(Asia/Tokyo)");
    }

    /** SonarLint対応: unused 警告を回避するため明示利用 */
    @SuppressWarnings("unused")
    private static ZoneId unusedZoneIdReference() {
        return ZoneId.systemDefault();
    }

}
