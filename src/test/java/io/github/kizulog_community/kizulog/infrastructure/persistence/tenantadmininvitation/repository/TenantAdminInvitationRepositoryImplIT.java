package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantadmininvitation.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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

import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantAdminInvitation;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantadmininvitation.entity.TenantAdminInvitationEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantadmininvitation.entity.TenantAdminInvitationId;

/**
 * TenantAdminInvitationRepositoryImpl の統合テスト
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TenantAdminInvitationRepositoryImpl.class)
class TenantAdminInvitationRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private TenantAdminInvitationRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private TenantAdminInvitationJpaRepository jpaRepository;

    /** テスト用基準時刻（UTC） */
    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    @Test
    @DisplayName("findLatestByInvitationId: 同一invitation_idで複数バージョンが存在する場合、最大versionのレコードを返す")
    void findLatestByInvitationId_returnsLatestVersion_whenMultipleVersionsExist() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity("inv-1", v1, "tenant-A", "hash-1", v1.plusDays(1), "User v1", "creator-1");
        saveEntity("inv-1", v3, "tenant-A", "hash-3", v3.plusDays(1), "User v3", "creator-3");
        saveEntity("inv-1", v2, "tenant-A", "hash-2", v2.plusDays(1), "User v2", "creator-2");

        Optional<TenantAdminInvitation> result = sut.findLatestByInvitationId("inv-1");

        assertThat(result).isPresent();
        assertThat(result.get().getInvitationId()).isEqualTo("inv-1");
        assertThat(result.get().getVersion()).isEqualTo(v3);
        assertThat(result.get().getTenantId()).isEqualTo("tenant-A");
        assertThat(result.get().getTokenHash()).isEqualTo("hash-3");
        assertThat(result.get().getDisplayName()).isEqualTo("User v3");
        assertThat(result.get().getCreatedBy()).isEqualTo("creator-3");
    }

    @Test
    @DisplayName("findLatestByInvitationId: 単一バージョンしか存在しない場合、そのレコードを返す")
    void findLatestByInvitationId_returnsSingleVersion_whenOnlyOneExists() {
        saveEntity("inv-only", BASE_TIME, "tenant-A", "hash-x", BASE_TIME.plusDays(1),
                "Sole User", "creator");

        Optional<TenantAdminInvitation> result = sut.findLatestByInvitationId("inv-only");

        assertThat(result).isPresent();
        assertThat(result.get().getDisplayName()).isEqualTo("Sole User");
    }

    @Test
    @DisplayName("findLatestByInvitationId: 該当invitation_idが存在しない場合、空のOptionalを返す")
    void findLatestByInvitationId_returnsEmpty_whenNotFound() {
        Optional<TenantAdminInvitation> result = sut.findLatestByInvitationId("missing");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByInvitationId: 他のinvitation_idのレコードのみ存在する場合、空のOptionalを返す")
    void findLatestByInvitationId_returnsEmpty_whenOtherIdsExist() {
        saveEntity("inv-A", BASE_TIME, "tenant-A", "hash-A", BASE_TIME.plusDays(1),
                "A", "creator-A");

        Optional<TenantAdminInvitation> result = sut.findLatestByInvitationId("inv-B");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByTokenHash: 存在するtoken_hashの場合、該当レコードを返す")
    void findByTokenHash_returnsRecord_whenExists() {
        saveEntity("inv-1", BASE_TIME, "tenant-A", "unique-hash", BASE_TIME.plusDays(1),
                "User", "creator");

        Optional<TenantAdminInvitation> result = sut.findByTokenHash("unique-hash");

        assertThat(result).isPresent();
        assertThat(result.get().getInvitationId()).isEqualTo("inv-1");
        assertThat(result.get().getTenantId()).isEqualTo("tenant-A");
        assertThat(result.get().getTokenHash()).isEqualTo("unique-hash");
    }

    @Test
    @DisplayName("findByTokenHash: 同一invitation_idの旧バージョンのtoken_hashでも該当レコードを返す")
    void findByTokenHash_returnsOldVersionRecord_whenSearchedWithOldHash() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        saveEntity("inv-1", v1, "tenant-A", "hash-old", v1.plusDays(1), "User v1", "creator");
        saveEntity("inv-1", v2, "tenant-A", "hash-new", v2.plusDays(1), "User v2", "creator");

        Optional<TenantAdminInvitation> result = sut.findByTokenHash("hash-old");

        assertThat(result).isPresent();
        assertThat(result.get().getInvitationId()).isEqualTo("inv-1");
        assertThat(result.get().getVersion()).isEqualTo(v1);
        assertThat(result.get().getTokenHash()).isEqualTo("hash-old");
    }

    @Test
    @DisplayName("findByTokenHash: 該当token_hashが存在しない場合、空のOptionalを返す")
    void findByTokenHash_returnsEmpty_whenNotFound() {
        saveEntity("inv-1", BASE_TIME, "tenant-A", "exists", BASE_TIME.plusDays(1),
                "User", "creator");

        Optional<TenantAdminInvitation> result = sut.findByTokenHash("nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByTokenHash: テーブルが空の場合、空のOptionalを返す")
    void findByTokenHash_returnsEmpty_whenTableIsEmpty() {
        Optional<TenantAdminInvitation> result = sut.findByTokenHash("any");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllLatestByTenantId: 複数のinvitation_idがある場合、各IDの最新versionのみを返す")
    void findAllLatestByTenantId_returnsLatestVersionPerInvitationId() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        saveEntity("inv-A", v1, "tenant-1", "A-v1", v1.plusDays(1), "A v1", "creator");
        saveEntity("inv-A", v2, "tenant-1", "A-v2", v2.plusDays(1), "A v2", "creator");
        saveEntity("inv-B", v1, "tenant-1", "B-v1", v1.plusDays(1), "B v1", "creator");

        List<TenantAdminInvitation> result = sut.findAllLatestByTenantId("tenant-1");

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(TenantAdminInvitation::getInvitationId,
                        TenantAdminInvitation::getTokenHash)
                .containsExactlyInAnyOrder(
                        tuple("inv-A", "A-v2"),
                        tuple("inv-B", "B-v1"));
    }

    @Test
    @DisplayName("findAllLatestByTenantId: テーブルが空の場合、空のリストを返す")
    void findAllLatestByTenantId_returnsEmpty_whenTableIsEmpty() {
        List<TenantAdminInvitation> result = sut.findAllLatestByTenantId("tenant-1");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllLatestByTenantId: 1件のみの場合、その1件を返す")
    void findAllLatestByTenantId_returnsSingle_whenOnlyOneExists() {
        saveEntity("inv-1", BASE_TIME, "tenant-1", "hash-1", BASE_TIME.plusDays(1),
                "User", "creator");

        List<TenantAdminInvitation> result = sut.findAllLatestByTenantId("tenant-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInvitationId()).isEqualTo("inv-1");
    }

    @Test
    @DisplayName("findAllLatestByTenantId: 別テナントの招待は含まない（テナント境界）")
    void findAllLatestByTenantId_excludesOtherTenants() {
        saveEntity("inv-A1", BASE_TIME, "tenant-1", "hash-A1", BASE_TIME.plusDays(1),
                "A1", "creator");
        saveEntity("inv-A2", BASE_TIME.plusHours(1), "tenant-1", "hash-A2",
                BASE_TIME.plusDays(1), "A2", "creator");
        saveEntity("inv-B1", BASE_TIME.plusHours(2), "tenant-2", "hash-B1",
                BASE_TIME.plusDays(1), "B1", "creator");

        List<TenantAdminInvitation> result = sut.findAllLatestByTenantId("tenant-1");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TenantAdminInvitation::getInvitationId)
                .containsExactlyInAnyOrder("inv-A1", "inv-A2");
        assertThat(result).extracting(TenantAdminInvitation::getTenantId)
                .containsOnly("tenant-1");
    }

    @Test
    @DisplayName("findAllLatestByTenantId: 該当テナントに招待がない場合、空のリストを返す")
    void findAllLatestByTenantId_returnsEmpty_whenTenantHasNoInvitations() {
        saveEntity("inv-1", BASE_TIME, "tenant-1", "hash-1", BASE_TIME.plusDays(1),
                "User", "creator");

        List<TenantAdminInvitation> result = sut.findAllLatestByTenantId("tenant-2");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save: 新規レコードを永続化し、全フィールドがDBから取得可能になる（tenantId含む）")
    void save_persistsNewRecord_withAllFields() {
        OffsetDateTime version = BASE_TIME;
        OffsetDateTime expiresAt = BASE_TIME.plusDays(1);
        TenantAdminInvitation domain = new TenantAdminInvitation(
                "inv-new", version, "tenant-A", "hash-new", expiresAt,
                "Display Name", version, "creator-new");

        sut.save(domain);

        Optional<TenantAdminInvitation> result = sut.findLatestByInvitationId("inv-new");
        assertThat(result).isPresent();
        TenantAdminInvitation persisted = result.get();
        assertThat(persisted.getInvitationId()).isEqualTo("inv-new");
        assertThat(persisted.getVersion()).isEqualTo(version);
        assertThat(persisted.getTenantId()).isEqualTo("tenant-A");
        assertThat(persisted.getTokenHash()).isEqualTo("hash-new");
        assertThat(persisted.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(persisted.getDisplayName()).isEqualTo("Display Name");
        assertThat(persisted.getCreatedAt()).isEqualTo(version);
        assertThat(persisted.getCreatedBy()).isEqualTo("creator-new");
    }

    @Test
    @DisplayName("save: 同一invitation_idで複数バージョンを保存できる(再発行)")
    void save_savesMultipleVersionsForSameInvitationId() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);

        sut.save(new TenantAdminInvitation(
                "inv-1", v1, "tenant-A", "hash-v1", v1.plusDays(1), "User", v1, "creator"));
        sut.save(new TenantAdminInvitation(
                "inv-1", v2, "tenant-A", "hash-v2", v2.plusDays(1), "User", v2, "creator"));

        assertThat(jpaRepository.count()).isEqualTo(2);
        Optional<TenantAdminInvitation> latest = sut.findLatestByInvitationId("inv-1");
        assertThat(latest).isPresent();
        assertThat(latest.get().getVersion()).isEqualTo(v2);
        assertThat(latest.get().getTokenHash()).isEqualTo("hash-v2");
    }

    private void saveEntity(
            String invitationId,
            OffsetDateTime version,
            String tenantId,
            String tokenHash,
            OffsetDateTime expiresAt,
            String displayName,
            String createdBy) {
        TenantAdminInvitationEntity entity = new TenantAdminInvitationEntity(
                new TenantAdminInvitationId(invitationId, version),
                tenantId,
                tokenHash,
                expiresAt,
                displayName,
                version,
                createdBy);
        jpaRepository.save(entity);
    }

}
