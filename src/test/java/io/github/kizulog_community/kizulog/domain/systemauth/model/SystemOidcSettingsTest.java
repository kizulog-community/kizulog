package io.github.kizulog_community.kizulog.domain.systemauth.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SystemOidcSettingsの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemOidcSettingsTest {

    private static final OffsetDateTime VERSION =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    /**
     * テスト用のSystemOidcSettingを生成する
     */
    private SystemOidcSetting sampleSetting(String id) {
        return new SystemOidcSetting(
                id, "https://auth.example/realms/" + id, "client-" + id, "secret-" + id);
    }

    @Test
    @DisplayName("コンストラクタ：全フィールド指定で各getterが値を返す")
    void constructor_withAllFields_returnsAllValues() {
        List<SystemOidcSetting> list = List.of(sampleSetting("master"));

        SystemOidcSettings settings = new SystemOidcSettings(VERSION, list);

        assertThat(settings.getVersion()).isEqualTo(VERSION);
        assertThat(settings.getSettings()).hasSize(1);
        assertThat(settings.getSettings().get(0).getId()).isEqualTo("master");
    }

    @Test
    @DisplayName("コンストラクタ：versionがnullならNullPointerException")
    void constructor_withNullVersion_throwsNpe() {
        assertThatNullPointerException().isThrownBy(
                () -> new SystemOidcSettings(null, List.of()))
                .withMessageContaining("version");
    }

    @Test
    @DisplayName("コンストラクタ：settingsがnullならNullPointerException")
    void constructor_withNullSettings_throwsNpe() {
        assertThatNullPointerException().isThrownBy(
                () -> new SystemOidcSettings(VERSION, null))
                .withMessageContaining("settings");
    }

    @Test
    @DisplayName("コンストラクタ：空リストでも生成できる")
    void constructor_withEmptyList_isAllowed() {
        SystemOidcSettings settings = new SystemOidcSettings(VERSION, List.of());

        assertThat(settings.getSettings()).isEmpty();
    }

    @Test
    @DisplayName("getSettings：返されるListは不変")
    void getSettings_returnsUnmodifiableList() {
        List<SystemOidcSetting> list = List.of(sampleSetting("master"));
        SystemOidcSettings settings = new SystemOidcSettings(VERSION, list);

        assertThat(settings.getSettings()).isUnmodifiable();
    }

    @Test
    @DisplayName("コンストラクタ：渡したリストを後から変更してもsettingsには影響しない")
    void constructor_defensivelyCopiesInputList() {
        List<SystemOidcSetting> mutable = new ArrayList<>();
        mutable.add(sampleSetting("master"));

        SystemOidcSettings settings = new SystemOidcSettings(VERSION, mutable);

        // 元のリストに追加してもsettings側には影響しない
        mutable.add(sampleSetting("other"));

        assertThat(settings.getSettings()).hasSize(1);
    }

}
