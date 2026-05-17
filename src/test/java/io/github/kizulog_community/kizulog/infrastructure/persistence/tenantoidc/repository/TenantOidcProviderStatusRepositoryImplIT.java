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

import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderStatus;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderStatusValue;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantoidc.entity.TenantOidcProviderStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantoidc.entity.TenantOidcProviderStatusId;

/**
 * TenantOidcProviderStatusRepositoryImpl の統合テスト
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TenantOidcProviderStatusRepositoryImpl.class)
class TenantOidcProviderStatusRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private TenantOidcProviderStatusRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private TenantOidcProviderStatusJpaRepository jpaRepository;

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
            TenantOidcProviderStatusValue status, String reason, String createdBy) {
        TenantOidcProviderStatusEntity entity = new TenantOidcProviderStatusEntity(
                new TenantOidcProviderStatusId(tenantId, providerId, version),
                status, reason, version, createdBy);
        jpaRepository.save(entity);
    }

    @Test
    @DisplayName("findLatestByTenantIdAndProviderId: 同一(tenantId, providerId)で複数versionが存在する場合、最大versionのレコードを返す")
    void findLatestByTenantIdAndProviderId_returnsLatestVersion() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity(TENANT_A, "google", v1,
                TenantOidcProviderStatusValue.ENABLED, "initial", "user:1");
        saveEntity(TENANT_A, "google", v3,
                TenantOidcProviderStatusValue.ENABLED, "re-enabled", "user:3");
        saveEntity(TENANT_A, "google", v2,
                TenantOidcProviderStatusValue.DISABLED, "maintenance", "user:2");

        Optional<TenantOidcProviderStatus> result =
                sut.findLatestByTenantIdAndProviderId(TENANT_A, "google");

        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo(v3);
        assertThat(result.get().getStatus())
                .isEqualTo(TenantOidcProviderStatusValue.ENABLED);
        assertThat(result.get().getReason()).isEqualTo("re-enabled");
        assertThat(result.get().getCreatedBy()).isEqualTo("user:3");
    }

    @Test
    @DisplayName("findLatestByTenantIdAndProviderId: 該当レコードが存在しない場合、空Optionalを返す")
    void findLatestByTenantIdAndProviderId_returnsEmpty_whenNotFound() {
        saveEntity(TENANT_A, "google", BASE_TIME,
                TenantOidcProviderStatusValue.ENABLED, "setup", "user");

        Optional<TenantOidcProviderStatus> result =
                sut.findLatestByTenantIdAndProviderId(TENANT_A, "missing");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByTenantIdAndProviderId: 異なるtenantの同名provider_idは別物として扱われる")
    void findLatestByTenantIdAndProviderId_isolatesPerTenant() {
        saveEntity(TENANT_A, "google", BASE_TIME,
                TenantOidcProviderStatusValue.ENABLED, "A enabled", "user-a");
        saveEntity(TENANT_B, "google", BASE_TIME,
                TenantOidcProviderStatusValue.DISABLED, "B disabled", "user-b");

        Optional<TenantOidcProviderStatus> a =
                sut.findLatestByTenantIdAndProviderId(TENANT_A, "google");
        Optional<TenantOidcProviderStatus> b =
                sut.findLatestByTenantIdAndProviderId(TENANT_B, "google");

        assertThat(a).isPresent();
        assertThat(a.get().getStatus()).isEqualTo(TenantOidcProviderStatusValue.ENABLED);
        assertThat(b).isPresent();
        assertThat(b.get().getStatus()).isEqualTo(TenantOidcProviderStatusValue.DISABLED);
    }

    @Test
    @DisplayName("findAllByTenantIdAndProviderIdOrderByVersionDesc: 履歴がversion降順で返る")
    void findAllByTenantIdAndProviderIdOrderByVersionDesc_returnsAllOrderedDesc() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity(TENANT_A, "google", v1,
                TenantOidcProviderStatusValue.ENABLED, "initial", "user:1");
        saveEntity(TENANT_A, "google", v2,
                TenantOidcProviderStatusValue.DISABLED, "maintenance", "user:2");
        saveEntity(TENANT_A, "google", v3,
                TenantOidcProviderStatusValue.ENABLED, "re-enabled", "user:3");

        List<TenantOidcProviderStatus> result =
                sut.findAllByTenantIdAndProviderIdOrderByVersionDesc(TENANT_A, "google");

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getVersion()).isEqualTo(v3);
        assertThat(result.get(1).getVersion()).isEqualTo(v2);
        assertThat(result.get(2).getVersion()).isEqualTo(v1);
    }

    @Test
    @DisplayName("findAllByTenantIdAndProviderIdOrderByVersionDesc: 履歴が無い場合、空リストを返す")
    void findAllByTenantIdAndProviderIdOrderByVersionDesc_returnsEmpty_whenNoData() {
        List<TenantOidcProviderStatus> result =
                sut.findAllByTenantIdAndProviderIdOrderByVersionDesc(TENANT_A, "google");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByTenantIdAndProviderIdOrderByVersionDesc: 他テナントの履歴は含まれない")
    void findAllByTenantIdAndProviderIdOrderByVersionDesc_excludesOtherTenants() {
        saveEntity(TENANT_A, "google", BASE_TIME,
                TenantOidcProviderStatusValue.ENABLED, "A initial", "user-a");
        saveEntity(TENANT_B, "google", BASE_TIME,
                TenantOidcProviderStatusValue.DISABLED, "B initial", "user-b");
        saveEntity(TENANT_B, "google", BASE_TIME.plusHours(1),
                TenantOidcProviderStatusValue.ENABLED, "B re-enabled", "user-b");

        List<TenantOidcProviderStatus> result =
                sut.findAllByTenantIdAndProviderIdOrderByVersionDesc(TENANT_A, "google");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTenantId()).isEqualTo(TENANT_A);
        assertThat(result.get(0).getReason()).isEqualTo("A initial");
    }

    @Test
    @DisplayName("findAllLatestEnabledByTenantId: テナント内のENABLEDプロバイダーのみ最新statusレコードが返る")
    void findAllLatestEnabledByTenantId_returnsOnlyEnabled() {
        OffsetDateTime v1 = BASE_TIME;
        // google: ENABLED
        saveEntity(TENANT_A, "google", v1,
                TenantOidcProviderStatusValue.ENABLED, "enabled", "user");
        // azure: DISABLED（含まれない）
        saveEntity(TENANT_A, "azure", v1,
                TenantOidcProviderStatusValue.DISABLED, "disabled", "user");
        // okta: ENABLED
        saveEntity(TENANT_A, "okta", v1,
                TenantOidcProviderStatusValue.ENABLED, "enabled", "user");

        List<TenantOidcProviderStatus> result =
                sut.findAllLatestEnabledByTenantId(TENANT_A);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TenantOidcProviderStatus::getProviderId)
                .containsExactlyInAnyOrder("google", "okta");
        assertThat(result).allMatch(s ->
                s.getStatus() == TenantOidcProviderStatusValue.ENABLED);
    }

    @Test
    @DisplayName("findAllLatestEnabledByTenantId: 最新versionでENABLEDになった場合、結果に含まれる")
    void findAllLatestEnabledByTenantId_returnsWhenLatestIsEnabled() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        // 過去: DISABLED → 最新: ENABLED → 含まれる
        saveEntity(TENANT_A, "google", v1,
                TenantOidcProviderStatusValue.DISABLED, "initial-disabled", "user");
        saveEntity(TENANT_A, "google", v2,
                TenantOidcProviderStatusValue.ENABLED, "re-enabled", "user");

        List<TenantOidcProviderStatus> result =
                sut.findAllLatestEnabledByTenantId(TENANT_A);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVersion()).isEqualTo(v2);
        assertThat(result.get(0).getReason()).isEqualTo("re-enabled");
    }

    @Test
    @DisplayName("findAllLatestEnabledByTenantId: 最新versionでDISABLEDになった場合、結果に含まれない")
    void findAllLatestEnabledByTenantId_excludesWhenLatestIsDisabled() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        // 過去: ENABLED → 最新: DISABLED → 含まれない
        saveEntity(TENANT_A, "google", v1,
                TenantOidcProviderStatusValue.ENABLED, "initial-enabled", "user");
        saveEntity(TENANT_A, "google", v2,
                TenantOidcProviderStatusValue.DISABLED, "disabled", "user");

        List<TenantOidcProviderStatus> result =
                sut.findAllLatestEnabledByTenantId(TENANT_A);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllLatestEnabledByTenantId: 他テナントのENABLEDは含まれない")
    void findAllLatestEnabledByTenantId_excludesOtherTenants() {
        // TENANT_A: google ENABLED
        saveEntity(TENANT_A, "google", BASE_TIME,
                TenantOidcProviderStatusValue.ENABLED, "A enabled", "user-a");
        // TENANT_B: azure ENABLED
        saveEntity(TENANT_B, "azure", BASE_TIME,
                TenantOidcProviderStatusValue.ENABLED, "B enabled", "user-b");
        // TENANT_B: okta ENABLED
        saveEntity(TENANT_B, "okta", BASE_TIME,
                TenantOidcProviderStatusValue.ENABLED, "B okta enabled", "user-b");

        List<TenantOidcProviderStatus> resultA =
                sut.findAllLatestEnabledByTenantId(TENANT_A);
        List<TenantOidcProviderStatus> resultB =
                sut.findAllLatestEnabledByTenantId(TENANT_B);

        assertThat(resultA).hasSize(1);
        assertThat(resultA.get(0).getProviderId()).isEqualTo("google");
        assertThat(resultA.get(0).getTenantId()).isEqualTo(TENANT_A);
        assertThat(resultB).hasSize(2);
        assertThat(resultB).extracting(TenantOidcProviderStatus::getProviderId)
                .containsExactlyInAnyOrder("azure", "okta");
    }

    @Test
    @DisplayName("save: 新規ステータスレコードが永続化される")
    void save_persistsNewStatus() {
        TenantOidcProviderStatus status = new TenantOidcProviderStatus(
                TENANT_A, "google", BASE_TIME,
                TenantOidcProviderStatusValue.ENABLED, "first enable",
                BASE_TIME, "creator");

        sut.save(status);

        Optional<TenantOidcProviderStatus> saved =
                sut.findLatestByTenantIdAndProviderId(TENANT_A, "google");
        assertThat(saved).isPresent();
        assertThat(saved.get().getStatus())
                .isEqualTo(TenantOidcProviderStatusValue.ENABLED);
        assertThat(saved.get().getReason()).isEqualTo("first enable");
        assertThat(saved.get().getCreatedBy()).isEqualTo("creator");
    }

    @Test
    @DisplayName("save: 同一(tenantId, providerId)で異なるversionを保存すると別行として共存し、最新版が取得される")
    void save_keepsBothVersions_whenSameTenantProviderDifferentVersion() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        sut.save(new TenantOidcProviderStatus(
                TENANT_A, "google", v1,
                TenantOidcProviderStatusValue.ENABLED, "initial", v1, "creator-1"));
        sut.save(new TenantOidcProviderStatus(
                TENANT_A, "google", v2,
                TenantOidcProviderStatusValue.DISABLED, "maintenance", v2, "creator-2"));

        // 両 version が存在
        assertThat(jpaRepository.findAll()).hasSize(2);
        // findLatest は v2 を返す
        Optional<TenantOidcProviderStatus> latest =
                sut.findLatestByTenantIdAndProviderId(TENANT_A, "google");
        assertThat(latest).isPresent();
        assertThat(latest.get().getVersion()).isEqualTo(v2);
        assertThat(latest.get().getStatus())
                .isEqualTo(TenantOidcProviderStatusValue.DISABLED);
        assertThat(latest.get().getReason()).isEqualTo("maintenance");
    }

}
