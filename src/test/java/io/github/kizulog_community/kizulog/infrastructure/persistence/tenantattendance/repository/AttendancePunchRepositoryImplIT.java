package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantattendance.repository;

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

import io.github.kizulog_community.kizulog.domain.tenantattendance.model.AttendancePunch;
import io.github.kizulog_community.kizulog.domain.tenantattendance.model.PunchType;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantattendance.entity.AttendancePunchEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantattendance.entity.AttendancePunchId;

/**
 * AttendancePunchRepositoryImpl の統合テスト
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(AttendancePunchRepositoryImpl.class)
class AttendancePunchRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private AttendancePunchRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private AttendancePunchJpaRepository jpaRepository;

    private static final String ACCOUNT_ID = "acc-1";
    private static final String OTHER_ACCOUNT_ID = "acc-2";
    private static final String TENANT_ID = "tenant-1";
    private static final String CREATED_BY = "acc-1";
    /** 基準時刻（UTC）。打刻に有効期限の概念は無いため任意の固定時刻でよい。 */
    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 12, 1, 9, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    /**
     * テストデータ投入ヘルパー（JPA直接保存）。
     */
    private void saveEntity(
            String punchId, OffsetDateTime version, String accountId,
            PunchType punchType, OffsetDateTime punchedAt) {
        AttendancePunchEntity entity = new AttendancePunchEntity(
                new AttendancePunchId(punchId, version),
                accountId, TENANT_ID, punchType, punchedAt, version, CREATED_BY);
        jpaRepository.save(entity);
    }

    @Test
    @DisplayName("save: ドメインモデルを保存しfindByAccountIdSinceで取得できる")
    void save_thenFindSince_returnsSaved() {
        AttendancePunch punch = new AttendancePunch(
                "pid-1", BASE_TIME, ACCOUNT_ID, TENANT_ID,
                PunchType.CLOCK_IN, BASE_TIME, BASE_TIME, CREATED_BY);
        sut.save(punch);

        List<AttendancePunch> result = sut.findByAccountIdSince(ACCOUNT_ID, BASE_TIME);

        assertThat(result).hasSize(1);
        AttendancePunch got = result.get(0);
        assertThat(got.getPunchId()).isEqualTo("pid-1");
        assertThat(got.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(got.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(got.getPunchType()).isEqualTo(PunchType.CLOCK_IN);
        assertThat(got.getPunchedAt()).isEqualTo(BASE_TIME);
    }

    @Test
    @DisplayName("findByAccountIdSince: sinceちょうどの打刻は含まれる（境界含む）")
    void findByAccountIdSince_inclusiveBoundary() {
        saveEntity("pid-1", BASE_TIME, ACCOUNT_ID, PunchType.CLOCK_IN, BASE_TIME);

        List<AttendancePunch> result = sut.findByAccountIdSince(ACCOUNT_ID, BASE_TIME);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPunchedAt()).isEqualTo(BASE_TIME);
    }

    @Test
    @DisplayName("findByAccountIdSince: sinceより前の打刻は除外される")
    void findByAccountIdSince_excludesBeforeSince() {
        saveEntity("pid-old", BASE_TIME.minusHours(1), ACCOUNT_ID, PunchType.CLOCK_IN,
                BASE_TIME.minusHours(1));
        saveEntity("pid-new", BASE_TIME, ACCOUNT_ID, PunchType.CLOCK_IN, BASE_TIME);

        List<AttendancePunch> result = sut.findByAccountIdSince(ACCOUNT_ID, BASE_TIME);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPunchId()).isEqualTo("pid-new");
    }

    @Test
    @DisplayName("findByAccountIdSince: 業務時刻の昇順で返る")
    void findByAccountIdSince_ordersByPunchedAtAsc() {
        saveEntity("pid-3", BASE_TIME.plusHours(2), ACCOUNT_ID, PunchType.BREAK_START,
                BASE_TIME.plusHours(2));
        saveEntity("pid-1", BASE_TIME, ACCOUNT_ID, PunchType.CLOCK_IN, BASE_TIME);
        saveEntity("pid-2", BASE_TIME.plusHours(1), ACCOUNT_ID, PunchType.BREAK_END,
                BASE_TIME.plusHours(1));

        List<AttendancePunch> result = sut.findByAccountIdSince(ACCOUNT_ID, BASE_TIME);

        assertThat(result).extracting(AttendancePunch::getPunchId)
                .containsExactly("pid-1", "pid-2", "pid-3");
    }

    @Test
    @DisplayName("findByAccountIdSince: 他アカウントの打刻は除外される")
    void findByAccountIdSince_filtersByAccountId() {
        saveEntity("pid-mine", BASE_TIME, ACCOUNT_ID, PunchType.CLOCK_IN, BASE_TIME);
        saveEntity("pid-other", BASE_TIME, OTHER_ACCOUNT_ID, PunchType.CLOCK_IN, BASE_TIME);

        List<AttendancePunch> result = sut.findByAccountIdSince(ACCOUNT_ID, BASE_TIME);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPunchId()).isEqualTo("pid-mine");
    }

    @Test
    @DisplayName("findByAccountIdSince: 同一punch_idは最新バージョンのみを1件返す")
    void findByAccountIdSince_returnsLatestVersionPerPunchId() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusMinutes(5);
        // v1: 業務時刻09:00、v2（訂正）: 業務時刻10:00
        saveEntity("pid-1", v1, ACCOUNT_ID, PunchType.CLOCK_IN, BASE_TIME);
        saveEntity("pid-1", v2, ACCOUNT_ID, PunchType.CLOCK_IN, BASE_TIME.plusHours(1));

        List<AttendancePunch> result = sut.findByAccountIdSince(ACCOUNT_ID, BASE_TIME);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVersion()).isEqualTo(v2);
        assertThat(result.get(0).getPunchedAt()).isEqualTo(BASE_TIME.plusHours(1));
    }

    @Test
    @DisplayName("findLatestPunchedAtByAccountIdAndType: 該当無しなら空Optional")
    void findLatest_empty_returnsEmpty() {
        Optional<OffsetDateTime> result =
                sut.findLatestPunchedAtByAccountIdAndType(ACCOUNT_ID, PunchType.CLOCK_IN);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestPunchedAtByAccountIdAndType: 同種別の最大業務時刻を返す")
    void findLatest_returnsMaxPunchedAt() {
        saveEntity("pid-1", BASE_TIME, ACCOUNT_ID, PunchType.CLOCK_IN, BASE_TIME);
        saveEntity("pid-2", BASE_TIME.plusDays(1), ACCOUNT_ID, PunchType.CLOCK_IN,
                BASE_TIME.plusDays(1));

        Optional<OffsetDateTime> result =
                sut.findLatestPunchedAtByAccountIdAndType(ACCOUNT_ID, PunchType.CLOCK_IN);

        assertThat(result).contains(BASE_TIME.plusDays(1));
    }

    @Test
    @DisplayName("findLatestPunchedAtByAccountIdAndType: 指定種別のみを対象とする")
    void findLatest_filtersByType() {
        saveEntity("pid-in", BASE_TIME, ACCOUNT_ID, PunchType.CLOCK_IN, BASE_TIME);
        saveEntity("pid-out", BASE_TIME.plusHours(8), ACCOUNT_ID, PunchType.CLOCK_OUT,
                BASE_TIME.plusHours(8));

        Optional<OffsetDateTime> result =
                sut.findLatestPunchedAtByAccountIdAndType(ACCOUNT_ID, PunchType.CLOCK_IN);

        // CLOCK_OUT(より遅い時刻)は対象外、CLOCK_INの時刻が返る
        assertThat(result).contains(BASE_TIME);
    }

    @Test
    @DisplayName("findLatestPunchedAtByAccountIdAndType: 他アカウントは対象外")
    void findLatest_filtersByAccountId() {
        saveEntity("pid-other", BASE_TIME.plusDays(1), OTHER_ACCOUNT_ID, PunchType.CLOCK_IN,
                BASE_TIME.plusDays(1));
        saveEntity("pid-mine", BASE_TIME, ACCOUNT_ID, PunchType.CLOCK_IN, BASE_TIME);

        Optional<OffsetDateTime> result =
                sut.findLatestPunchedAtByAccountIdAndType(ACCOUNT_ID, PunchType.CLOCK_IN);

        assertThat(result).contains(BASE_TIME);
    }

    @Test
    @DisplayName("findLatestPunchedAtByAccountIdAndType: 同一punch_idは最新バージョンの業務時刻を用いる")
    void findLatest_usesLatestVersionPerPunchId() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusMinutes(5);
        
        saveEntity("pid-1", v1, ACCOUNT_ID, PunchType.CLOCK_IN, BASE_TIME);
        saveEntity("pid-1", v2, ACCOUNT_ID, PunchType.CLOCK_IN, BASE_TIME.minusHours(2));

        Optional<OffsetDateTime> result =
                sut.findLatestPunchedAtByAccountIdAndType(ACCOUNT_ID, PunchType.CLOCK_IN);

        assertThat(result).contains(BASE_TIME.minusHours(2));
    }

}
