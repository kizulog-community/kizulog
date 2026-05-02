package io.github.kizulog_community.kizulog.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SupportedTimezoneの単体テスト.
 *
 * @author Jun Kobayashi
 */
class SupportedTimezoneTest {

    @Test
    @DisplayName("of()でAsia/Tokyoを生成できる")
    void of_asiaTokyo_returnsInstance() {
        SupportedTimezone tz = SupportedTimezone.of("Asia/Tokyo");
        assertThat(tz).isNotNull();
        assertThat(tz.getId()).isEqualTo("Asia/Tokyo");
    }

    @Test
    @DisplayName("of()のZoneIdはZoneId.of()と等価")
    void of_returnsSameZoneId() {
        SupportedTimezone tz = SupportedTimezone.of("Asia/Tokyo");
        assertThat(tz.getZoneId()).isEqualTo(ZoneId.of("Asia/Tokyo"));
    }

    @Test
    @DisplayName("of()の表示名はID + UTCオフセット形式")
    void of_displayName_includesIdAndOffset() {
        SupportedTimezone tz = SupportedTimezone.of("Asia/Tokyo");
        assertThat(tz.getDisplayName())
                .startsWith("Asia/Tokyo")
                .contains("UTC+09:00");
    }

    @Test
    @DisplayName("of()に不正なIDを渡すと例外がスローされる")
    void of_invalidId_throwsException() {
        assertThatThrownBy(() -> SupportedTimezone.of("Invalid/Timezone"))
                .isInstanceOf(java.time.zone.ZoneRulesException.class);
    }

    @Test
    @DisplayName("name()はIDと等しい値を返す")
    void name_returnsId() {
        SupportedTimezone tz = SupportedTimezone.of("Asia/Tokyo");
        assertThat(tz.name()).isEqualTo("Asia/Tokyo");
    }

    @Test
    @DisplayName("findById()で有効なIDを渡すとOptional.of()を返す")
    void findById_validId_returnsOptional() {
        Optional<SupportedTimezone> result = SupportedTimezone.findById("Asia/Tokyo");
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("Asia/Tokyo");
    }

    @Test
    @DisplayName("findById()でnullを渡すと空のOptionalを返す")
    void findById_null_returnsEmpty() {
        assertThat(SupportedTimezone.findById(null)).isEmpty();
    }

    @Test
    @DisplayName("findById()で不正なIDを渡すと空のOptionalを返す")
    void findById_invalidId_returnsEmpty() {
        assertThat(SupportedTimezone.findById("Invalid/Timezone")).isEmpty();
    }

    @Test
    @DisplayName("values()は空でないリストを返す")
    void values_returnsNonEmptyList() {
        List<SupportedTimezone> list = SupportedTimezone.values();
        assertThat(list).isNotEmpty();
    }

    @Test
    @DisplayName("values()はAsia/Tokyoを含む")
    void values_containsAsiaTokyo() {
        List<SupportedTimezone> list = SupportedTimezone.values();
        assertThat(list).extracting(SupportedTimezone::getId)
                .contains("Asia/Tokyo");
    }

    @Test
    @DisplayName("values()はUTCオフセット昇順でソートされている")
    void values_sortedByOffsetAscending() {
        List<SupportedTimezone> list = SupportedTimezone.values();
        for (int i = 1; i < list.size(); i++) {
            int prev = list.get(i - 1).getZoneId().getRules()
                    .getOffset(java.time.Instant.now()).getTotalSeconds();
            int curr = list.get(i).getZoneId().getRules()
                    .getOffset(java.time.Instant.now()).getTotalSeconds();
            assertThat(curr).isGreaterThanOrEqualTo(prev);
        }
    }

    @Test
    @DisplayName("equalsは同一IDで真を返す")
    void equals_sameId_returnsTrue() {
        SupportedTimezone tz1 = SupportedTimezone.of("Asia/Tokyo");
        SupportedTimezone tz2 = SupportedTimezone.of("Asia/Tokyo");
        assertThat(tz1).isEqualTo(tz2);
    }

    @Test
    @DisplayName("equalsは異なるIDで偽を返す")
    void equals_differentId_returnsFalse() {
        SupportedTimezone tz1 = SupportedTimezone.of("Asia/Tokyo");
        SupportedTimezone tz2 = SupportedTimezone.of("America/New_York");
        assertThat(tz1).isNotEqualTo(tz2);
    }

    @Test
    @DisplayName("hashCodeは同一IDで同じ値")
    void hashCode_sameId_returnsSameValue() {
        SupportedTimezone tz1 = SupportedTimezone.of("Asia/Tokyo");
        SupportedTimezone tz2 = SupportedTimezone.of("Asia/Tokyo");
        assertThat(tz1.hashCode()).isEqualTo(tz2.hashCode());
    }

}