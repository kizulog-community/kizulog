package io.github.kizulog_community.kizulog.domain.systemaccount.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.github.kizulog_community.kizulog.domain.systemaccount.exception.IdentityLinkError;
import io.github.kizulog_community.kizulog.domain.systemaccount.exception.IdentityLinkException;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.LinkedIdentityView;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentity;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentityStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityStatusRepository;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.DecryptedOidcProvider;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.OidcProviderStatusValue;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.ProviderWithStatus;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProvider;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProviderStatus;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.SystemOidcProviderService;

/**
 * SystemAccountIdentityLinkServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemAccountIdentityLinkServiceTest {

    private SystemAccountIdentityRepository identityRepository;
    private SystemAccountIdentityStatusRepository identityStatusRepository;
    private SystemOidcProviderService systemOidcProviderService;

    private SystemAccountIdentityLinkService sut;

    @BeforeEach
    void setUp() {
        identityRepository = mock(SystemAccountIdentityRepository.class);
        identityStatusRepository = mock(SystemAccountIdentityStatusRepository.class);
        systemOidcProviderService = mock(SystemOidcProviderService.class);

        sut = new SystemAccountIdentityLinkService(
                identityRepository,
                identityStatusRepository,
                systemOidcProviderService);
    }

    @Test
    @DisplayName("linkIdentity: 正常系: provider存在・重複なし→identity+status(ACTIVE)を作成")
    void linkIdentity_success() {
        String accountId = "acc-1";
        String providerId = "google";
        String iss = "https://accounts.google.com";
        String aud = "client-1";
        String sub = "google-sub-1";

        when(systemOidcProviderService.findEnabledForAuthentication(providerId))
                .thenReturn(Optional.of(decryptedProvider(providerId, iss)));
        when(identityRepository.findLatestByAccountId(accountId))
                .thenReturn(List.of());
        when(identityRepository.findLatestByIssAndAudAndSub(iss, aud, sub))
                .thenReturn(Optional.empty());

        SystemAccountIdentity result = sut.linkIdentity(accountId, providerId, iss, aud, sub);

        assertThat(result.getAccountId()).isEqualTo(accountId);
        assertThat(result.getIss()).isEqualTo(iss);
        assertThat(result.getAud()).isEqualTo(aud);
        assertThat(result.getSub()).isEqualTo(sub);

        ArgumentCaptor<SystemAccountIdentity> identityCaptor =
                ArgumentCaptor.forClass(SystemAccountIdentity.class);
        verify(identityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getCreatedBy())
                .isEqualTo("system:identity-link:" + accountId);

        ArgumentCaptor<SystemAccountIdentityStatus> statusCaptor =
                ArgumentCaptor.forClass(SystemAccountIdentityStatus.class);
        verify(identityStatusRepository).save(statusCaptor.capture());
        assertThat(statusCaptor.getValue().getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(statusCaptor.getValue().getReason()).isNull();
    }

    @Test
    @DisplayName("linkIdentity: providerが見つからない→PROVIDER_NOT_FOUND・DB書込みなし")
    void linkIdentity_providerNotFound() {
        when(systemOidcProviderService.findEnabledForAuthentication("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.linkIdentity(
                "acc-1", "missing", "https://x", "c", "s"))
                .isInstanceOf(IdentityLinkException.class)
                .extracting("error").isEqualTo(IdentityLinkError.PROVIDER_NOT_FOUND);

        verify(identityRepository, never()).save(any());
        verify(identityStatusRepository, never()).save(any());
    }

    @Test
    @DisplayName("linkIdentity: 同一providerで既にACTIVE identityあり→PROVIDER_ALREADY_LINKED")
    void linkIdentity_providerAlreadyLinked() {
        String accountId = "acc-1";
        String iss = "https://accounts.google.com";

        when(systemOidcProviderService.findEnabledForAuthentication("google"))
                .thenReturn(Optional.of(decryptedProvider("google", iss)));

        SystemAccountIdentity existingActive = identity("id-existing", accountId, iss, "c", "s-old");
        when(identityRepository.findLatestByAccountId(accountId))
                .thenReturn(List.of(existingActive));
        when(identityStatusRepository.findLatestByIdentityId("id-existing"))
                .thenReturn(Optional.of(activeStatus("id-existing")));

        assertThatThrownBy(() -> sut.linkIdentity(accountId, "google", iss, "c", "s-new"))
                .isInstanceOf(IdentityLinkException.class)
                .extracting("error").isEqualTo(IdentityLinkError.PROVIDER_ALREADY_LINKED);

        verify(identityRepository, never()).save(any());
        verify(identityStatusRepository, never()).save(any());
    }

    @Test
    @DisplayName("linkIdentity: 同一providerだが既存がINACTIVEなら成功（再連携を許容）")
    void linkIdentity_succeedWhenExistingForSameProviderIsInactive() {
        String accountId = "acc-1";
        String iss = "https://accounts.google.com";

        when(systemOidcProviderService.findEnabledForAuthentication("google"))
                .thenReturn(Optional.of(decryptedProvider("google", iss)));

        SystemAccountIdentity existingInactive = identity("id-existing", accountId, iss, "c", "s-old");
        when(identityRepository.findLatestByAccountId(accountId))
                .thenReturn(List.of(existingInactive));
        when(identityStatusRepository.findLatestByIdentityId("id-existing"))
                .thenReturn(Optional.of(inactiveStatus("id-existing")));
        when(identityRepository.findLatestByIssAndAudAndSub(iss, "c", "s-new"))
                .thenReturn(Optional.empty());

        SystemAccountIdentity result = sut.linkIdentity(accountId, "google", iss, "c", "s-new");

        assertThat(result.getSub()).isEqualTo("s-new");
        verify(identityRepository).save(any());
        verify(identityStatusRepository).save(any());
    }

    @Test
    @DisplayName("linkIdentity: iss/aud/subが他accountで使用中→IDENTITY_ALREADY_LINKED")
    void linkIdentity_identityAlreadyLinkedToOtherAccount() {
        String accountId = "acc-1";
        String iss = "https://accounts.google.com";

        when(systemOidcProviderService.findEnabledForAuthentication("google"))
                .thenReturn(Optional.of(decryptedProvider("google", iss)));
        when(identityRepository.findLatestByAccountId(accountId))
                .thenReturn(List.of()); // 自accountには未登録
        when(identityRepository.findLatestByIssAndAudAndSub(iss, "c", "s-new"))
                .thenReturn(Optional.of(
                        identity("id-other", "acc-other", iss, "c", "s-new"))); // 他accountで使用

        assertThatThrownBy(() -> sut.linkIdentity(accountId, "google", iss, "c", "s-new"))
                .isInstanceOf(IdentityLinkException.class)
                .extracting("error").isEqualTo(IdentityLinkError.IDENTITY_ALREADY_LINKED);

        verify(identityRepository, never()).save(any());
        verify(identityStatusRepository, never()).save(any());
    }

    @Test
    @DisplayName("unlinkIdentity: 正常系: 解除対象のstatusにINACTIVEレコードを追加")
    void unlinkIdentity_success() {
        String accountId = "acc-1";
        String targetId = "id-target";
        String iss = "https://accounts.google.com";

        when(identityRepository.findLatestByIdentityId(targetId))
                .thenReturn(Optional.of(identity(targetId, accountId, iss, "c", "s")));
        // 解除後もACTIVEが残る: target + active2
        when(identityRepository.findLatestByAccountId(accountId))
                .thenReturn(List.of(
                        identity(targetId, accountId, iss, "c", "s"),
                        identity("id-keep", accountId, "https://other", "c2", "s2")));
        when(identityStatusRepository.findLatestByIdentityId(targetId))
                .thenReturn(Optional.of(activeStatus(targetId)));
        when(identityStatusRepository.findLatestByIdentityId("id-keep"))
                .thenReturn(Optional.of(activeStatus("id-keep")));

        sut.unlinkIdentity(accountId, targetId, "id-keep", "manual unlink");

        ArgumentCaptor<SystemAccountIdentityStatus> captor =
                ArgumentCaptor.forClass(SystemAccountIdentityStatus.class);
        verify(identityStatusRepository).save(captor.capture());
        SystemAccountIdentityStatus saved = captor.getValue();
        assertThat(saved.getIdentityId()).isEqualTo(targetId);
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(saved.getReason()).isEqualTo("manual unlink");
        assertThat(saved.getCreatedBy()).isEqualTo("system:identity-unlink:" + accountId);
    }

    @Test
    @DisplayName("unlinkIdentity: identityが存在しない→IDENTITY_NOT_FOUND")
    void unlinkIdentity_identityNotFound() {
        when(identityRepository.findLatestByIdentityId("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.unlinkIdentity("acc-1", "missing", null, null))
                .isInstanceOf(IdentityLinkException.class)
                .extracting("error").isEqualTo(IdentityLinkError.IDENTITY_NOT_FOUND);

        verify(identityStatusRepository, never()).save(any());
    }

    @Test
    @DisplayName("unlinkIdentity: 他accountのidentity→IDENTITY_NOT_OWNED")
    void unlinkIdentity_identityNotOwned() {
        when(identityRepository.findLatestByIdentityId("id-other"))
                .thenReturn(Optional.of(identity("id-other", "acc-other", "iss", "c", "s")));

        assertThatThrownBy(() -> sut.unlinkIdentity("acc-1", "id-other", null, null))
                .isInstanceOf(IdentityLinkException.class)
                .extracting("error").isEqualTo(IdentityLinkError.IDENTITY_NOT_OWNED);

        verify(identityStatusRepository, never()).save(any());
    }

    @Test
    @DisplayName("unlinkIdentity: 現セッションで使用中→CANNOT_UNLINK_CURRENT_SESSION")
    void unlinkIdentity_currentSession() {
        String accountId = "acc-1";
        String targetId = "id-target";
        when(identityRepository.findLatestByIdentityId(targetId))
                .thenReturn(Optional.of(identity(targetId, accountId, "iss", "c", "s")));

        assertThatThrownBy(() -> sut.unlinkIdentity(accountId, targetId, targetId, null))
                .isInstanceOf(IdentityLinkException.class)
                .extracting("error").isEqualTo(IdentityLinkError.CANNOT_UNLINK_CURRENT_SESSION);

        verify(identityStatusRepository, never()).save(any());
    }

    @Test
    @DisplayName("unlinkIdentity: 既にINACTIVE→ALREADY_INACTIVE")
    void unlinkIdentity_alreadyInactive() {
        String accountId = "acc-1";
        String targetId = "id-target";
        when(identityRepository.findLatestByIdentityId(targetId))
                .thenReturn(Optional.of(identity(targetId, accountId, "iss", "c", "s")));
        when(identityStatusRepository.findLatestByIdentityId(targetId))
                .thenReturn(Optional.of(inactiveStatus(targetId)));

        assertThatThrownBy(() -> sut.unlinkIdentity(accountId, targetId, "id-other", null))
                .isInstanceOf(IdentityLinkException.class)
                .extracting("error").isEqualTo(IdentityLinkError.ALREADY_INACTIVE);

        verify(identityStatusRepository, never()).save(any());
    }

    @Test
    @DisplayName("unlinkIdentity: 最後のACTIVE→CANNOT_UNLINK_LAST_ACTIVE")
    void unlinkIdentity_lastActive() {
        String accountId = "acc-1";
        String targetId = "id-only";
        when(identityRepository.findLatestByIdentityId(targetId))
                .thenReturn(Optional.of(identity(targetId, accountId, "iss", "c", "s")));
        when(identityRepository.findLatestByAccountId(accountId))
                .thenReturn(List.of(identity(targetId, accountId, "iss", "c", "s")));
        when(identityStatusRepository.findLatestByIdentityId(targetId))
                .thenReturn(Optional.of(activeStatus(targetId)));

        assertThatThrownBy(() -> sut.unlinkIdentity(accountId, targetId, "id-session", null))
                .isInstanceOf(IdentityLinkException.class)
                .extracting("error").isEqualTo(IdentityLinkError.CANNOT_UNLINK_LAST_ACTIVE);

        verify(identityStatusRepository, never()).save(any());
    }

    @Test
    @DisplayName("listLinkedIdentities: ACTIVE/INACTIVE混在をstatus付きで一覧化、現セッションフラグも反映")
    void listLinkedIdentities_mixedStatusesWithCurrentSessionMarking() {
        String accountId = "acc-1";
        SystemAccountIdentity a = identity("id-a", accountId, "https://prov-a", "c", "s-a");
        SystemAccountIdentity b = identity("id-b", accountId, "https://prov-b", "c", "s-b");

        when(identityRepository.findLatestByAccountId(accountId))
                .thenReturn(List.of(a, b));
        when(identityStatusRepository.findLatestByIdentityId("id-a"))
                .thenReturn(Optional.of(activeStatus("id-a")));
        when(identityStatusRepository.findLatestByIdentityId("id-b"))
                .thenReturn(Optional.of(inactiveStatus("id-b")));

        ProviderWithStatus provA = providerWithStatus("prov-a", "Provider A",
                "https://prov-a", OidcProviderStatusValue.ENABLED);
        ProviderWithStatus provB = providerWithStatus("prov-b", "Provider B",
                "https://prov-b", OidcProviderStatusValue.DISABLED);
        when(systemOidcProviderService.listAll())
                .thenReturn(List.of(provA, provB));

        List<LinkedIdentityView> result = sut.listLinkedIdentities(accountId, "id-a");

        assertThat(result).hasSize(2);
        // ACTIVE優先で並ぶ
        assertThat(result.get(0).getIdentityId()).isEqualTo("id-a");
        assertThat(result.get(0).isActive()).isTrue();
        assertThat(result.get(0).isCurrentSession()).isTrue();
        assertThat(result.get(0).getProviderDisplayName()).isEqualTo("Provider A");
        assertThat(result.get(0).isProviderEnabled()).isTrue();

        assertThat(result.get(1).getIdentityId()).isEqualTo("id-b");
        assertThat(result.get(1).isActive()).isFalse();
        assertThat(result.get(1).isCurrentSession()).isFalse();
        assertThat(result.get(1).getProviderDisplayName()).isEqualTo("Provider B");
        assertThat(result.get(1).isProviderEnabled()).isFalse();
    }

    @Test
    @DisplayName("listLinkedIdentities: identity 0件→空リスト")
    void listLinkedIdentities_empty() {
        when(identityRepository.findLatestByAccountId("acc-1"))
                .thenReturn(List.of());
        when(systemOidcProviderService.listAll()).thenReturn(List.of());

        List<LinkedIdentityView> result = sut.listLinkedIdentities("acc-1", null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("listLinkableProvidersForAccount: ENABLED中、自accountにACTIVE連携のあるproviderは除外")
    void listLinkableProvidersForAccount_excludesAlreadyLinked() {
        String accountId = "acc-1";

        ProviderWithStatus enabledA = providerWithStatus("prov-a", "Provider A",
                "https://prov-a", OidcProviderStatusValue.ENABLED);
        ProviderWithStatus enabledB = providerWithStatus("prov-b", "Provider B",
                "https://prov-b", OidcProviderStatusValue.ENABLED);
        ProviderWithStatus disabledC = providerWithStatus("prov-c", "Provider C",
                "https://prov-c", OidcProviderStatusValue.DISABLED);

        when(systemOidcProviderService.listAll())
                .thenReturn(List.of(enabledA, enabledB, disabledC));

        // accountはhttps://prov-aと既にACTIVE連携済
        SystemAccountIdentity activeOnA =
                identity("id-a", accountId, "https://prov-a", "c", "s");
        when(identityRepository.findLatestByAccountId(accountId))
                .thenReturn(List.of(activeOnA));
        when(identityStatusRepository.findLatestByIdentityId("id-a"))
                .thenReturn(Optional.of(activeStatus("id-a")));

        List<ProviderWithStatus> result = sut.listLinkableProvidersForAccount(accountId);

        // prov-a除外、prov-c除外（DISABLED）、prov-b のみ返る
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProvider().getProviderId()).isEqualTo("prov-b");
    }

    @Test
    @DisplayName("listLinkableProvidersForAccount: 既存連携が全てINACTIVEなら除外しない")
    void listLinkableProvidersForAccount_inactiveLinkDoesNotExclude() {
        String accountId = "acc-1";

        ProviderWithStatus enabledA = providerWithStatus("prov-a", "Provider A",
                "https://prov-a", OidcProviderStatusValue.ENABLED);
        when(systemOidcProviderService.listAll()).thenReturn(List.of(enabledA));

        SystemAccountIdentity inactiveOnA =
                identity("id-a", accountId, "https://prov-a", "c", "s");
        when(identityRepository.findLatestByAccountId(accountId))
                .thenReturn(List.of(inactiveOnA));
        when(identityStatusRepository.findLatestByIdentityId("id-a"))
                .thenReturn(Optional.of(inactiveStatus("id-a")));

        List<ProviderWithStatus> result = sut.listLinkableProvidersForAccount(accountId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProvider().getProviderId()).isEqualTo("prov-a");
    }

    private SystemAccountIdentity identity(
            String identityId, String accountId, String iss, String aud, String sub) {
        OffsetDateTime version = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        return new SystemAccountIdentity(
                identityId, version, accountId, iss, aud, sub, version, "test");
    }

    private SystemAccountIdentityStatus activeStatus(String identityId) {
        OffsetDateTime version = OffsetDateTime.of(2026, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC);
        return new SystemAccountIdentityStatus(
                identityId, version, AccountStatus.ACTIVE, null, version, "test");
    }

    private SystemAccountIdentityStatus inactiveStatus(String identityId) {
        OffsetDateTime version = OffsetDateTime.of(2026, 1, 3, 0, 0, 0, 0, ZoneOffset.UTC);
        return new SystemAccountIdentityStatus(
                identityId, version, AccountStatus.INACTIVE, "test reason", version, "test");
    }

    private DecryptedOidcProvider decryptedProvider(String providerId, String iss) {
        OffsetDateTime version = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        return new DecryptedOidcProvider(
                providerId, version, "Display " + providerId, iss, "client-1", "secret");
    }

    private ProviderWithStatus providerWithStatus(
            String providerId, String displayName, String uri, OidcProviderStatusValue status) {
        OffsetDateTime version = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        SystemOidcProvider provider = new SystemOidcProvider(
                providerId, version, displayName, uri, "client", "enc-secret", version, "test");
        SystemOidcProviderStatus statusObj = new SystemOidcProviderStatus(
                providerId, version, status, null, version, "test");
        return new ProviderWithStatus(provider, statusObj);
    }

}
