package io.github.kizulog_community.kizulog.domain.shared;

import java.time.ZoneId;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * サポートタイムゾーン列挙型
 *
 * @author Jun Kobayashi
 */
@Getter
@RequiredArgsConstructor
public enum SupportedTimezone {

    /** 日本標準時. */
	ASIA_TOKYO(ZoneId.of("Asia/Tokyo"), "日本標準時 (JST)");

    /** タイムゾーンID（Java標準・IANA準拠） */
    private final ZoneId zoneId;

    /** 表示名 */
    private final String displayName;

}