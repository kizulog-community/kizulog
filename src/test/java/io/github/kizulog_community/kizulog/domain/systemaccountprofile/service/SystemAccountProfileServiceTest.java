package io.github.kizulog_community.kizulog.domain.systemaccountprofile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.github.kizulog_community.kizulog.domain.systemaccountprofile.exception.AccountProfileError;
import io.github.kizulog_community.kizulog.domain.systemaccountprofile.exception.AccountProfileException;
import io.github.kizulog_community.kizulog.domain.systemaccountprofile.model.SystemAccountProfile;
import io.github.kizulog_community.kizulog.domain.systemaccountprofile.port.SystemAccountProfileRepository;

/**
 * SystemAccountProfileServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemAccountProfileServiceTest {

    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    private SystemAccountProfileRepository repository;
    private SystemAccountProfileService sut;

    @BeforeEach
    void setUp() {
        repository = mock(SystemAccountProfileRepository.class);
        sut = new SystemAccountProfileService(repository);
    }

    @Test
    @DisplayName("getProfile: プロファイルが存在する場合はOptionalで返す")
    void getProfile_returnsValue_whenExists() {
        Map<String, Object> claims = Map.of("name", "山田 太郎");
        SystemAccountProfile stored = new SystemAccountProfile(
                "id-1", BASE_TIME, claims, BASE_TIME, "sub-1");
        when(repository.findLatestByIdentityId("id-1")).thenReturn(Optional.of(stored));

        Optional<SystemAccountProfile> result = sut.getProfile("id-1");

        assertThat(result).isPresent();
        assertThat(result.get().getIdentityId()).isEqualTo("id-1");
        assertThat(result.get().getClaims().get("name")).isEqualTo("山田 太郎");
    }

    @Test
    @DisplayName("getProfile: プロファイルが未登録の場合は空のOptionalを返す（例外ではない）")
    void getProfile_returnsEmpty_whenNotExists() {
        when(repository.findLatestByIdentityId("id-1")).thenReturn(Optional.empty());

        Optional<SystemAccountProfile> result = sut.getProfile("id-1");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getProfile: identityIdがnullの場合はIDENTITY_ID_REQUIRED例外")
    void getProfile_throwsException_whenIdentityIdIsNull() {
        assertThatThrownBy(() -> sut.getProfile(null))
                .isInstanceOf(AccountProfileException.class)
                .extracting("error")
                .isEqualTo(AccountProfileError.IDENTITY_ID_REQUIRED);
    }

    @Test
    @DisplayName("getProfile: identityIdが空白の場合はIDENTITY_ID_REQUIRED例外")
    void getProfile_throwsException_whenIdentityIdIsBlank() {
        assertThatThrownBy(() -> sut.getProfile("  "))
                .isInstanceOf(AccountProfileException.class)
                .extracting("error")
                .isEqualTo(AccountProfileError.IDENTITY_ID_REQUIRED);
    }

    @Test
    @DisplayName("upsertIfChanged: 既存なしの場合は新version保存される")
    void upsertIfChanged_savesNewVersion_whenNotExists() {
        when(repository.findLatestByIdentityId("id-1")).thenReturn(Optional.empty());
        Map<String, Object> newClaims = Map.of("name", "山田 太郎");

        sut.upsertIfChanged("id-1", newClaims, "sub-1");

        ArgumentCaptor<SystemAccountProfile> captor =
                ArgumentCaptor.forClass(SystemAccountProfile.class);
        verify(repository).save(captor.capture());

        SystemAccountProfile saved = captor.getValue();
        assertThat(saved.getIdentityId()).isEqualTo("id-1");
        assertThat(saved.getClaims()).isEqualTo(newClaims);
        assertThat(saved.getCreatedBy()).isEqualTo("sub-1");
        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.getCreatedAt()).isEqualTo(saved.getVersion());
    }

    @Test
    @DisplayName("upsertIfChanged: 既存と同じクレームならsaveしない（DB肥大化抑制）")
    void upsertIfChanged_skipsSave_whenClaimsUnchanged() {
        Map<String, Object> existingClaims = new LinkedHashMap<>();
        existingClaims.put("name", "山田 太郎");
        existingClaims.put("email", "taro@example.com");

        SystemAccountProfile existing = new SystemAccountProfile(
                "id-1", BASE_TIME, existingClaims, BASE_TIME, "sub-1");
        when(repository.findLatestByIdentityId("id-1")).thenReturn(Optional.of(existing));

        // 同じクレーム（順序違いだがdeep-equalで等価）を渡す
        Map<String, Object> newClaims = new LinkedHashMap<>();
        newClaims.put("email", "taro@example.com");
        newClaims.put("name", "山田 太郎");

        sut.upsertIfChanged("id-1", newClaims, "sub-1");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("upsertIfChanged: 既存とクレームが異なる場合は新version保存される")
    void upsertIfChanged_savesNewVersion_whenClaimsChanged() {
        Map<String, Object> existingClaims = Map.of("name", "山田 太郎");
        SystemAccountProfile existing = new SystemAccountProfile(
                "id-1", BASE_TIME, existingClaims, BASE_TIME, "sub-1");
        when(repository.findLatestByIdentityId("id-1")).thenReturn(Optional.of(existing));

        Map<String, Object> newClaims = Map.of("name", "山田 太郎", "organization", "開発部");

        sut.upsertIfChanged("id-1", newClaims, "sub-1");

        ArgumentCaptor<SystemAccountProfile> captor =
                ArgumentCaptor.forClass(SystemAccountProfile.class);
        verify(repository).save(captor.capture());

        SystemAccountProfile saved = captor.getValue();
        assertThat(saved.getClaims()).isEqualTo(newClaims);
        // 新versionが既存versionより新しい
        assertThat(saved.getVersion()).isAfter(BASE_TIME);
    }

    @Test
    @DisplayName("upsertIfChanged: claimsがnullの場合はCLAIMS_REQUIRED例外")
    void upsertIfChanged_throwsException_whenClaimsIsNull() {
        assertThatThrownBy(() -> sut.upsertIfChanged("id-1", null, "sub-1"))
                .isInstanceOf(AccountProfileException.class)
                .extracting("error")
                .isEqualTo(AccountProfileError.CLAIMS_REQUIRED);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("upsertIfChanged: claimsが空の場合はCLAIMS_REQUIRED例外")
    void upsertIfChanged_throwsException_whenClaimsIsEmpty() {
        assertThatThrownBy(() -> sut.upsertIfChanged("id-1", Map.of(), "sub-1"))
                .isInstanceOf(AccountProfileException.class)
                .extracting("error")
                .isEqualTo(AccountProfileError.CLAIMS_REQUIRED);

        verify(repository, never()).save(any());
    }

}
