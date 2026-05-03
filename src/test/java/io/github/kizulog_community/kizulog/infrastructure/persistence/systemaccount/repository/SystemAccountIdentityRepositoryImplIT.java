package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

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

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountIdentityEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountIdentityId;

/**
 * SystemAccountIdentityRepositoryImpl の統合テスト
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(SystemAccountIdentityRepositoryImpl.class)
class SystemAccountIdentityRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private SystemAccountIdentityRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private SystemAccountIdentityJpaRepository jpaRepository;

    /** テスト用基準時刻（UTC） */
    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    /**
     * テストデータ投入ヘルパー（JPA直接保存）
     */
    private void saveEntity(
            String identityId, OffsetDateTime version,
            String accountId, String iss, String aud, String sub, String createdBy) {
        SystemAccountIdentityEntity entity = new SystemAccountIdentityEntity(
                new SystemAccountIdentityId(identityId, version),
                accountId, iss, aud, sub, version, createdBy);
        jpaRepository.save(entity);
    }

    // ========================================================================
    // findLatestByIssAndAudAndSub
    // ========================================================================

    @Test
    @DisplayName("findLatestByIssAndAudAndSub: 同一identityで複数versionが存在する場合、最大versionを返す")
    void findLatestByIssAndAudAndSub_returnsLatestVersion() {
        // given - 同じidentity_idでversion違い
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        saveEntity("id-1", v1, "acc-1", "iss-A", "aud-A", "sub-A", "user:1");
        saveEntity("id-1", v2, "acc-1", "iss-A", "aud-A", "sub-A", "user:2");

        // when
        Optional<SystemAccountIdentity> result =
                sut.findLatestByIssAndAudAndSub("iss-A", "aud-A", "sub-A");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getIdentityId()).isEqualTo("id-1");
        assertThat(result.get().getVersion()).isEqualTo(v2);
        assertThat(result.get().getCreatedBy()).isEqualTo("user:2");
    }

    @Test
    @DisplayName("findLatestByIssAndAudAndSub: 単一versionしか存在しない場合、そのレコードを返す")
    void findLatestByIssAndAudAndSub_returnsSingleVersion() {
        // given
        saveEntity("id-1", BASE_TIME, "acc-1", "iss-A", "aud-A", "sub-A", "system:setup");

        // when
        Optional<SystemAccountIdentity> result =
                sut.findLatestByIssAndAudAndSub("iss-A", "aud-A", "sub-A");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getIdentityId()).isEqualTo("id-1");
        assertThat(result.get().getAccountId()).isEqualTo("acc-1");
        assertThat(result.get().getIss()).isEqualTo("iss-A");
        assertThat(result.get().getAud()).isEqualTo("aud-A");
        assertThat(result.get().getSub()).isEqualTo("sub-A");
    }

    @Test
    @DisplayName("findLatestByIssAndAudAndSub: 該当(iss,aud,sub)が存在しない場合、空Optional")
    void findLatestByIssAndAudAndSub_returnsEmpty_whenNotFound() {
        // given
        saveEntity("id-1", BASE_TIME, "acc-1", "iss-A", "aud-A", "sub-A", "user:1");

        // when
        Optional<SystemAccountIdentity> result =
                sut.findLatestByIssAndAudAndSub("iss-X", "aud-X", "sub-X");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByIssAndAudAndSub: テーブルが空の場合、空Optional")
    void findLatestByIssAndAudAndSub_returnsEmpty_whenTableIsEmpty() {
        // when
        Optional<SystemAccountIdentity> result =
                sut.findLatestByIssAndAudAndSub("iss-A", "aud-A", "sub-A");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByIssAndAudAndSub: 複数(iss,aud,sub)が混在する場合、指定組合せのみ返す")
    void findLatestByIssAndAudAndSub_returnsOnlyMatching() {
        // given
        saveEntity("id-1", BASE_TIME, "acc-1", "iss-A", "aud-A", "sub-A", "user:1");
        saveEntity("id-2", BASE_TIME.plusHours(1), "acc-2", "iss-B", "aud-B", "sub-B", "user:2");

        // when
        Optional<SystemAccountIdentity> result =
                sut.findLatestByIssAndAudAndSub("iss-A", "aud-A", "sub-A");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getIdentityId()).isEqualTo("id-1");
    }

    // ========================================================================
    // findLatestByIdentityId
    // ========================================================================

    @Test
    @DisplayName("findLatestByIdentityId: 同一identity_idで複数versionが存在する場合、最大versionを返す")
    void findLatestByIdentityId_returnsLatestVersion() {
        // given
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        saveEntity("id-1", v1, "acc-1", "iss-A", "aud-A", "sub-A", "user:1");
        saveEntity("id-1", v2, "acc-1", "iss-A", "aud-A", "sub-A", "user:2");

        // when
        Optional<SystemAccountIdentity> result = sut.findLatestByIdentityId("id-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo(v2);
        assertThat(result.get().getCreatedBy()).isEqualTo("user:2");
    }

    @Test
    @DisplayName("findLatestByIdentityId: 該当identity_idが存在しない場合、空Optional")
    void findLatestByIdentityId_returnsEmpty_whenNotFound() {
        // given
        saveEntity("id-1", BASE_TIME, "acc-1", "iss-A", "aud-A", "sub-A", "user:1");

        // when
        Optional<SystemAccountIdentity> result = sut.findLatestByIdentityId("id-X");

        // then
        assertThat(result).isEmpty();
    }

    // ========================================================================
    // findLatestByAccountId
    // ========================================================================

    @Test
    @DisplayName("findLatestByAccountId: 1アカウントに複数identityがある場合、各identityの最新版を全て返す")
    void findLatestByAccountId_returnsAllIdentitiesForAccount() {
        // given - acc-1に2つのidentity（複数OIDC対応）
        saveEntity("id-1", BASE_TIME, "acc-1", "iss-A", "aud-A", "sub-A", "user:1");
        saveEntity("id-2", BASE_TIME.plusHours(1), "acc-1", "iss-B", "aud-B", "sub-B", "user:2");

        // when
        List<SystemAccountIdentity> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(SystemAccountIdentity::getIdentityId)
                .containsExactlyInAnyOrder("id-1", "id-2");
    }

    @Test
    @DisplayName("findLatestByAccountId: 同一identityで複数versionがある場合、各identityの最新版のみ返す")
    void findLatestByAccountId_returnsLatestPerIdentity() {
        // given
        saveEntity("id-1", BASE_TIME, "acc-1", "iss-A", "aud-A", "sub-A", "user:1");
        saveEntity("id-1", BASE_TIME.plusHours(1), "acc-1", "iss-A", "aud-A", "sub-A", "user:2");

        // when
        List<SystemAccountIdentity> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVersion()).isEqualTo(BASE_TIME.plusHours(1));
    }

    @Test
    @DisplayName("findLatestByAccountId: 該当accountIdが存在しない場合、空リスト")
    void findLatestByAccountId_returnsEmpty_whenNotFound() {
        // given
        saveEntity("id-1", BASE_TIME, "acc-1", "iss-A", "aud-A", "sub-A", "user:1");

        // when
        List<SystemAccountIdentity> result = sut.findLatestByAccountId("acc-X");

        // then
        assertThat(result).isEmpty();
    }

    // ========================================================================
    // save
    // ========================================================================

    @Test
    @DisplayName("save: 新規レコードを保存できる")
    void save_persistsNewRecord() {
        // given
        SystemAccountIdentity identity = new SystemAccountIdentity(
                "id-1", BASE_TIME, "acc-1", "iss-A", "aud-A", "sub-A",
                BASE_TIME, "system:setup");

        // when
        sut.save(identity);

        // then
        assertThat(jpaRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("save: 同一identity_idで複数バージョンを保存すると全バージョンが残る")
    void save_persistsMultipleVersions() {
        // given
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        SystemAccountIdentity i1 = new SystemAccountIdentity(
                "id-1", v1, "acc-1", "iss-A", "aud-A", "sub-A", v1, "user:1");
        SystemAccountIdentity i2 = new SystemAccountIdentity(
                "id-1", v2, "acc-1", "iss-A", "aud-A", "sub-A", v2, "user:2");

        // when
        sut.save(i1);
        sut.save(i2);

        // then
        assertThat(jpaRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("save: 全フィールドが正しく永続化される")
    void save_persistsAllFields() {
        // given
        SystemAccountIdentity identity = new SystemAccountIdentity(
                "id-1", BASE_TIME, "acc-1", "iss-value", "aud-value", "sub-value",
                BASE_TIME, "system:test");

        // when
        sut.save(identity);

        // then
        Optional<SystemAccountIdentity> loaded = sut.findLatestByIdentityId("id-1");
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getIdentityId()).isEqualTo("id-1");
        assertThat(loaded.get().getAccountId()).isEqualTo("acc-1");
        assertThat(loaded.get().getIss()).isEqualTo("iss-value");
        assertThat(loaded.get().getAud()).isEqualTo("aud-value");
        assertThat(loaded.get().getSub()).isEqualTo("sub-value");
        assertThat(loaded.get().getCreatedAt()).isEqualTo(BASE_TIME);
        assertThat(loaded.get().getCreatedBy()).isEqualTo("system:test");
    }

}
