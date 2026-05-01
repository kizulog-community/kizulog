package io.github.kizulog_community.kizulog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;

/**
 * Spring MVC設定
 *
 * <p>カスタムConverterを登録する。</p>
 *
 * @author Jun Kobayashi
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * カスタムFormatter/Converterの登録
     *
     * @param registry レジストリ
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToSupportedTimezoneConverter());
    }

    /**
     * 文字列をSupportedTimezoneに変換するConverter
     */
    private static class StringToSupportedTimezoneConverter
            implements Converter<String, SupportedTimezone> {

        @Override
        public SupportedTimezone convert(String source) {
            if (source == null || source.isBlank()) {
                return null;
            }
            return SupportedTimezone.of(source);
        }
    }

}