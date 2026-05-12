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

import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProvider;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemoidc.entity.SystemOidcProviderEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemoidc.entity.SystemOidcProviderId;

/**
 * SystemOidcProviderRepositoryImpl の統合テスト
 *
 * <p>Output Port（SystemOidcProviderRepository）のメソッドについて、
 * 正常系・境界値・異常系を検証する。</p>
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(SystemOidcProviderRepositoryImpl.class)
class SystemOidcProviderRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private SystemOidcProviderRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private SystemOidcProviderJpaRepository jpaRepository;

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
        saveEntity("master", v1, "Master v1", "https://auth.example/realms/master",
                "client-1", "secret-1", "user:1");
        saveEntity("master", v3, "Master v3", "https://auth.example/realms/master",
                "client-3", "secret-3", "user:3");
        saveEntity("master", v2, "Master v2", "https://auth.example/realms/master",
                "client-2", "secret-2", "user:2");

        Optional<SystemOidcProvider> result = sut.findLatestByProviderId("master");

        assertThat(result).isPresent();
        assertThat(result.get().getProviderId()).isEqualTo("master");
        assertThat(result.get().getVersion()).isEqualTo(v3);
        assertThat(result.get().getDisplayName()).isEqualTo("Master v3");
        assertThat(result.get().getClientId()).isEqualTo("client-3");
        assertThat(result.get().getClientSecret()).isEqualTo("secret-3");
        assertThat(result.get().getCreatedBy()).isEqualTo("user:3");
    }

    @Test
    @DisplayName("findLatestByProviderId: 単一バージョンしか存在しない場合、そのレコードを返す")
    void findLatestByProviderId_returnsSingleVersion_whenOnlyOneExists() {
        saveEntity("google", BASE_TIME, "Google", "https://accounts.google.com",
                "client-g", "secret-g", "user:setup");

        Optional<SystemOidcProvider> result = sut.findLatestByProviderId("google");

        assertThat(result).isPresent();
        assertThat(result.get().getProviderId()).isEqualTo("google");
        assertThat(result.get().getDisplayName()).isEqualTo("Google");
    }

    @Test
    @DisplayName("findLatestByProviderId: 該当provider_idが存在しない場合、空のOptionalを返す")
    void findLatestByProviderId_returnsEmpty_whenNotFound() {
        saveEntity("master", BASE_TIME, "Master", "https://auth.example/realms/master",
                "client-1", "secret-1", "user:1");

        Optional<SystemOidcProvider> result = sut.findLatestByProviderId("not-exist");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByProviderId: テーブルが空の場合、空のOptionalを返す")
    void findLatestByProviderId_returnsEmpty_whenTableEmpty() {
        Optional<SystemOidcProvider> result = sut.findLatestByProviderId("master");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByProviderId: 複数provider_idが混在する場合、指定idの最新を返す")
    void findLatestByProviderId_returnsCorrectProvider_whenMultipleIdsExist() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        saveEntity("master", v1, "Master", "https://auth.example/realms/master",
                "client-m", "secret-m", "user:m");
        saveEntity("google", v2, "Google", "https://accounts.google.com",
                "client-g", "secret-g", "user:g");

        Optional<SystemOidcProvider> result = sut.findLatestByProviderId("google");

        assertThat(result).isPresent();
        assertThat(result.get().getProviderId()).isEqualTo("google");
        assertThat(result.get().getVersion()).isEqualTo(v2);
    }

    @Test
    @DisplayName("findAllLatest: 各provider_id毎に最新versionのみを返す")
    void findAllLatest_returnsLatestPerProviderId() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        // master は3バージョン
        saveEntity("master", v1, "Master v1", "https://auth.example/realms/master",
                "c-1", "s-1", "u-1");
        saveEntity("master", v2, "Master v2", "https://auth.example/realms/master",
                "c-2", "s-2", "u-2");
        saveEntity("master", v3, "Master v3", "https://auth.example/realms/master",
                "c-3", "s-3", "u-3");
        // google は1バージョン
        saveEntity("google", v1, "Google", "https://accounts.google.com",
                "c-g", "s-g", "u-g");

        List<SystemOidcProvider> result = sut.findAllLatest();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SystemOidcProvider::getProviderId)
                .containsExactlyInAnyOrder("master", "google");

        SystemOidcProvider master = result.stream()
                .filter(p -> "master".equals(p.getProviderId())).findFirst().orElseThrow();
        assertThat(master.getVersion()).isEqualTo(v3);
        assertThat(master.getDisplayName()).isEqualTo("Master v3");
    }

    @Test
    @DisplayName("findAllLatest: テーブルが空の場合、空のリストを返す")
    void findAllLatest_returnsEmptyList_whenTableEmpty() {
        List<SystemOidcProvider> result = sut.findAllLatest();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllLatest: 単一プロバイダー単一バージョンの場合、1件返す")
    void findAllLatest_returnsSingleProvider_whenOnlyOneExists() {
        saveEntity("master", BASE_TIME, "Master", "https://auth.example/realms/master",
                "client-1", "secret-1", "user:1");

        List<SystemOidcProvider> result = sut.findAllLatest();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProviderId()).isEqualTo("master");
    }

    @Test
    @DisplayName("existsByProviderId: provider_idが存在する場合、trueを返す")
    void existsByProviderId_returnsTrue_whenExists() {
        saveEntity("master", BASE_TIME, "Master", "https://auth.example/realms/master",
                "client-1", "secret-1", "user:1");

        boolean result = sut.existsByProviderId("master");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByProviderId: provider_idが存在しない場合、falseを返す")
    void existsByProviderId_returnsFalse_whenNotExists() {
        saveEntity("master", BASE_TIME, "Master", "https://auth.example/realms/master",
                "client-1", "secret-1", "user:1");

        boolean result = sut.existsByProviderId("not-exist");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("existsByProviderId: テーブルが空の場合、falseを返す")
    void existsByProviderId_returnsFalse_whenTableEmpty() {
        boolean result = sut.existsByProviderId("master");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("existsByProviderId: 過去バージョンしかなくてもtrueを返す（過去含む重複禁止のため）")
    void existsByProviderId_returnsTrue_whenOnlyPastVersionExists() {
        // version-managed なので、過去バージョンが存在するだけでもidとしては既存扱い
        OffsetDateTime past = BASE_TIME;
        saveEntity("master", past, "Master", "https://auth.example/realms/master",
                "client-1", "secret-1", "user:1");

        boolean result = sut.existsByProviderId("master");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("save: 新規プロバイダーが正しく永続化される")
    void save_persistsNewProvider() {
        SystemOidcProvider provider = new SystemOidcProvider(
                "master", BASE_TIME, "Master Display",
                "https://auth.example/realms/master",
                "client-1", "secret-1", BASE_TIME, "user:setup");

        sut.save(provider);

        Optional<SystemOidcProvider> reloaded = sut.findLatestByProviderId("master");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getProviderId()).isEqualTo("master");
        assertThat(reloaded.get().getVersion()).isEqualTo(BASE_TIME);
        assertThat(reloaded.get().getDisplayName()).isEqualTo("Master Display");
        assertThat(reloaded.get().getUri()).isEqualTo("https://auth.example/realms/master");
        assertThat(reloaded.get().getClientId()).isEqualTo("client-1");
        assertThat(reloaded.get().getClientSecret()).isEqualTo("secret-1");
        assertThat(reloaded.get().getCreatedAt()).isEqualTo(BASE_TIME);
        assertThat(reloaded.get().getCreatedBy()).isEqualTo("user:setup");
    }

    @Test
    @DisplayName("save: 同一provider_idで複数バージョンを保存できる（履歴が積み上がる）")
    void save_persistsMultipleVersions_forSameProviderId() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);

        SystemOidcProvider providerV1 = new SystemOidcProvider(
                "master", v1, "Master V1",
                "https://auth.example/realms/master",
                "client-1", "secret-1", v1, "user:1");
        SystemOidcProvider providerV2 = new SystemOidcProvider(
                "master", v2, "Master V2",
                "https://auth.example/realms/master",
                "client-2", "secret-2", v2, "user:2");

        sut.save(providerV1);
        sut.save(providerV2);

        Optional<SystemOidcProvider> latest = sut.findLatestByProviderId("master");
        assertThat(latest).isPresent();
        assertThat(latest.get().getVersion()).isEqualTo(v2);
        assertThat(latest.get().getDisplayName()).isEqualTo("Master V2");
    }

    @Test
    @DisplayName("save: 全フィールドが正しく永続化される")
    void save_allFieldsPersistedCorrectly() {
        OffsetDateTime createdAt = BASE_TIME.plusMinutes(5);
        SystemOidcProvider provider = new SystemOidcProvider(
                "google", BASE_TIME, "Google Workspace",
                "https://accounts.google.com",
                "kizulog-google-client", "encrypted-secret-value",
                createdAt, "system:setup-wizard");

        sut.save(provider);

        Optional<SystemOidcProvider> reloaded = sut.findLatestByProviderId("google");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getProviderId()).isEqualTo("google");
        assertThat(reloaded.get().getVersion()).isEqualTo(BASE_TIME);
        assertThat(reloaded.get().getDisplayName()).isEqualTo("Google Workspace");
        assertThat(reloaded.get().getUri()).isEqualTo("https://accounts.google.com");
        assertThat(reloaded.get().getClientId()).isEqualTo("kizulog-google-client");
        assertThat(reloaded.get().getClientSecret()).isEqualTo("encrypted-secret-value");
        assertThat(reloaded.get().getCreatedAt()).isEqualTo(createdAt);
        assertThat(reloaded.get().getCreatedBy()).isEqualTo("system:setup-wizard");
    }

    private void saveEntity(
            String providerId, OffsetDateTime version, String displayName,
            String uri, String clientId, String clientSecret, String createdBy) {
        SystemOidcProviderEntity entity = new SystemOidcProviderEntity(
                new SystemOidcProviderId(providerId, version),
                displayName, uri, clientId, clientSecret,
                OffsetDateTime.now(ZoneOffset.UTC), createdBy);
        jpaRepository.save(entity);
    }

}
