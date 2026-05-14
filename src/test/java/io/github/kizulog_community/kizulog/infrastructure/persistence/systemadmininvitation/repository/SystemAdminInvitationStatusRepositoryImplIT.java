package io.github.kizulog_community.kizulog.infrastructure.persistence.systemadmininvitation.repository;

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

import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.InvitationStatusValue;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.SystemAdminInvitationStatus;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemadmininvitation.entity.SystemAdminInvitationStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemadmininvitation.entity.SystemAdminInvitationStatusId;

/**
 * SystemAdminInvitationStatusRepositoryImpl の統合テスト
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(SystemAdminInvitationStatusRepositoryImpl.class)
class SystemAdminInvitationStatusRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private SystemAdminInvitationStatusRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private SystemAdminInvitationStatusJpaRepository jpaRepository;

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
        saveEntity("inv-1", v1, InvitationStatusValue.PENDING, null, "creator-1");
        saveEntity("inv-1", v3, InvitationStatusValue.USED, null, "creator-3");
        saveEntity("inv-1", v2, InvitationStatusValue.CANCELLED, "誤発行", "creator-2");

        Optional<SystemAdminInvitationStatus> result =
                sut.findLatestByInvitationId("inv-1");

        assertThat(result).isPresent();
        assertThat(result.get().getInvitationId()).isEqualTo("inv-1");
        assertThat(result.get().getVersion()).isEqualTo(v3);
        assertThat(result.get().getStatus()).isEqualTo(InvitationStatusValue.USED);
        assertThat(result.get().getCreatedBy()).isEqualTo("creator-3");
    }

    @Test
    @DisplayName("findLatestByInvitationId: 単一バージョンしか存在しない場合、そのレコードを返す")
    void findLatestByInvitationId_returnsSingleVersion_whenOnlyOneExists() {
        saveEntity("inv-only", BASE_TIME, InvitationStatusValue.PENDING, null, "creator");

        Optional<SystemAdminInvitationStatus> result =
                sut.findLatestByInvitationId("inv-only");

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(InvitationStatusValue.PENDING);
    }

    @Test
    @DisplayName("findLatestByInvitationId: 該当invitation_idが存在しない場合、空のOptionalを返す")
    void findLatestByInvitationId_returnsEmpty_whenNotFound() {
        Optional<SystemAdminInvitationStatus> result = sut.findLatestByInvitationId("missing");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByInvitationId: reasonがnullでも正しく取得できる")
    void findLatestByInvitationId_returnsRecordWithNullReason() {
        saveEntity("inv-1", BASE_TIME, InvitationStatusValue.PENDING, null, "creator");

        Optional<SystemAdminInvitationStatus> result = sut.findLatestByInvitationId("inv-1");

        assertThat(result).isPresent();
        assertThat(result.get().getReason()).isNull();
    }

    @Test
    @DisplayName("findLatestByInvitationId: reasonに値がある場合、正しく取得できる")
    void findLatestByInvitationId_returnsRecordWithReason() {
        saveEntity("inv-1", BASE_TIME, InvitationStatusValue.CANCELLED,
                "招待相手退職のため", "creator");

        Optional<SystemAdminInvitationStatus> result = sut.findLatestByInvitationId("inv-1");

        assertThat(result).isPresent();
        assertThat(result.get().getReason()).isEqualTo("招待相手退職のため");
    }

    @Test
    @DisplayName("findInvitationIdsByLatestStatus: 各invitation_idの最新versionがPENDINGのものだけ返す")
    void findInvitationIdsByLatestStatus_returnsOnlyLatestPending() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);

        // inv-A: PENDING のみ → 該当
        saveEntity("inv-A", v1, InvitationStatusValue.PENDING, null, "creator");
        // inv-B: PENDING→USED (最新USED) → 非該当
        saveEntity("inv-B", v1, InvitationStatusValue.PENDING, null, "creator");
        saveEntity("inv-B", v2, InvitationStatusValue.USED, null, "creator");
        // inv-C: PENDING→CANCELLED (最新CANCELLED) → 非該当
        saveEntity("inv-C", v1, InvitationStatusValue.PENDING, null, "creator");
        saveEntity("inv-C", v2, InvitationStatusValue.CANCELLED, "reason", "creator");
        // inv-D: PENDING のみ → 該当
        saveEntity("inv-D", v2, InvitationStatusValue.PENDING, null, "creator");

        List<String> result = sut.findInvitationIdsByLatestStatus(
                InvitationStatusValue.PENDING);

        assertThat(result).containsExactlyInAnyOrder("inv-A", "inv-D");
    }

    @Test
    @DisplayName("findInvitationIdsByLatestStatus: USEDの抽出")
    void findInvitationIdsByLatestStatus_findsUsed() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);

        saveEntity("inv-A", v1, InvitationStatusValue.PENDING, null, "creator");
        saveEntity("inv-A", v2, InvitationStatusValue.USED, null, "creator");
        saveEntity("inv-B", v1, InvitationStatusValue.PENDING, null, "creator");

        List<String> result = sut.findInvitationIdsByLatestStatus(InvitationStatusValue.USED);

        assertThat(result).containsExactly("inv-A");
    }

    @Test
    @DisplayName("findInvitationIdsByLatestStatus: CANCELLEDの抽出")
    void findInvitationIdsByLatestStatus_findsCancelled() {
        saveEntity("inv-A", BASE_TIME, InvitationStatusValue.PENDING, null, "creator");
        saveEntity("inv-A", BASE_TIME.plusHours(1), InvitationStatusValue.CANCELLED,
                "reason", "creator");

        List<String> result = sut.findInvitationIdsByLatestStatus(
                InvitationStatusValue.CANCELLED);

        assertThat(result).containsExactly("inv-A");
    }

    @Test
    @DisplayName("findInvitationIdsByLatestStatus: テーブルが空の場合、空のリストを返す")
    void findInvitationIdsByLatestStatus_returnsEmpty_whenTableIsEmpty() {
        List<String> result = sut.findInvitationIdsByLatestStatus(
                InvitationStatusValue.PENDING);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findInvitationIdsByLatestStatus: 該当ステータスが0件の場合、空のリストを返す")
    void findInvitationIdsByLatestStatus_returnsEmpty_whenNoMatches() {
        saveEntity("inv-A", BASE_TIME, InvitationStatusValue.PENDING, null, "creator");

        List<String> result = sut.findInvitationIdsByLatestStatus(
                InvitationStatusValue.USED);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save: 新規レコードを永続化し、全フィールドがDBから取得可能になる")
    void save_persistsNewRecord_withAllFields() {
        OffsetDateTime version = BASE_TIME;
        SystemAdminInvitationStatus domain = new SystemAdminInvitationStatus(
                "inv-new", version, InvitationStatusValue.PENDING,
                null, version, "creator-new");

        sut.save(domain);

        Optional<SystemAdminInvitationStatus> result = sut.findLatestByInvitationId("inv-new");
        assertThat(result).isPresent();
        SystemAdminInvitationStatus persisted = result.get();
        assertThat(persisted.getInvitationId()).isEqualTo("inv-new");
        assertThat(persisted.getVersion()).isEqualTo(version);
        assertThat(persisted.getStatus()).isEqualTo(InvitationStatusValue.PENDING);
        assertThat(persisted.getReason()).isNull();
        assertThat(persisted.getCreatedAt()).isEqualTo(version);
        assertThat(persisted.getCreatedBy()).isEqualTo("creator-new");
    }

    @Test
    @DisplayName("save: reasonに値があるレコードを永続化できる")
    void save_persistsRecordWithReason() {
        OffsetDateTime version = BASE_TIME;
        SystemAdminInvitationStatus domain = new SystemAdminInvitationStatus(
                "inv-1", version, InvitationStatusValue.CANCELLED,
                "テスト理由", version, "creator");

        sut.save(domain);

        Optional<SystemAdminInvitationStatus> result = sut.findLatestByInvitationId("inv-1");
        assertThat(result).isPresent();
        assertThat(result.get().getReason()).isEqualTo("テスト理由");
    }

    @Test
    @DisplayName("save: 同一invitation_idで複数バージョンを保存できる(状態遷移)")
    void save_savesMultipleVersionsForSameInvitationId() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);

        sut.save(new SystemAdminInvitationStatus(
                "inv-1", v1, InvitationStatusValue.PENDING, null, v1, "creator"));
        sut.save(new SystemAdminInvitationStatus(
                "inv-1", v2, InvitationStatusValue.USED, null, v2, "acceptor"));

        assertThat(jpaRepository.count()).isEqualTo(2);
        Optional<SystemAdminInvitationStatus> latest = sut.findLatestByInvitationId("inv-1");
        assertThat(latest).isPresent();
        assertThat(latest.get().getVersion()).isEqualTo(v2);
        assertThat(latest.get().getStatus()).isEqualTo(InvitationStatusValue.USED);
        assertThat(latest.get().getCreatedBy()).isEqualTo("acceptor");
    }

    private void saveEntity(
            String invitationId,
            OffsetDateTime version,
            InvitationStatusValue status,
            String reason,
            String createdBy) {
        SystemAdminInvitationStatusEntity entity = new SystemAdminInvitationStatusEntity(
                new SystemAdminInvitationStatusId(invitationId, version),
                status,
                reason,
                version,
                createdBy);
        jpaRepository.save(entity);
    }

}
