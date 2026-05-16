package io.github.kizulog_community.kizulog.domain.tenant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * HostNormalizerの単体テスト
 *
 * @author Jun Kobayashi
 */
class HostNormalizerTest {

    private HostNormalizer hostNormalizer;

    @BeforeEach
    void setUp() {
        hostNormalizer = new HostNormalizer();
    }

    @Test
    @DisplayName("normalizeAndValidate: 単純なホスト名はそのまま返す")
    void normalizeAndValidate_returnsSimpleHostAsIs() {
        String result = hostNormalizer.normalizeAndValidate("example.com");

        assertThat(result).isEqualTo("example.com");
    }

    @Test
    @DisplayName("normalizeAndValidate: 大文字混在は小文字化される")
    void normalizeAndValidate_lowercasesUppercaseCharacters() {
        String result = hostNormalizer.normalizeAndValidate("Example.Com");

        assertThat(result).isEqualTo("example.com");
    }

    @Test
    @DisplayName("normalizeAndValidate: 前後空白はtrimされる")
    void normalizeAndValidate_trimsLeadingAndTrailingWhitespace() {
        String result = hostNormalizer.normalizeAndValidate("  example.com  ");

        assertThat(result).isEqualTo("example.com");
    }

    @Test
    @DisplayName("normalizeAndValidate: 複数階層サブドメインを許容する")
    void normalizeAndValidate_acceptsMultipleLevelSubdomain() {
        String result = hostNormalizer.normalizeAndValidate("a.b.c.example.com");

        assertThat(result).isEqualTo("a.b.c.example.com");
    }

    @Test
    @DisplayName("normalizeAndValidate: ラベル内のハイフンを許容する")
    void normalizeAndValidate_acceptsHyphenInsideLabel() {
        String result = hostNormalizer.normalizeAndValidate("my-host.example.com");

        assertThat(result).isEqualTo("my-host.example.com");
    }

    @Test
    @DisplayName("normalizeAndValidate: 数字のみのラベルを許容する（RFC1123）")
    void normalizeAndValidate_acceptsNumericOnlyLabel() {
        String result = hostNormalizer.normalizeAndValidate("123.example.com");

        assertThat(result).isEqualTo("123.example.com");
    }

    @Test
    @DisplayName("normalizeAndValidate: null入力でIllegalArgumentExceptionをスローする")
    void normalizeAndValidate_throwsOnNullInput() {
        assertThatThrownBy(() -> hostNormalizer.normalizeAndValidate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    @DisplayName("normalizeAndValidate: 空文字でIllegalArgumentExceptionをスローする")
    void normalizeAndValidate_throwsOnEmptyInput() {
        assertThatThrownBy(() -> hostNormalizer.normalizeAndValidate(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("normalizeAndValidate: 空白のみの入力でIllegalArgumentExceptionをスローする")
    void normalizeAndValidate_throwsOnWhitespaceOnlyInput() {
        assertThatThrownBy(() -> hostNormalizer.normalizeAndValidate("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("normalizeAndValidate: スキーマ含むhostでIllegalArgumentExceptionをスローする")
    void normalizeAndValidate_throwsWhenInputContainsScheme() {
        assertThatThrownBy(() ->
                hostNormalizer.normalizeAndValidate("https://example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scheme");
    }

    @Test
    @DisplayName("normalizeAndValidate: ポート番号含むhostでIllegalArgumentExceptionをスローする")
    void normalizeAndValidate_throwsWhenInputContainsPort() {
        assertThatThrownBy(() ->
                hostNormalizer.normalizeAndValidate("example.com:8080"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("port");
    }

    @Test
    @DisplayName("normalizeAndValidate: ハイフンで始まるラベルでIllegalArgumentExceptionをスローする")
    void normalizeAndValidate_throwsOnLabelStartingWithHyphen() {
        assertThatThrownBy(() ->
                hostNormalizer.normalizeAndValidate("-bad.example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RFC1123");
    }

    @Test
    @DisplayName("normalizeAndValidate: ハイフンで終わるラベルでIllegalArgumentExceptionをスローする")
    void normalizeAndValidate_throwsOnLabelEndingWithHyphen() {
        assertThatThrownBy(() ->
                hostNormalizer.normalizeAndValidate("bad-.example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RFC1123");
    }

    @Test
    @DisplayName("normalizeAndValidate: 254文字以上でIllegalArgumentExceptionをスローする")
    void normalizeAndValidate_throwsWhenLengthExceedsMax() {
        // 254文字のホスト名を生成（253文字までOKなので254はNG）
        // 例: aaa...a.com で全体254文字
        StringBuilder sb = new StringBuilder();
        // 60文字ラベル × 4 + ドット3 = 60*4 + 3 = 243
        // 残り11文字: ラベル「.aaaaaaaaa」（ドット1 + 9文字）で1ラベル追加 → 252文字
        // さらに「.aa」で計255文字。254以上を作るシンプルな方法は1ラベル長文字に。
        // 一番楽なのは label×4 + ドット = 254文字: 各label = 63文字、ドット3
        // 63*4 = 252 + 3 = 255文字
        // → 254文字を作るには: 63*4 - 1 + 3 = 254 → 63+63+63+62 + 3ドット = 254
        sb.append("a".repeat(63)).append(".");
        sb.append("a".repeat(63)).append(".");
        sb.append("a".repeat(63)).append(".");
        sb.append("a".repeat(62));
        // length = 64+64+64+62 = 254
        String tooLong = sb.toString();
        assertThat(tooLong).hasSize(254);

        assertThatThrownBy(() ->
                hostNormalizer.normalizeAndValidate(tooLong))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
