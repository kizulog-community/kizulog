package io.github.kizulog_community.kizulog.infrastructure.web.converter;

import java.time.zone.ZoneRulesException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;

/**
 * 文字列をSupportedTimezoneに変換するSpring MVC Converter
 *
 * <p>変換ルール:
 * <ul>
 * <li>null / 空白文字列 → null（フォーム未入力扱い）</li>
 * <li>有効なIANA タイムゾーンID → SupportedTimezone インスタンス</li>
 * <li>不正なID → ZoneRulesException（呼び出し側でハンドリング）</li>
 * </ul>
 * </p>
 *
 * @author Jun Kobayashi
 */
@Component
public class StringToSupportedTimezoneConverter
        implements Converter<String, SupportedTimezone> {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(StringToSupportedTimezoneConverter.class);

    /**
     * 文字列を SupportedTimezone に変換する。
     *
     * @param source タイムゾーンID文字列
     * @return SupportedTimezoneインスタンス、null/空白の場合はnull
     * @throws ZoneRulesException 不正なIDの場合
     */
    @Override
    public SupportedTimezone convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        try {
            return SupportedTimezone.of(source);
        } catch (ZoneRulesException e) {
            log.warn("不正なタイムゾーンIDを変換しようとしました: source={}", source);
            throw e;
        }
    }

}
