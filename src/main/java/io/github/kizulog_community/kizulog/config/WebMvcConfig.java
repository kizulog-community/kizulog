package io.github.kizulog_community.kizulog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.github.kizulog_community.kizulog.infrastructure.web.converter.StringToSupportedTimezoneConverter;
import lombok.RequiredArgsConstructor;

/**
 * Spring MVC設定
 *
 * <p>カスタムConverterを登録する。</p>
 *
 * @author Jun Kobayashi
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    /** 文字列→SupportedTimezone変換Converter */
    private final StringToSupportedTimezoneConverter stringToSupportedTimezoneConverter;

    /**
     * カスタムFormatter/Converterの登録
     *
     * @param registry レジストリ
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToSupportedTimezoneConverter);
    }

}
