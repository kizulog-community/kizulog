package io.github.kizulog_community.kizulog.domain.tenantaccountprofile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.github.kizulog_community.kizulog.domain.tenantaccountprofile.exception.AccountProfileError;
import io.github.kizulog_community.kizulog.domain.tenantaccountprofile.exception.AccountProfileException;
import io.github.kizulog_community.kizulog.domain.tenantaccountprofile.model.TenantAccountProfile;
import io.github.kizulog_community.kizulog.domain.tenantaccountprofile.port.TenantAccountProfileRepository;

/**
 * TenantAccountProfileServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantAccountProfileServiceTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);
    private static final String IDENTITY_ID = "id-1";
    private static final String CREATED_BY = "sub-1";

    private TenantAccountProfileRepository repository;
    private TenantAccountProfileService service;

    @BeforeEach
    void setUp() {
        repository = mock(TenantAccountProfileRepository.class);
        service = new TenantAccountProfileService(repository);
    }

    private TenantAccountProfile profileOf(Map<String, Object> claims) {
        return new TenantAccountProfile(IDENTITY_ID, BASE_TIME, claims, BASE_TIME, CREATED_BY);
    }

    @Test
    @DisplayName("getProfile: リポジトリの最新版をそのまま返す")
    void getProfile_returnsLatestFromRepository() {
        TenantAccountProfile profile = profileOf(Map.of("family_name", "山田"));
        when(repository.findLatestByIdentityId(IDENTITY_ID)).thenReturn(Optional.of(profile));

        Optional<TenantAccountProfile> result = service.getProfile(IDENTITY_ID);

        assertThat(result).containsSame(profile);
    }

    @Test
    @DisplayName("getProfile: identityIdが空の場合、IDENTITY_ID_REQUIREDをスロー")
    void getProfile_throws_whenIdentityIdBlank() {
        assertThatThrownBy(() -> service.getProfile("  "))
                .isInstanceOf(AccountProfileException.class)
                .extracting(e -> ((AccountProfileException) e).getError())
                .isEqualTo(AccountProfileError.IDENTITY_ID_REQUIRED);
    }

    @Test
    @DisplayName("upsertIfChanged: 既存なしの場合、新バージョンを保存する")
    void upsertIfChanged_savesNewVersion_whenNoExisting() {
        when(repository.findLatestByIdentityId(IDENTITY_ID)).thenReturn(Optional.empty());
        Map<String, Object> claims = Map.of("family_name", "山田", "given_name", "太郎");

        service.upsertIfChanged(IDENTITY_ID, claims, CREATED_BY);

        ArgumentCaptor<TenantAccountProfile> captor =
                ArgumentCaptor.forClass(TenantAccountProfile.class);
        verify(repository).save(captor.capture());
        TenantAccountProfile saved = captor.getValue();
        assertThat(saved.getIdentityId()).isEqualTo(IDENTITY_ID);
        assertThat(saved.getClaims()).isEqualTo(claims);
        assertThat(saved.getCreatedBy()).isEqualTo(CREATED_BY);
    }

    @Test
    @DisplayName("upsertIfChanged: クレームが既存と同一の場合、saveをスキップする")
    void upsertIfChanged_skipsSave_whenClaimsUnchanged() {
        Map<String, Object> claims = Map.of("family_name", "山田");
        when(repository.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(profileOf(claims)));

        service.upsertIfChanged(IDENTITY_ID, Map.of("family_name", "山田"), CREATED_BY);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("upsertIfChanged: クレームに差分がある場合、新バージョンを保存する")
    void upsertIfChanged_savesNewVersion_whenClaimsChanged() {
        when(repository.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(profileOf(Map.of("family_name", "山田"))));
        Map<String, Object> newClaims = Map.of("family_name", "佐藤");

        service.upsertIfChanged(IDENTITY_ID, newClaims, CREATED_BY);

        ArgumentCaptor<TenantAccountProfile> captor =
                ArgumentCaptor.forClass(TenantAccountProfile.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getClaims()).isEqualTo(newClaims);
    }

    @Test
    @DisplayName("upsertIfChanged: claimsが空の場合、CLAIMS_REQUIREDをスロー")
    void upsertIfChanged_throws_whenClaimsEmpty() {
        assertThatThrownBy(() -> service.upsertIfChanged(IDENTITY_ID, Map.of(), CREATED_BY))
                .isInstanceOf(AccountProfileException.class)
                .extracting(e -> ((AccountProfileException) e).getError())
                .isEqualTo(AccountProfileError.CLAIMS_REQUIRED);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("upsertIfChanged: identityIdが空の場合、IDENTITY_ID_REQUIREDをスロー")
    void upsertIfChanged_throws_whenIdentityIdBlank() {
        assertThatThrownBy(() ->
                service.upsertIfChanged("", Map.of("family_name", "山田"), CREATED_BY))
                .isInstanceOf(AccountProfileException.class)
                .extracting(e -> ((AccountProfileException) e).getError())
                .isEqualTo(AccountProfileError.IDENTITY_ID_REQUIRED);
    }

}
