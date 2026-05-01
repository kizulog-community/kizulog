package io.github.kizulog_community.kizulog.domain.shared;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * サポートタイムゾーンクラス
 *
 * <p>IANA タイムゾーンデータベースに登録されている全タイムゾーンをサポートする。</p>
 *
 * <p>表示名は「Asia/Tokyo (UTC+09:00)」形式で、UTCオフセット昇順、
 * オフセットが同じ場合はID昇順でソートする。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@EqualsAndHashCode(of = "id")
public class SupportedTimezone {

    /** タイムゾーンID（IANA準拠：例 "Asia/Tokyo"） */
    private final String id;

    /** ZoneId */
    private final ZoneId zoneId;

    /** 表示名（例："Asia/Tokyo (UTC+09:00)"） */
    private final String displayName;

    /**
     * コンストラクタ
     *
     * @param id IANA タイムゾーンID
     */
    private SupportedTimezone(String id) {
        this.id = id;
        this.zoneId = ZoneId.of(id);
        this.displayName = buildDisplayName(id, this.zoneId);
    }

    /**
     * 表示名を生成する。
     *
     * @param id タイムゾーンID
     * @param zoneId ZoneId
     * @return 表示名（例："Asia/Tokyo (UTC+09:00)"）
     */
    private static String buildDisplayName(String id, ZoneId zoneId) {
        String offset = ZonedDateTime.now(zoneId)
                .format(DateTimeFormatter.ofPattern("XXX"));
        return id + " (UTC" + offset + ")";
    }

    /**
     * 指定IDのSupportedTimezoneを返す。
     *
     * @param id タイムゾーンID
     * @return SupportedTimezone
     * @throws java.time.zone.ZoneRulesException 不正なIDの場合
     */
    public static SupportedTimezone of(String id) {
        return new SupportedTimezone(id);
    }

    /**
     * 指定IDのSupportedTimezoneを取得する。
     *
     * @param id タイムゾーンID
     * @return SupportedTimezone（存在しない場合は空）
     */
    public static Optional<SupportedTimezone> findById(String id) {
        if (id == null || !ZoneId.getAvailableZoneIds().contains(id)) {
            return Optional.empty();
        }
        return Optional.of(new SupportedTimezone(id));
    }

    /**
     * サポートする全タイムゾーンのリストを返す。
     *
     * <p>UTCオフセット昇順、オフセットが同じ場合はID昇順でソートする。</p>
     *
     * @return 全タイムゾーンリスト
     */
    public static List<SupportedTimezone> values() {
        return ZoneId.getAvailableZoneIds().stream()
                .map(SupportedTimezone::new)
                .sorted(Comparator
                        .comparing((SupportedTimezone tz) ->
                                tz.zoneId.getRules()
                                        .getOffset(java.time.Instant.now())
                                        .getTotalSeconds())
                        .thenComparing(SupportedTimezone::getId))
                .toList();
    }

    /**
     * enum互換のname()メソッド
     *
     * <p>互換性のためIDをそのまま返す。</p>
     *
     * @return タイムゾーンID
     */
    public String name() {
        return id;
    }

}