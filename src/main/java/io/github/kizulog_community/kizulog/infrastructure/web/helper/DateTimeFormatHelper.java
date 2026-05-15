package io.github.kizulog_community.kizulog.infrastructure.web.helper;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.TimeZone;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Thymeleafテンプレートで日時を整形するためのヘルパー
 *
 * @author Jun Kobayashi
 */
@Component("dateTimeFormatHelper")
public class DateTimeFormatHelper {

    /** 監査ログ用フォーマット（TZ明示）：例 "2026-05-15 22:00:15 JST (Asia/Tokyo)" */
    private static final String PATTERN_AUDIT = "yyyy-MM-dd HH:mm:ss z (VV)";

    /** 一般日時フォーマット（秒精度）：例 "2026-05-15 22:00:15" */
    private static final String PATTERN_DATETIME = "yyyy-MM-dd HH:mm:ss";

    /** 一般日時フォーマット（分精度）：例 "2026-05-15 22:00" */
    private static final String PATTERN_DATETIME_SHORT = "yyyy-MM-dd HH:mm";

    /**
     * 監査ログ用フォーマット #PATTERN_AUDIT で整形する。
     *
     * @param temporal 整形対象（nullの場合は空文字を返す）
     * @return 整形済み文字列、temporalがnullなら空文字
     */
    public String format(OffsetDateTime temporal) {
        return formatInternal(temporal, PATTERN_AUDIT);
    }

    /**
     * 秒精度の日時フォーマット #PATTERN_DATETIME で整形する。
     *
     * @param temporal 整形対象（nullの場合は空文字を返す）
     * @return 整形済み文字列、temporalがnullなら空文字
     */
    public String formatDateTime(OffsetDateTime temporal) {
        return formatInternal(temporal, PATTERN_DATETIME);
    }

    /**
     * 日時フォーマット #PATTERN_DATETIME_SHORT で整形する。
     *
     * @param temporal 整形対象（nullの場合は空文字を返す）
     * @return 整形済み文字列、temporalがnullなら空文字
     */
    public String formatDateTimeShort(OffsetDateTime temporal) {
        return formatInternal(temporal, PATTERN_DATETIME_SHORT);
    }

    /**
     * 整形処理
     *
     * @param temporal 整形対象
     * @param pattern フォーマットパターン
     * @return 整形済み文字列
     */
    private String formatInternal(OffsetDateTime temporal, String pattern) {
        if (temporal == null) {
            return "";
        }
        ZoneId zoneId = resolveZoneId();
        Locale locale = LocaleContextHolder.getLocale();
        ZonedDateTime zoned = temporal.atZoneSameInstant(zoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, locale);
        return zoned.format(formatter);
    }

    /**
     * 現在の LocaleContextHolder から ZoneId を取得する。
     *
     * @return ZoneId
     */
    private ZoneId resolveZoneId() {
        TimeZone tz = LocaleContextHolder.getTimeZone();
        if (tz != null) {
            return tz.toZoneId();
        }
        return TimeZone.getDefault().toZoneId();
    }

}
