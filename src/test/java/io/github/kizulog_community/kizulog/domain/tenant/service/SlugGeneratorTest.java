package io.github.kizulog_community.kizulog.domain.tenant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SlugGeneratorの単体テスト
 *
 * @author Jun Kobayashi
 */
class SlugGeneratorTest {

    /** slug長: NanoID 21文字 */
    private static final int EXPECTED_LENGTH = 21;

    /** 許可された文字パターン（小文字英数字とハイフン） */
    private static final String VALID_CHARSET_PATTERN = "^[a-z0-9-]+$";

    private SlugGenerator slugGenerator;

    @BeforeEach
    void setUp() {
        slugGenerator = new SlugGenerator();
    }

    @Test
    @DisplayName("generate: 21文字のslugを生成する")
    void generate_returnsSlugOfExpectedLength() {
        String slug = slugGenerator.generate();

        assertThat(slug).hasSize(EXPECTED_LENGTH);
    }

    @Test
    @DisplayName("generate: 文字集合 [a-z0-9-] のみで構成される")
    void generate_returnsSlugWithAllowedCharsetOnly() {
        String slug = slugGenerator.generate();

        assertThat(slug).matches(VALID_CHARSET_PATTERN);
    }

    @Test
    @DisplayName("generate: 連続2回呼び出しで異なる値を返す")
    void generate_returnsDistinctValueOnConsecutiveCalls() {
        String slug1 = slugGenerator.generate();
        String slug2 = slugGenerator.generate();

        assertThat(slug1).isNotEqualTo(slug2);
    }

    @Test
    @DisplayName("generate: 100回呼び出しても全て異なる値を返す（衝突しない）")
    void generate_returnsAllDistinctValuesOnManyCalls() {
        int iterations = 100;
        Set<String> generated = new HashSet<>();

        for (int i = 0; i < iterations; i++) {
            generated.add(slugGenerator.generate());
        }

        assertThat(generated).hasSize(iterations);
    }

    @Test
    @DisplayName("generate: 単発呼び出しで例外を投げない")
    void generate_doesNotThrowAnyException() {
        assertThatCode(() -> slugGenerator.generate()).doesNotThrowAnyException();
    }

}
