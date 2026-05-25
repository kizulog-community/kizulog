package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.repository;

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

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountIdentity;
import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountStatusValue;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountIdentityEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountIdentityId;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountIdentityStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountIdentityStatusId;

/**
 * TenantAccountIdentityRepositoryImpl の統合テスト
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TenantAccountIdentityRepositoryImpl.class)
class TenantAccountIdentityRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private TenantAccountIdentityRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private TenantAccountIdentityJpaRepository jpaRepository;

    /** identity_status 投入用 JPAリポジトリ */
    @Autowired
    private TenantAccountIdentityStatusJpaRepository statusJpaRepository;

    /** テスト用基準時刻（UTC） */
    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        statusJpaRepository.deleteAll();
        jpaRepository.deleteAll();
    }

    private void saveEntity(
            String identityId, OffsetDateTime version,
            String accountId, String tenantId,
            String iss, String aud, String sub, String createdBy) {
        TenantAccountIdentityEntity entity = new TenantAccountIdentityEntity(
                new TenantAccountIdentityId(identityId, version),
                accountId, tenantId, iss, aud, sub, version, createdBy);
        jpaRepository.save(entity);
    }

    private void saveStatus(
            String identityId, OffsetDateTime version,
            TenantAccountStatusValue status, String createdBy) {
        TenantAccountIdentityStatusEntity entity = new TenantAccountIdentityStatusEntity(
                new TenantAccountIdentityStatusId(identityId, version),
                status, null, version, createdBy);
        statusJpaRepository.save(entity);
    }

    @Test
    @DisplayName("findLatestByTenantIdAndIssAndAudAndSub: 同一identityで複数versionが存在する場合、最大versionを返す")
    void findLatestByTenantIdAndIssAndAudAndSub_returnsLatestVersion() {
        // given - 同じidentity_idでversion違い
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        saveEntity("id-1", v1, "acc-1", "tenant-A", "iss-A", "aud-A", "sub-A", "user:1");
        saveEntity("id-1", v2, "acc-1", "tenant-A", "iss-A", "aud-A", "sub-A", "user:2");

        // when
        Optional<TenantAccountIdentity> result =
                sut.findLatestByTenantIdAndIssAndAudAndSub(
                        "tenant-A", "iss-A", "aud-A", "sub-A");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getIdentityId()).isEqualTo("id-1");
        assertThat(result.get().getVersion()).isEqualTo(v2);
        assertThat(result.get().getCreatedBy()).isEqualTo("user:2");
    }

    @Test
    @DisplayName("findLatestByTenantIdAndIssAndAudAndSub: 単一versionしか存在しない場合、そのレコードを返す")
    void findLatestByTenantIdAndIssAndAudAndSub_returnsSingleVersion() {
        // given
        saveEntity("id-1", BASE_TIME, "acc-1", "tenant-A",
                "iss-A", "aud-A", "sub-A", "system:setup");

        // when
        Optional<TenantAccountIdentity> result =
                sut.findLatestByTenantIdAndIssAndAudAndSub(
                        "tenant-A", "iss-A", "aud-A", "sub-A");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getIdentityId()).isEqualTo("id-1");
        assertThat(result.get().getTenantId()).isEqualTo("tenant-A");
        assertThat(result.get().getAccountId()).isEqualTo("acc-1");
        assertThat(result.get().getIss()).isEqualTo("iss-A");
        assertThat(result.get().getAud()).isEqualTo("aud-A");
        assertThat(result.get().getSub()).isEqualTo("sub-A");
    }

    @Test
    @DisplayName("findLatestByTenantIdAndIssAndAudAndSub: 該当(tenant_id,iss,aud,sub)が存在しない場合、空Optional")
    void findLatestByTenantIdAndIssAndAudAndSub_returnsEmpty_whenNotFound() {
        // given
        saveEntity("id-1", BASE_TIME, "acc-1", "tenant-A",
                "iss-A", "aud-A", "sub-A", "user:1");

        // when
        Optional<TenantAccountIdentity> result =
                sut.findLatestByTenantIdAndIssAndAudAndSub(
                        "tenant-A", "iss-X", "aud-X", "sub-X");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByTenantIdAndIssAndAudAndSub: テーブルが空の場合、空Optional")
    void findLatestByTenantIdAndIssAndAudAndSub_returnsEmpty_whenTableIsEmpty() {
        // when
        Optional<TenantAccountIdentity> result =
                sut.findLatestByTenantIdAndIssAndAudAndSub(
                        "tenant-A", "iss-A", "aud-A", "sub-A");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByTenantIdAndIssAndAudAndSub: テナント境界が効く（同じiss/aud/subでも別テナントは取得されない）")
    void findLatestByTenantIdAndIssAndAudAndSub_excludesOtherTenants() {
        // given - 同じ Google アカウント (iss/aud/sub 同じ) で tenant-A と tenant-B に別々の identity
        saveEntity("id-A", BASE_TIME, "acc-A", "tenant-A",
                "https://accounts.google.com", "aud-shared", "sub-shared", "user:A");
        saveEntity("id-B", BASE_TIME, "acc-B", "tenant-B",
                "https://accounts.google.com", "aud-shared", "sub-shared", "user:B");

        // when - tenant-A で検索
        Optional<TenantAccountIdentity> result =
                sut.findLatestByTenantIdAndIssAndAudAndSub(
                        "tenant-A",
                        "https://accounts.google.com",
                        "aud-shared",
                        "sub-shared");

        // then - tenant-A の identity のみが返る
        assertThat(result).isPresent();
        assertThat(result.get().getIdentityId()).isEqualTo("id-A");
        assertThat(result.get().getTenantId()).isEqualTo("tenant-A");
        assertThat(result.get().getAccountId()).isEqualTo("acc-A");
    }

    @Test
    @DisplayName("findLatestByTenantIdAndIssAndAudAndSub: 複数identityが混在する場合、指定組合せのみ返す")
    void findLatestByTenantIdAndIssAndAudAndSub_returnsOnlyMatching() {
        // given
        saveEntity("id-1", BASE_TIME, "acc-1", "tenant-A",
                "iss-A", "aud-A", "sub-A", "user:1");
        saveEntity("id-2", BASE_TIME.plusHours(1), "acc-2", "tenant-A",
                "iss-B", "aud-B", "sub-B", "user:2");

        // when
        Optional<TenantAccountIdentity> result =
                sut.findLatestByTenantIdAndIssAndAudAndSub(
                        "tenant-A", "iss-A", "aud-A", "sub-A");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getIdentityId()).isEqualTo("id-1");
    }

    @Test
    @DisplayName("findLatestByIdentityId: 同一identity_idで複数versionが存在する場合、最大versionを返す")
    void findLatestByIdentityId_returnsLatestVersion() {
        // given
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        saveEntity("id-1", v1, "acc-1", "tenant-A", "iss-A", "aud-A", "sub-A", "user:1");
        saveEntity("id-1", v2, "acc-1", "tenant-A", "iss-A", "aud-A", "sub-A", "user:2");

        // when
        Optional<TenantAccountIdentity> result = sut.findLatestByIdentityId("id-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo(v2);
        assertThat(result.get().getCreatedBy()).isEqualTo("user:2");
    }

    @Test
    @DisplayName("findLatestByIdentityId: 該当identity_idが存在しない場合、空Optional")
    void findLatestByIdentityId_returnsEmpty_whenNotFound() {
        // given
        saveEntity("id-1", BASE_TIME, "acc-1", "tenant-A",
                "iss-A", "aud-A", "sub-A", "user:1");

        // when
        Optional<TenantAccountIdentity> result = sut.findLatestByIdentityId("id-X");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByAccountId: 1アカウントに複数identityがある場合、各identityの最新版を全て返す")
    void findLatestByAccountId_returnsAllIdentitiesForAccount() {
        // given - acc-1に2つのidentity（複数OIDC対応）
        saveEntity("id-1", BASE_TIME, "acc-1", "tenant-A",
                "iss-A", "aud-A", "sub-A", "user:1");
        saveEntity("id-2", BASE_TIME.plusHours(1), "acc-1", "tenant-A",
                "iss-B", "aud-B", "sub-B", "user:2");

        // when
        List<TenantAccountIdentity> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(TenantAccountIdentity::getIdentityId)
                .containsExactlyInAnyOrder("id-1", "id-2");
    }

    @Test
    @DisplayName("findLatestByAccountId: 同一identityで複数versionがある場合、各identityの最新版のみ返す")
    void findLatestByAccountId_returnsLatestPerIdentity() {
        // given
        saveEntity("id-1", BASE_TIME, "acc-1", "tenant-A",
                "iss-A", "aud-A", "sub-A", "user:1");
        saveEntity("id-1", BASE_TIME.plusHours(1), "acc-1", "tenant-A",
                "iss-A", "aud-A", "sub-A", "user:2");

        // when
        List<TenantAccountIdentity> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVersion()).isEqualTo(BASE_TIME.plusHours(1));
    }

    @Test
    @DisplayName("findLatestByAccountId: 該当accountIdが存在しない場合、空リスト")
    void findLatestByAccountId_returnsEmpty_whenNotFound() {
        // given
        saveEntity("id-1", BASE_TIME, "acc-1", "tenant-A",
                "iss-A", "aud-A", "sub-A", "user:1");

        // when
        List<TenantAccountIdentity> result = sut.findLatestByAccountId("acc-X");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save: 新規レコードを保存できる")
    void save_persistsNewRecord() {
        // given
        TenantAccountIdentity identity = new TenantAccountIdentity(
                "id-1", BASE_TIME, "acc-1", "tenant-A",
                "iss-A", "aud-A", "sub-A",
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
        TenantAccountIdentity i1 = new TenantAccountIdentity(
                "id-1", v1, "acc-1", "tenant-A",
                "iss-A", "aud-A", "sub-A", v1, "user:1");
        TenantAccountIdentity i2 = new TenantAccountIdentity(
                "id-1", v2, "acc-1", "tenant-A",
                "iss-A", "aud-A", "sub-A", v2, "user:2");

        // when
        sut.save(i1);
        sut.save(i2);

        // then
        assertThat(jpaRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("save: 全フィールドが正しく永続化される（tenantId含む）")
    void save_persistsAllFields() {
        // given
        TenantAccountIdentity identity = new TenantAccountIdentity(
                "id-1", BASE_TIME, "acc-1", "tenant-A",
                "iss-value", "aud-value", "sub-value",
                BASE_TIME, "system:test");

        // when
        sut.save(identity);

        // then
        Optional<TenantAccountIdentity> loaded = sut.findLatestByIdentityId("id-1");
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getIdentityId()).isEqualTo("id-1");
        assertThat(loaded.get().getAccountId()).isEqualTo("acc-1");
        assertThat(loaded.get().getTenantId()).isEqualTo("tenant-A");
        assertThat(loaded.get().getIss()).isEqualTo("iss-value");
        assertThat(loaded.get().getAud()).isEqualTo("aud-value");
        assertThat(loaded.get().getSub()).isEqualTo("sub-value");
        assertThat(loaded.get().getCreatedAt()).isEqualTo(BASE_TIME);
        assertThat(loaded.get().getCreatedBy()).isEqualTo("system:test");
    }

    @Test
    @DisplayName("countActiveByTenantIdAndIss: 該当(tenant_id,iss)を持つACTIVEなidentity数を返す")
    void countActiveByTenantIdAndIss_returnsCountOfActiveIdentities() {
        String iss = "https://auth.example/realms/master";
        String tenantId = "tenant-A";
        saveEntity("id-A", BASE_TIME, "acc-1", tenantId, iss, "aud-A", "sub-A", "u1");
        saveStatus("id-A", BASE_TIME, TenantAccountStatusValue.ACTIVE, "u1");
        saveEntity("id-B", BASE_TIME, "acc-2", tenantId, iss, "aud-B", "sub-B", "u2");
        saveStatus("id-B", BASE_TIME, TenantAccountStatusValue.ACTIVE, "u2");
        saveEntity("id-C", BASE_TIME, "acc-3", tenantId, iss, "aud-C", "sub-C", "u3");
        saveStatus("id-C", BASE_TIME, TenantAccountStatusValue.INACTIVE, "u3");
        saveEntity("id-D", BASE_TIME, "acc-4", tenantId,
                "https://other.example", "aud-D", "sub-D", "u4");
        saveStatus("id-D", BASE_TIME, TenantAccountStatusValue.ACTIVE, "u4");

        int count = sut.countActiveByTenantIdAndIss(tenantId, iss);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countActiveByTenantIdAndIss: 該当(tenant_id,iss)を持つidentityが存在しない場合、0を返す")
    void countActiveByTenantIdAndIss_returnsZero_whenNoMatch() {
        saveEntity("id-1", BASE_TIME, "acc-1", "tenant-A",
                "https://other.example", "aud", "sub", "u");
        saveStatus("id-1", BASE_TIME, TenantAccountStatusValue.ACTIVE, "u");

        int count = sut.countActiveByTenantIdAndIss(
                "tenant-A", "https://not-exist.example");

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("countActiveByTenantIdAndIss: テーブルが空の場合、0を返す")
    void countActiveByTenantIdAndIss_returnsZero_whenTableEmpty() {
        int count = sut.countActiveByTenantIdAndIss(
                "tenant-A", "https://auth.example/realms/master");

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("countActiveByTenantIdAndIss: 過去ACTIVEだが最新がINACTIVEのidentityはカウントされない")
    void countActiveByTenantIdAndIss_excludesPastActiveIfLatestIsInactive() {
        String iss = "https://auth.example/realms/master";
        String tenantId = "tenant-A";
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);

        saveEntity("id-A", v1, "acc-1", tenantId, iss, "aud", "sub", "u1");
        saveStatus("id-A", v1, TenantAccountStatusValue.ACTIVE, "u1");
        saveStatus("id-A", v2, TenantAccountStatusValue.INACTIVE, "u2");

        int count = sut.countActiveByTenantIdAndIss(tenantId, iss);

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("countActiveByTenantIdAndIss: 過去INACTIVEだが最新がACTIVEのidentityはカウントされる")
    void countActiveByTenantIdAndIss_includesIfLatestIsActive() {
        String iss = "https://auth.example/realms/master";
        String tenantId = "tenant-A";
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);

        saveEntity("id-A", v1, "acc-1", tenantId, iss, "aud", "sub", "u1");
        saveStatus("id-A", v1, TenantAccountStatusValue.INACTIVE, "u1");
        saveStatus("id-A", v2, TenantAccountStatusValue.ACTIVE, "u2");

        int count = sut.countActiveByTenantIdAndIss(tenantId, iss);

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("countActiveByTenantIdAndIss: 別テナントのACTIVEはカウントしない（テナント境界）")
    void countActiveByTenantIdAndIss_excludesOtherTenants() {
        String iss = "https://auth.example/realms/master";
        // tenant-A に 1件、tenant-B に 1件、両方 ACTIVE
        saveEntity("id-A", BASE_TIME, "acc-A", "tenant-A", iss, "aud", "sub-A", "u1");
        saveStatus("id-A", BASE_TIME, TenantAccountStatusValue.ACTIVE, "u1");
        saveEntity("id-B", BASE_TIME, "acc-B", "tenant-B", iss, "aud", "sub-B", "u2");
        saveStatus("id-B", BASE_TIME, TenantAccountStatusValue.ACTIVE, "u2");

        // when - tenant-A のみカウント
        int count = sut.countActiveByTenantIdAndIss("tenant-A", iss);

        // then
        assertThat(count).isEqualTo(1);
    }

}
