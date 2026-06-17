package io.github.kizulog_community.kizulog.infrastructure.security.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProvider;
import io.github.kizulog_community.kizulog.domain.tenantoidc.port.TenantOidcProviderRepository;
import io.github.kizulog_community.kizulog.domain.tenantoidc.service.ClaimsMappingResolver;

/**
 * TenantProfileClaimsResolver の単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantProfileClaimsResolverTest {

    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);
    private static final String TENANT_ID = "tenant-A";
    private static final String OTHER_TENANT_ID = "tenant-B";
    private static final String ISS = "https://auth.example/realms/main";
    private static final String AUD = "client-aud";

    private TenantOidcProviderRepository providerRepository;
    private TenantProfileClaimsResolver sut;

    @BeforeEach
    void setUp() {
        providerRepository = mock(TenantOidcProviderRepository.class);
        sut = new TenantProfileClaimsResolver(providerRepository, new ClaimsMappingResolver());
    }

    @Test
    @DisplayName("resolveForStorage: マッピング適用済みのターゲットキーMapを返す")
    void resolveForStorage_returnsMappedTargets() {
        Map<String, String> mapping = Map.of(
                "familyName", "fn",
                "givenName", "gn",
                "organization", "org",
                "email", "em");
        when(providerRepository.findAllLatestByIssAndAud(ISS, AUD))
                .thenReturn(List.of(providerWithMapping(TENANT_ID, mapping)));

        Map<String, Object> raw = Map.of(
                "fn", "山田",
                "gn", "太郎",
                "org", "開発部",
                "em", "taro@example.com");

        Map<String, Object> result = sut.resolveForStorage(TENANT_ID, ISS, AUD, raw);

        assertThat(result).containsEntry("familyName", "山田")
                .containsEntry("givenName", "太郎")
                .containsEntry("organization", "開発部")
                .containsEntry("email", "taro@example.com")
                .doesNotContainKey("middleName");
    }

    @Test
    @DisplayName("resolveForStorage: (iss,aud)一致でもtenantId不一致なら空Map")
    void resolveForStorage_returnsEmpty_whenTenantMismatch() {
        when(providerRepository.findAllLatestByIssAndAud(ISS, AUD))
                .thenReturn(List.of(providerWithMapping(OTHER_TENANT_ID, Map.of("familyName", "fn"))));

        Map<String, Object> result =
                sut.resolveForStorage(TENANT_ID, ISS, AUD, Map.of("fn", "山田"));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("resolveForStorage: 入力nullの場合は空Map")
    void resolveForStorage_returnsEmpty_whenInputNull() {
        assertThat(sut.resolveForStorage(null, ISS, AUD, Map.of("fn", "山田"))).isEmpty();
        assertThat(sut.resolveForStorage(TENANT_ID, null, AUD, Map.of("fn", "山田"))).isEmpty();
        assertThat(sut.resolveForStorage(TENANT_ID, ISS, null, Map.of("fn", "山田"))).isEmpty();
        assertThat(sut.resolveForStorage(TENANT_ID, ISS, AUD, null)).isEmpty();
    }

    @Test
    @DisplayName("resolveForStorage: リポジトリ例外でも伝播せず空Map（fail-open）")
    void resolveForStorage_failOpen_whenRepositoryThrows() {
        when(providerRepository.findAllLatestByIssAndAud(ISS, AUD))
                .thenThrow(new RuntimeException("db error"));

        Map<String, Object> result =
                sut.resolveForStorage(TENANT_ID, ISS, AUD, Map.of("fn", "山田"));

        assertThat(result).isEmpty();
    }

    private TenantOidcProvider providerWithMapping(String tenantId, Map<String, String> mapping) {
        return new TenantOidcProvider(
                tenantId, "google", BASE_TIME,
                "Display", ISS, AUD, "client", "enc",
                mapping, BASE_TIME, "creator");
    }

}
