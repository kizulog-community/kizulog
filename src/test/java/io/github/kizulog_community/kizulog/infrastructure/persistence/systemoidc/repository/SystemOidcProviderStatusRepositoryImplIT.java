package io.github.kizulog_community.kizulog.infrastructure.persistence.systemoidc.repository;

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

import io.github.kizulog_community.kizulog.domain.systemoidc.model.OidcProviderStatusValue;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProviderStatus;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemoidc.entity.SystemOidcProviderStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemoidc.entity.SystemOidcProviderStatusId;

/**
 * SystemOidcProviderStatusRepositoryImpl の統合テスト
 *
 * <p>Output Port（SystemOidcProviderStatusRepository）のメソッドについて、
 * 正常系・境界値・異常系を検証する。</p>
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(SystemOidcProviderStatusRepositoryImpl.class)
class SystemOidcProviderStatusRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private SystemOidcProviderStatusRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private SystemOidcProviderStatusJpaRepository jpaRepository;

    /** テスト用基準時刻（UTC） */
    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    @Test
    @DisplayName("findLatestByProviderId: 同一provider_idで複数バージョンが存在する場合、最大versionのレコードを返す")
    void findLatestByProviderId_returnsLatestVersion_whenMultipleVersionsExist() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity("master", v1, OidcProviderStatusValue.ENABLED, "initial", "user:1");
        saveEntity("master", v3, OidcProviderStatusValue.ENABLED, "re-enabled", "user:3");
        saveEntity("master", v2, OidcProviderStatusValue.DISABLED, "maintenance", "user:2");

        Optional<SystemOidcProviderStatus> result = sut.findLatestByProviderId("master");

        assertThat(result).isPresent();
        assertThat(result.get().getProviderId()).isEqualTo("master");
        assertThat(result.get().getVersion()).isEqualTo(v3);
        assertThat(result.get().getStatus()).isEqualTo(OidcProviderStatusValue.ENABLED);
        assertThat(result.get().getReason()).isEqualTo("re-enabled");
        assertThat(result.get().getCreatedBy()).isEqualTo("user:3");
    }

    @Test
    @DisplayName("findLatestByProviderId: 単一バージョンしか存在しない場合、そのレコードを返す")
    void findLatestByProviderId_returnsSingleVersion_whenOnlyOneExists() {
        saveEntity("google", BASE_TIME, OidcProviderStatusValue.ENABLED,
                "setup", "user:setup");

        Optional<SystemOidcProviderStatus> result = sut.findLatestByProviderId("google");

        assertThat(result).isPresent();
        assertThat(result.get().getProviderId()).isEqualTo("google");
        assertThat(result.get().getStatus()).isEqualTo(OidcProviderStatusValue.ENABLED);
    }

    @Test
    @DisplayName("findLatestByProviderId: 該当provider_idが存在しない場合、空のOptionalを返す")
    void findLatestByProviderId_returnsEmpty_whenNotFound() {
        saveEntity("master", BASE_TIME, OidcProviderStatusValue.ENABLED,
                "setup", "user:1");

        Optional<SystemOidcProviderStatus> result = sut.findLatestByProviderId("not-exist");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByProviderId: テーブルが空の場合、空のOptionalを返す")
    void findLatestByProviderId_returnsEmpty_whenTableEmpty() {
        Optional<SystemOidcProviderStatus> result = sut.findLatestByProviderId("master");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findProviderIdsByLatestStatus: 最新ステータスがENABLEDのprovider_idを全て返す")
    void findProviderIdsByLatestStatus_returnsEnabledProviderIds() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        // master: v1=ENABLED, v2=ENABLED → 最新ENABLED
        saveEntity("master", v1, OidcProviderStatusValue.ENABLED, "initial", "u1");
        saveEntity("master", v2, OidcProviderStatusValue.ENABLED, "no change", "u2");
        // google: v1=ENABLED, v2=DISABLED → 最新DISABLED
        saveEntity("google", v1, OidcProviderStatusValue.ENABLED, "initial", "u1");
        saveEntity("google", v2, OidcProviderStatusValue.DISABLED, "deprecated", "u2");
        // azure: v1=ENABLED → 最新ENABLED
        saveEntity("azure", v1, OidcProviderStatusValue.ENABLED, "initial", "u1");

        List<String> result =
                sut.findProviderIdsByLatestStatus(OidcProviderStatusValue.ENABLED);

        assertThat(result).containsExactlyInAnyOrder("master", "azure");
    }

    @Test
    @DisplayName("findProviderIdsByLatestStatus: 最新ステータスがDISABLEDのprovider_idを全て返す")
    void findProviderIdsByLatestStatus_returnsDisabledProviderIds() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        saveEntity("master", v1, OidcProviderStatusValue.ENABLED, "initial", "u1");
        saveEntity("master", v2, OidcProviderStatusValue.DISABLED, "stopping", "u2");
        saveEntity("google", v1, OidcProviderStatusValue.ENABLED, "initial", "u1");

        List<String> result =
                sut.findProviderIdsByLatestStatus(OidcProviderStatusValue.DISABLED);

        assertThat(result).containsExactly("master");
    }

    @Test
    @DisplayName("findProviderIdsByLatestStatus: 該当ステータスが存在しない場合、空のリストを返す")
    void findProviderIdsByLatestStatus_returnsEmpty_whenNoMatch() {
        saveEntity("master", BASE_TIME, OidcProviderStatusValue.ENABLED, "init", "u1");

        List<String> result =
                sut.findProviderIdsByLatestStatus(OidcProviderStatusValue.DISABLED);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findProviderIdsByLatestStatus: テーブルが空の場合、空のリストを返す")
    void findProviderIdsByLatestStatus_returnsEmpty_whenTableEmpty() {
        List<String> result =
                sut.findProviderIdsByLatestStatus(OidcProviderStatusValue.ENABLED);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findProviderIdsByLatestStatus: 過去にENABLEDだが最新がDISABLEDのレコードはENABLED検索結果に含まれない")
    void findProviderIdsByLatestStatus_excludesPastEnabledIfLatestIsDisabled() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        // 過去にENABLED、無効化、再びENABLED → 最新ENABLED
        saveEntity("master", v1, OidcProviderStatusValue.ENABLED, "initial", "u1");
        saveEntity("master", v2, OidcProviderStatusValue.DISABLED, "deprecated", "u2");
        saveEntity("master", v3, OidcProviderStatusValue.ENABLED, "reactivated", "u3");

        List<String> enabledList =
                sut.findProviderIdsByLatestStatus(OidcProviderStatusValue.ENABLED);
        List<String> disabledList =
                sut.findProviderIdsByLatestStatus(OidcProviderStatusValue.DISABLED);

        assertThat(enabledList).containsExactly("master");
        assertThat(disabledList).isEmpty();
    }

    @Test
    @DisplayName("save: 新規ステータスが正しく永続化される")
    void save_persistsNewStatus() {
        SystemOidcProviderStatus status = new SystemOidcProviderStatus(
                "master", BASE_TIME, OidcProviderStatusValue.ENABLED,
                "initial setup", BASE_TIME, "user:setup");

        sut.save(status);

        Optional<SystemOidcProviderStatus> reloaded = sut.findLatestByProviderId("master");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getProviderId()).isEqualTo("master");
        assertThat(reloaded.get().getVersion()).isEqualTo(BASE_TIME);
        assertThat(reloaded.get().getStatus()).isEqualTo(OidcProviderStatusValue.ENABLED);
        assertThat(reloaded.get().getReason()).isEqualTo("initial setup");
        assertThat(reloaded.get().getCreatedAt()).isEqualTo(BASE_TIME);
        assertThat(reloaded.get().getCreatedBy()).isEqualTo("user:setup");
    }

    @Test
    @DisplayName("save: 同一provider_idで複数バージョンを保存できる（状態変更履歴）")
    void save_persistsMultipleVersions_forSameProviderId() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);

        SystemOidcProviderStatus enabled = new SystemOidcProviderStatus(
                "master", v1, OidcProviderStatusValue.ENABLED,
                "initial", v1, "user:1");
        SystemOidcProviderStatus disabled = new SystemOidcProviderStatus(
                "master", v2, OidcProviderStatusValue.DISABLED,
                "stopping", v2, "user:2");

        sut.save(enabled);
        sut.save(disabled);

        Optional<SystemOidcProviderStatus> latest = sut.findLatestByProviderId("master");
        assertThat(latest).isPresent();
        assertThat(latest.get().getVersion()).isEqualTo(v2);
        assertThat(latest.get().getStatus()).isEqualTo(OidcProviderStatusValue.DISABLED);
        assertThat(latest.get().getReason()).isEqualTo("stopping");
    }

    @Test
    @DisplayName("save: reasonがnullでも保存できる（DBスキーマ的にnullable）")
    void save_persistsNullReason() {
        SystemOidcProviderStatus status = new SystemOidcProviderStatus(
                "master", BASE_TIME, OidcProviderStatusValue.ENABLED,
                null, BASE_TIME, "user:setup");

        sut.save(status);

        Optional<SystemOidcProviderStatus> reloaded = sut.findLatestByProviderId("master");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getReason()).isNull();
    }

    @Test
    @DisplayName("save: 全フィールドが正しく永続化される")
    void save_allFieldsPersistedCorrectly() {
        OffsetDateTime createdAt = BASE_TIME.plusMinutes(10);
        SystemOidcProviderStatus status = new SystemOidcProviderStatus(
                "google", BASE_TIME, OidcProviderStatusValue.DISABLED,
                "Replaced by Azure AD", createdAt, "admin:operator-1");

        sut.save(status);

        Optional<SystemOidcProviderStatus> reloaded = sut.findLatestByProviderId("google");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getProviderId()).isEqualTo("google");
        assertThat(reloaded.get().getVersion()).isEqualTo(BASE_TIME);
        assertThat(reloaded.get().getStatus()).isEqualTo(OidcProviderStatusValue.DISABLED);
        assertThat(reloaded.get().getReason()).isEqualTo("Replaced by Azure AD");
        assertThat(reloaded.get().getCreatedAt()).isEqualTo(createdAt);
        assertThat(reloaded.get().getCreatedBy()).isEqualTo("admin:operator-1");
    }

    /**
     * テストデータを直接JPAリポジトリ経由で投入する。
     */
    private void saveEntity(
            String providerId, OffsetDateTime version,
            OidcProviderStatusValue status, String reason, String createdBy) {
        SystemOidcProviderStatusEntity entity = new SystemOidcProviderStatusEntity(
                new SystemOidcProviderStatusId(providerId, version),
                status, reason,
                OffsetDateTime.now(ZoneOffset.UTC), createdBy);
        jpaRepository.save(entity);
    }

}
