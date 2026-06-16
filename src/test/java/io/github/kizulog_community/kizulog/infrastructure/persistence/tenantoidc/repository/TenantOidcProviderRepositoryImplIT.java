package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantoidc.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;

import io.github.kizulog_community.kizulog.domain.tenantoidc.model.ClaimsMappingTarget;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProvider;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantoidc.entity.TenantOidcProviderEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantoidc.entity.TenantOidcProviderId;

/**
 * TenantOidcProviderRepositoryImpl の統合テスト
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TenantOidcProviderRepositoryImpl.class)
class TenantOidcProviderRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private TenantOidcProviderRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private TenantOidcProviderJpaRepository jpaRepository;

    /** テスト用基準時刻（UTC） */
    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private static final String TENANT_A = "tenant-A";
    private static final String TENANT_B = "tenant-B";

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    /**
     * テストエンティティ保存ヘルパー
     */
    private void saveEntity(
            String tenantId, String providerId, OffsetDateTime version,
            String displayName, String iss, String aud,
            String clientId, String clientSecret, String createdBy) {
        TenantOidcProviderEntity entity = new TenantOidcProviderEntity(
                new TenantOidcProviderId(tenantId, providerId, version),
                displayName, iss, aud, clientId, clientSecret,
                ClaimsMappingTarget.defaultMapping(),
                version, createdBy);
        jpaRepository.save(entity);
    }

    @Test
    @DisplayName("findLatestByTenantIdAndProviderId: 同一(tenantId, providerId)で複数versionが存在する場合、最大versionを返す")
    void findLatestByTenantIdAndProviderId_returnsLatestVersion() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity(TENANT_A, "google", v1, "Google v1", "https://iss-1", "aud-1",
                "cid-1", "sec-1", "user:1");
        saveEntity(TENANT_A, "google", v3, "Google v3", "https://iss-3", "aud-3",
                "cid-3", "sec-3", "user:3");
        saveEntity(TENANT_A, "google", v2, "Google v2", "https://iss-2", "aud-2",
                "cid-2", "sec-2", "user:2");

        Optional<TenantOidcProvider> result =
                sut.findLatestByTenantIdAndProviderId(TENANT_A, "google");

        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo(v3);
        assertThat(result.get().getDisplayName()).isEqualTo("Google v3");
        assertThat(result.get().getClientId()).isEqualTo("cid-3");
    }

    @Test
    @DisplayName("findLatestByTenantIdAndProviderId: 該当レコードが存在しない場合、空Optionalを返す")
    void findLatestByTenantIdAndProviderId_returnsEmpty_whenNotFound() {
        saveEntity(TENANT_A, "google", BASE_TIME, "Google", "https://iss", "aud",
                "cid", "sec", "user");

        Optional<TenantOidcProvider> result =
                sut.findLatestByTenantIdAndProviderId(TENANT_A, "missing");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByTenantIdAndProviderId: 異なるtenantのprovider_idは別物として扱われる")
    void findLatestByTenantIdAndProviderId_isolatesPerTenant() {
        OffsetDateTime now = BASE_TIME;
        saveEntity(TENANT_A, "google", now, "A Google", "https://iss", "aud",
                "cid-a", "sec-a", "user-a");
        saveEntity(TENANT_B, "google", now, "B Google", "https://iss", "aud",
                "cid-b", "sec-b", "user-b");

        Optional<TenantOidcProvider> a = sut.findLatestByTenantIdAndProviderId(TENANT_A, "google");
        Optional<TenantOidcProvider> b = sut.findLatestByTenantIdAndProviderId(TENANT_B, "google");

        assertThat(a).isPresent();
        assertThat(a.get().getDisplayName()).isEqualTo("A Google");
        assertThat(b).isPresent();
        assertThat(b.get().getDisplayName()).isEqualTo("B Google");
    }

    @Test
    @DisplayName("findAllLatestByTenantId: テナント内の複数プロバイダーで、各providerIdの最新versionが返る")
    void findAllLatestByTenantId_returnsLatestPerProviderId() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        // google: v1, v2
        saveEntity(TENANT_A, "google", v1, "Google v1", "https://google.iss", "aud-g",
                "cid-g1", "sec", "user");
        saveEntity(TENANT_A, "google", v2, "Google v2", "https://google.iss", "aud-g",
                "cid-g2", "sec", "user");
        // azure: v1のみ
        saveEntity(TENANT_A, "azure", v1, "Azure v1", "https://azure.iss", "aud-a",
                "cid-a", "sec", "user");

        List<TenantOidcProvider> result = sut.findAllLatestByTenantId(TENANT_A);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TenantOidcProvider::getProviderId)
                .containsExactlyInAnyOrder("google", "azure");
        // google は v2 のレコード
        TenantOidcProvider google = result.stream()
                .filter(p -> p.getProviderId().equals("google"))
                .findFirst().orElseThrow();
        assertThat(google.getClientId()).isEqualTo("cid-g2");
    }

    @Test
    @DisplayName("findAllLatestByTenantId: 該当tenantに何もない場合、空リストを返す")
    void findAllLatestByTenantId_returnsEmpty_whenNoData() {
        saveEntity(TENANT_B, "google", BASE_TIME, "B Google", "https://iss", "aud",
                "cid", "sec", "user");

        List<TenantOidcProvider> result = sut.findAllLatestByTenantId(TENANT_A);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllLatestByTenantId: 他テナントのプロバイダーは含まれない")
    void findAllLatestByTenantId_excludesOtherTenants() {
        saveEntity(TENANT_A, "google", BASE_TIME, "A Google", "https://iss", "aud",
                "cid", "sec", "user");
        saveEntity(TENANT_B, "google", BASE_TIME, "B Google", "https://iss", "aud",
                "cid", "sec", "user");
        saveEntity(TENANT_B, "azure", BASE_TIME, "B Azure", "https://iss-2", "aud-2",
                "cid", "sec", "user");

        List<TenantOidcProvider> result = sut.findAllLatestByTenantId(TENANT_A);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTenantId()).isEqualTo(TENANT_A);
        assertThat(result.get(0).getDisplayName()).isEqualTo("A Google");
    }

    @Test
    @DisplayName("findAllLatestByIssAndAud: 同一(iss, aud)を複数テナントが持つ場合、全テナント分を返す")
    void findAllLatestByIssAndAud_returnsAcrossTenants() {
        String iss = "https://shared-idp.example/realms/master";
        String aud = "shared-aud";
        saveEntity(TENANT_A, "shared", BASE_TIME, "A Shared", iss, aud,
                "cid-a", "sec-a", "user-a");
        saveEntity(TENANT_B, "shared", BASE_TIME, "B Shared", iss, aud,
                "cid-b", "sec-b", "user-b");
        // 別 (iss, aud) のプロバイダー（マッチしない）
        saveEntity(TENANT_A, "other", BASE_TIME, "A Other",
                "https://other.iss", "other-aud", "cid", "sec", "user");

        List<TenantOidcProvider> result = sut.findAllLatestByIssAndAud(iss, aud);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TenantOidcProvider::getTenantId)
                .containsExactlyInAnyOrder(TENANT_A, TENANT_B);
    }

    @Test
    @DisplayName("findAllLatestByIssAndAud: 同一(tenantId, providerId)の複数versionがある場合、最新版のみ返す")
    void findAllLatestByIssAndAud_returnsLatestPerProvider() {
        String iss = "https://idp.example";
        String aud = "aud";
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        saveEntity(TENANT_A, "google", v1, "Google v1", iss, aud, "cid-1", "sec", "u");
        saveEntity(TENANT_A, "google", v2, "Google v2", iss, aud, "cid-2", "sec", "u");

        List<TenantOidcProvider> result = sut.findAllLatestByIssAndAud(iss, aud);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVersion()).isEqualTo(v2);
        assertThat(result.get(0).getDisplayName()).isEqualTo("Google v2");
    }

    @Test
    @DisplayName("findAllLatestByIssAndAud: 該当が無い場合、空リストを返す")
    void findAllLatestByIssAndAud_returnsEmpty_whenNotFound() {
        saveEntity(TENANT_A, "google", BASE_TIME, "Google", "https://iss-x", "aud-x",
                "cid", "sec", "user");

        List<TenantOidcProvider> result = sut.findAllLatestByIssAndAud(
                "https://other", "other-aud");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllLatestByIssAndAud: issは一致するがaudが異なる場合、結果に含まれない")
    void findAllLatestByIssAndAud_excludesWhenAudDiffers() {
        String iss = "https://shared.iss";
        saveEntity(TENANT_A, "google", BASE_TIME, "A Google", iss, "aud-a",
                "cid", "sec", "user");
        saveEntity(TENANT_A, "azure", BASE_TIME, "A Azure", iss, "aud-b",
                "cid", "sec", "user");

        List<TenantOidcProvider> result = sut.findAllLatestByIssAndAud(iss, "aud-a");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProviderId()).isEqualTo("google");
    }

    @Test
    @DisplayName("existsByTenantIdAndProviderId: 過去version含めて存在する場合、trueを返す")
    void existsByTenantIdAndProviderId_returnsTrue_whenExists() {
        saveEntity(TENANT_A, "google", BASE_TIME, "Google", "https://iss", "aud",
                "cid", "sec", "user");

        boolean result = sut.existsByTenantIdAndProviderId(TENANT_A, "google");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByTenantIdAndProviderId: 該当無し or 異なるテナントにのみ存在の場合、falseを返す")
    void existsByTenantIdAndProviderId_returnsFalse_whenOnlyInOtherTenant() {
        saveEntity(TENANT_B, "google", BASE_TIME, "B Google", "https://iss", "aud",
                "cid", "sec", "user");

        boolean result = sut.existsByTenantIdAndProviderId(TENANT_A, "google");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("save: 新規エンティティが永続化される")
    void save_persistsNewEntity() {
        TenantOidcProvider provider = new TenantOidcProvider(
                TENANT_A, "new-provider", BASE_TIME,
                "New Display", "https://new.iss", "new-aud",
                "new-cid", "new-encrypted-sec",
                ClaimsMappingTarget.defaultMapping(),
                BASE_TIME, "creator");

        sut.save(provider);

        Optional<TenantOidcProvider> saved =
                sut.findLatestByTenantIdAndProviderId(TENANT_A, "new-provider");
        assertThat(saved).isPresent();
        assertThat(saved.get().getDisplayName()).isEqualTo("New Display");
        assertThat(saved.get().getClientSecret()).isEqualTo("new-encrypted-sec");
    }

    @Test
    @DisplayName("save: 同一(tenantId, providerId)で異なるversionを保存すると別行として共存し、最新版が取得される")
    void save_keepsBothVersions_whenSameTenantProviderDifferentVersion() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        sut.save(new TenantOidcProvider(
                TENANT_A, "google", v1, "Google v1", "https://iss", "aud",
                "cid-1", "sec-1", ClaimsMappingTarget.defaultMapping(), v1, "creator-1"));
        sut.save(new TenantOidcProvider(
                TENANT_A, "google", v2, "Google v2", "https://iss", "aud",
                "cid-2", "sec-2", ClaimsMappingTarget.defaultMapping(), v2, "creator-2"));

        // 両 version が存在
        assertThat(jpaRepository.findAll()).hasSize(2);
        // findLatest は v2 を返す
        Optional<TenantOidcProvider> latest =
                sut.findLatestByTenantIdAndProviderId(TENANT_A, "google");
        assertThat(latest).isPresent();
        assertThat(latest.get().getVersion()).isEqualTo(v2);
        assertThat(latest.get().getClientId()).isEqualTo("cid-2");
    }

}