package io.github.kizulog_community.kizulog.domain.systemauth.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SystemOidcSettingの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemOidcSettingTest {

    private static final String ID = "master";
    private static final String URI = "https://auth.dev.internal/realms/master";
    private static final String CLIENT_ID = "kizulog-master";
    private static final String CLIENT_SECRET = "decrypted-secret-value";

    @Test
    @DisplayName("コンストラクタ：全フィールド指定で各getterが値を返す")
    void constructor_withAllFields_returnsAllValues() {
        SystemOidcSetting setting = new SystemOidcSetting(
                ID, URI, CLIENT_ID, CLIENT_SECRET);

        assertThat(setting.getId()).isEqualTo(ID);
        assertThat(setting.getUri()).isEqualTo(URI);
        assertThat(setting.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(setting.getClientSecret()).isEqualTo(CLIENT_SECRET);
    }

    @Test
    @DisplayName("コンストラクタ：idがnullならNullPointerException")
    void constructor_withNullId_throwsNpe() {
        assertThatNullPointerException().isThrownBy(
                () -> new SystemOidcSetting(null, URI, CLIENT_ID, CLIENT_SECRET))
                .withMessageContaining("id");
    }

    @Test
    @DisplayName("コンストラクタ：uriがnullならNullPointerException")
    void constructor_withNullUri_throwsNpe() {
        assertThatNullPointerException().isThrownBy(
                () -> new SystemOidcSetting(ID, null, CLIENT_ID, CLIENT_SECRET))
                .withMessageContaining("uri");
    }

    @Test
    @DisplayName("コンストラクタ：clientIdがnullならNullPointerException")
    void constructor_withNullClientId_throwsNpe() {
        assertThatNullPointerException().isThrownBy(
                () -> new SystemOidcSetting(ID, URI, null, CLIENT_SECRET))
                .withMessageContaining("clientId");
    }

    @Test
    @DisplayName("コンストラクタ：clientSecretがnullならNullPointerException")
    void constructor_withNullClientSecret_throwsNpe() {
        assertThatNullPointerException().isThrownBy(
                () -> new SystemOidcSetting(ID, URI, CLIENT_ID, null))
                .withMessageContaining("clientSecret");
    }

    @Test
    @DisplayName("toString：clientSecretの値が含まれない")
    void toString_doesNotIncludeClientSecret() {
        SystemOidcSetting setting = new SystemOidcSetting(
                ID, URI, CLIENT_ID, CLIENT_SECRET);

        assertThat(setting.toString()).doesNotContain(CLIENT_SECRET);
    }

    @Test
    @DisplayName("toString：clientSecret以外のフィールド値は含まれる")
    void toString_includesOtherFields() {
        SystemOidcSetting setting = new SystemOidcSetting(
                ID, URI, CLIENT_ID, CLIENT_SECRET);

        assertThat(setting.toString())
                .contains(ID)
                .contains(URI)
                .contains(CLIENT_ID);
    }

}
