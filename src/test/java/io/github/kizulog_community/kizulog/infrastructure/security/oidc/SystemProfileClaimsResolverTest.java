package io.github.kizulog_community.kizulog.infrastructure.security.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.kizulog_community.kizulog.domain.systemoidc.model.ClaimsMappingTarget;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProvider;
import io.github.kizulog_community.kizulog.domain.systemoidc.port.SystemOidcProviderRepository;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.ClaimsMappingResolver;

/**
 * SystemProfileClaimsResolver の単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemProfileClaimsResolverTest {

    private static final String ISS = "https://example.com";
    private static final String PROVIDER_ID = "test-provider";
    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private SystemOidcProviderRepository providerRepository;
    private SystemProfileClaimsResolver sut;

    @BeforeEach
    void setUp() {
        providerRepository = mock(SystemOidcProviderRepository.class);
        // マッピング解決は純ロジックのため実体を使用
        sut = new SystemProfileClaimsResolver(providerRepository, new ClaimsMappingResolver());
    }

    @Test
    @DisplayName("resolveForStorage: デフォルトマッピングで5項目がターゲットキーで解決される")
    void resolveForStorage_resolvesWithDefaultMapping() {
        SystemOidcProvider provider = providerWithMapping(ClaimsMappingTarget.defaultMapping());
        when(providerRepository.findAllLatest()).thenReturn(List.of(provider));

        Map<String, Object> raw = Map.of(
                "family_name", "山田",
                "given_name", "太郎",
                "organization", "開発部",
                "email", "taro@example.com");

        Map<String, Object> result = sut.resolveForStorage(ISS, raw);

        assertThat(result).containsEntry("familyName", "山田")
                .containsEntry("givenName", "太郎")
                .containsEntry("organization", "開発部")
                .containsEntry("email", "taro@example.com")
                .doesNotContainKey("middleName");
    }

    @Test
    @DisplayName("resolveForStorage: カスタムマッピングが反映され標準キーは無視される")
    void resolveForStorage_respectsCustomMapping() {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("familyName", "surname");
        mapping.put("givenName", "first_name");
        mapping.put("organization", "dept");
        mapping.put("email", "mail");
        when(providerRepository.findAllLatest()).thenReturn(List.of(providerWithMapping(mapping)));

        Map<String, Object> raw = Map.of(
                "surname", "山田",
                "first_name", "太郎",
                "dept", "営業部",
                "mail", "taro@example.com",
                "family_name", "別人");

        Map<String, Object> result = sut.resolveForStorage(ISS, raw);

        assertThat(result).containsEntry("familyName", "山田")
                .containsEntry("givenName", "太郎")
                .containsEntry("organization", "営業部")
                .containsEntry("email", "taro@example.com");
    }

    @Test
    @DisplayName("resolveForStorage: マッピング未設定・クレーム欠落の属性は含まれない")
    void resolveForStorage_omitsUnmappedAndAbsent() {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("familyName", "family_name");
        mapping.put("givenName", "given_name");
        when(providerRepository.findAllLatest()).thenReturn(List.of(providerWithMapping(mapping)));

        // given_name は欠落、organization/email はマッピングなし
        Map<String, Object> raw = Map.of(
                "family_name", "山田",
                "organization", "開発部",
                "email", "taro@example.com");

        Map<String, Object> result = sut.resolveForStorage(ISS, raw);

        assertThat(result).containsEntry("familyName", "山田")
                .doesNotContainKey("givenName")
                .doesNotContainKey("organization")
                .doesNotContainKey("email")
                .doesNotContainKey("middleName");
    }

    @Test
    @DisplayName("resolveForStorage: 末尾スラッシュ揺れがあってもプロバイダが一致する")
    void resolveForStorage_matchesProvider_withTrailingSlashDifference() {
        SystemOidcProvider provider = new SystemOidcProvider(
                PROVIDER_ID, BASE_TIME, "Test Provider", "https://example.com/",
                "client-1", "secret", ClaimsMappingTarget.defaultMapping(), BASE_TIME, "system");
        when(providerRepository.findAllLatest()).thenReturn(List.of(provider));

        Map<String, Object> result = sut.resolveForStorage(ISS, Map.of("family_name", "山田"));

        assertThat(result).containsEntry("familyName", "山田");
    }

    @Test
    @DisplayName("resolveForStorage: プロバイダが見つからない場合は空Map")
    void resolveForStorage_returnsEmpty_whenProviderNotFound() {
        SystemOidcProvider other = new SystemOidcProvider(
                "other", BASE_TIME, "Other", "https://other.example.com",
                "client-1", "secret", ClaimsMappingTarget.defaultMapping(), BASE_TIME, "system");
        when(providerRepository.findAllLatest()).thenReturn(List.of(other));

        Map<String, Object> result = sut.resolveForStorage(ISS, Map.of("family_name", "山田"));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("resolveForStorage: iss/クレームがnullの場合は空Map")
    void resolveForStorage_returnsEmpty_whenInputNull() {
        assertThat(sut.resolveForStorage(null, Map.of("family_name", "山田"))).isEmpty();
        assertThat(sut.resolveForStorage(ISS, null)).isEmpty();
    }

    @Test
    @DisplayName("resolveForStorage: リポジトリ例外でも伝播せず空Map（fail-open）")
    void resolveForStorage_failOpen_whenRepositoryThrows() {
        when(providerRepository.findAllLatest()).thenThrow(new RuntimeException("db error"));

        Map<String, Object> result = sut.resolveForStorage(ISS, Map.of("family_name", "山田"));

        assertThat(result).isEmpty();
    }

    private SystemOidcProvider providerWithMapping(Map<String, String> mapping) {
        return new SystemOidcProvider(
                PROVIDER_ID, BASE_TIME, "Test Provider", ISS,
                "client-1", "secret", mapping, BASE_TIME, "system");
    }

}
