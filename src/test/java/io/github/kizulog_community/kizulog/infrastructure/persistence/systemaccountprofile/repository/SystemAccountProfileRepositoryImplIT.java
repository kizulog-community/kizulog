package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountprofile.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;

import io.github.kizulog_community.kizulog.domain.systemaccountprofile.model.SystemAccountProfile;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountprofile.entity.SystemAccountProfileEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountprofile.entity.SystemAccountProfileId;

/**
 * SystemAccountProfileRepositoryImplの統合テスト
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(SystemAccountProfileRepositoryImpl.class)
class SystemAccountProfileRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private SystemAccountProfileRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private SystemAccountProfileJpaRepository jpaRepository;

    /** テスト用基準時刻（UTC） */
    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    @Test
    @DisplayName("findLatestByIdentityId: 単一バージョンしか存在しない場合、そのレコードを返す")
    void findLatestByIdentityId_returnsSingleVersion_whenOnlyOneExists() {
        Map<String, Object> claims = Map.of("name", "山田 太郎");
        saveEntity("id-1", BASE_TIME, claims, "sub-1");

        Optional<SystemAccountProfile> result = sut.findLatestByIdentityId("id-1");

        assertThat(result).isPresent();
        assertThat(result.get().getIdentityId()).isEqualTo("id-1");
        assertThat(result.get().getVersion()).isEqualTo(BASE_TIME);
        assertThat(result.get().getClaims().get("name")).isEqualTo("山田 太郎");
        assertThat(result.get().getCreatedBy()).isEqualTo("sub-1");
    }

    @Test
    @DisplayName("findLatestByIdentityId: 同一identityIdで複数バージョンが存在する場合、最大versionを返す")
    void findLatestByIdentityId_returnsLatestVersion_whenMultipleVersionsExist() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity("id-1", v1, Map.of("name", "v1"), "sub-1");
        saveEntity("id-1", v3, Map.of("name", "v3"), "sub-1");
        saveEntity("id-1", v2, Map.of("name", "v2"), "sub-1");

        Optional<SystemAccountProfile> result = sut.findLatestByIdentityId("id-1");

        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo(v3);
        assertThat(result.get().getClaims().get("name")).isEqualTo("v3");
    }

    @Test
    @DisplayName("findLatestByIdentityId: 該当identityIdが存在しない場合は空のOptionalを返す")
    void findLatestByIdentityId_returnsEmpty_whenNotFound() {
        saveEntity("id-1", BASE_TIME, Map.of("name", "山田"), "sub-1");

        Optional<SystemAccountProfile> result = sut.findLatestByIdentityId("id-2");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByIdentityId: テーブルが空の場合は空のOptionalを返す")
    void findLatestByIdentityId_returnsEmpty_whenTableIsEmpty() {
        Optional<SystemAccountProfile> result = sut.findLatestByIdentityId("id-1");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save: 新規プロファイルを保存し、findで完全に復元できる（JSONB往復検証）")
    void save_persistsAllFields_andCanBeRestoredViaFind() {
        // 多様な型を含むクレーム
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("name", "山田 太郎");
        claims.put("email", "taro@example.com");
        claims.put("email_verified", Boolean.TRUE);
        claims.put("updated_at", 1700000000L);
        claims.put("organization", "開発部");
        // ネストした構造（OIDCで稀にある）
        claims.put("address", Map.of("country", "JP", "city", "Tokyo"));

        SystemAccountProfile profile = new SystemAccountProfile(
                "id-1", BASE_TIME, claims, BASE_TIME, "sub-1");
        sut.save(profile);

        Optional<SystemAccountProfile> restored = sut.findLatestByIdentityId("id-1");

        assertThat(restored).isPresent();
        SystemAccountProfile r = restored.get();
        assertThat(r.getIdentityId()).isEqualTo("id-1");
        assertThat(r.getVersion()).isEqualTo(BASE_TIME);
        assertThat(r.getCreatedAt()).isEqualTo(BASE_TIME);
        assertThat(r.getCreatedBy()).isEqualTo("sub-1");

        // JSONB往復検証: 全フィールドが取り出せること
        Map<String, Object> rc = r.getClaims();
        assertThat(rc).containsEntry("name", "山田 太郎");
        assertThat(rc).containsEntry("email", "taro@example.com");
        assertThat(rc).containsEntry("email_verified", Boolean.TRUE);
        // 数値はLong or Integer等の表現になり得るためtoString比較で型差を吸収
        assertThat(rc.get("updated_at").toString()).isEqualTo("1700000000");
        assertThat(rc).containsEntry("organization", "開発部");
        // ネストMapも復元される
        assertThat(rc.get("address")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> address = (Map<String, Object>) rc.get("address");
        assertThat(address).containsEntry("country", "JP");
        assertThat(address).containsEntry("city", "Tokyo");
    }

    @Test
    @DisplayName("save: 同一identityIdで異なるversionを保存すると履歴として両方残る")
    void save_keepsHistoryWithDifferentVersions() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);

        sut.save(new SystemAccountProfile(
                "id-1", v1, Map.of("name", "旧名前"), v1, "sub-1"));
        sut.save(new SystemAccountProfile(
                "id-1", v2, Map.of("name", "新名前"), v2, "sub-1"));

        // findLatestは最新のみ返す
        Optional<SystemAccountProfile> latest = sut.findLatestByIdentityId("id-1");
        assertThat(latest).isPresent();
        assertThat(latest.get().getVersion()).isEqualTo(v2);
        assertThat(latest.get().getClaims().get("name")).isEqualTo("新名前");

        // JPA直接アクセスで履歴が残っていることを確認
        assertThat(jpaRepository.count()).isEqualTo(2);
    }

    /**
     * テストデータ投入ヘルパー
     *
     * @param identityId Identity ID
     * @param version バージョン
     * @param claims クレーム
     * @param createdBy 作成者
     */
    private void saveEntity(
            String identityId,
            OffsetDateTime version,
            Map<String, Object> claims,
            String createdBy) {
        SystemAccountProfileEntity entity = new SystemAccountProfileEntity(
                new SystemAccountProfileId(identityId, version),
                claims,
                version,
                createdBy);
        jpaRepository.save(entity);
    }

}
