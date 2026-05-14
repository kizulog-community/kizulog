package io.github.kizulog_community.kizulog.config;

import java.util.Locale;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import io.github.kizulog_community.kizulog.domain.systemconfig.service.LocalizationSettingService;

/**
 * ロケール設定クラス
 *
 * <p>セッション単位で言語を切り替えるための設定を行う。</p>
 *
 * @author Jun Kobayashi
 */
@Configuration
public class LocaleConfig {

    /** ロガー */
    private static final Logger log = LoggerFactory.getLogger(LocaleConfig.class);

    /** DB未保存・取得失敗時のフォールバックLocale */
    private static final Locale FALLBACK_LOCALE = Locale.JAPANESE;

    /** DB未保存・取得失敗時のフォールバックTimeZone */
    private static final TimeZone FALLBACK_TIMEZONE = TimeZone.getTimeZone("Asia/Tokyo");

    /**
     * セッションベースのLocaleResolverを登録する。
     *
     * @param localizationSettingService 言語・タイムゾーン設定サービス（遅延注入）
     * @return SessionLocaleResolver
     */
    @Bean
    SessionLocaleResolver localeResolver(
            @Lazy LocalizationSettingService localizationSettingService) {
        SessionLocaleResolver resolver = new SessionLocaleResolver();

        // 最終フォールバック（Function内で例外発生時等の最後の砦）
        resolver.setDefaultLocale(FALLBACK_LOCALE);
        resolver.setDefaultTimeZone(FALLBACK_TIMEZONE);

        // セッション未設定時にDBから動的取得
        resolver.setDefaultLocaleFunction(
                _ -> resolveDefaultLocale(localizationSettingService));
        resolver.setDefaultTimeZoneFunction(
                _ -> resolveDefaultTimeZone(localizationSettingService));

        return resolver;
    }

    /**
     * DBからデフォルトLocaleを解決する。
     *
     * @param service 言語・タイムゾーン設定サービス
     * @return デフォルトLocale
     */
    private Locale resolveDefaultLocale(LocalizationSettingService service) {
        try {
            return service.getLanguageSetting()
                    .map(setting -> setting.getDefaultLanguage())
                    .map(lang -> lang.getLocale())
                    .orElse(FALLBACK_LOCALE);
        } catch (Exception e) {
            log.warn("DBからのデフォルトLocale取得に失敗しました。フォールバック値({})を使用します。",
                    FALLBACK_LOCALE, e);
            return FALLBACK_LOCALE;
        }
    }

    /**
     * DBからデフォルトTimeZoneを解決する。
     *
     * @param service 言語・タイムゾーン設定サービス
     * @return デフォルトTimeZone
     */
    private TimeZone resolveDefaultTimeZone(LocalizationSettingService service) {
        try {
            return service.getTimezoneSetting()
                    .map(setting -> setting.getDefaultTimezone())
                    .map(tz -> TimeZone.getTimeZone(tz.getZoneId()))
                    .orElse(FALLBACK_TIMEZONE);
        } catch (Exception e) {
            log.warn("DBからのデフォルトTimeZone取得に失敗しました。フォールバック値({})を使用します。",
                    FALLBACK_TIMEZONE.getID(), e);
            return FALLBACK_TIMEZONE;
        }
    }

}
