package io.github.kizulog_community.kizulog.infrastructure.security.oidc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.kizulog_community.kizulog.config.OidcProfileClaimsProperties;

/**
 * OidcClaimsFilterの単体テスト
 *
 * @author Jun Kobayashi
 */
class OidcClaimsFilterTest {

    private OidcProfileClaimsProperties properties;
    private OidcClaimsFilter sut;

    @BeforeEach
    void setUp() {
        properties = new OidcProfileClaimsProperties();
        // デフォルトのcachedClaimsをそのまま使用
        sut = new OidcClaimsFilter(properties);
    }

    @Test
    @DisplayName("filter: ホワイトリストに含まれるクレームのみが採用される")
    void filter_keepsOnlyWhitelistedClaims() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("name", "山田 太郎");
        raw.put("email", "taro@example.com");
        raw.put("iat", 1700000000L);     // 認証メタ情報→除外
        raw.put("exp", 1700003600L);     // 同上
        raw.put("nonce", "abc123");      // 同上

        Map<String, Object> result = sut.filter(raw);

        assertThat(result).containsKeys("name", "email");
        assertThat(result).doesNotContainKeys("iat", "exp", "nonce");
    }

    @Test
    @DisplayName("filter: null値のクレームは除外される")
    void filter_excludesNullValues() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("name", "山田 太郎");
        raw.put("email", null);
        raw.put("organization", "開発部");

        Map<String, Object> result = sut.filter(raw);

        assertThat(result).containsKeys("name", "organization");
        assertThat(result).doesNotContainKey("email");
    }

    @Test
    @DisplayName("filter: rawClaimsがnullの場合は空Mapを返す")
    void filter_returnsEmptyMap_whenRawClaimsIsNull() {
        Map<String, Object> result = sut.filter(null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("filter: rawClaimsが空の場合は空Mapを返す")
    void filter_returnsEmptyMap_whenRawClaimsIsEmpty() {
        Map<String, Object> result = sut.filter(Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("filter: ホワイトリストが空の場合は空Mapを返す（安全側）")
    void filter_returnsEmptyMap_whenWhitelistIsEmpty() {
        properties.setCachedClaims(List.of());

        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("name", "山田 太郎");
        raw.put("email", "taro@example.com");

        Map<String, Object> result = sut.filter(raw);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("filter: 値の型は加工せずそのまま透過する（String/Boolean/Long/Map）")
    void filter_preservesValueTypes() {
        properties.setCachedClaims(List.of("name", "email_verified", "updated_at", "address"));

        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("name", "山田 太郎");
        raw.put("email_verified", Boolean.TRUE);
        raw.put("updated_at", 1700000000L);
        Map<String, Object> nestedAddress = Map.of("country", "JP", "city", "Tokyo");
        raw.put("address", nestedAddress);

        Map<String, Object> result = sut.filter(raw);

        assertThat(result.get("name")).isInstanceOf(String.class).isEqualTo("山田 太郎");
        assertThat(result.get("email_verified")).isInstanceOf(Boolean.class).isEqualTo(Boolean.TRUE);
        assertThat(result.get("updated_at")).isInstanceOf(Long.class).isEqualTo(1700000000L);
        assertThat(result.get("address")).isInstanceOf(Map.class);
    }

}
