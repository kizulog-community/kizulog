package io.github.kizulog_community.kizulog.domain.tenantadmininvitation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccount;
import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountIdentity;
import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountIdentityStatus;
import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountRole;
import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountRoleStatus;
import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountStatus;
import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountStatusValue;
import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantRole;
import io.github.kizulog_community.kizulog.domain.tenantaccount.port.TenantAccountIdentityRepository;
import io.github.kizulog_community.kizulog.domain.tenantaccount.port.TenantAccountIdentityStatusRepository;
import io.github.kizulog_community.kizulog.domain.tenantaccount.port.TenantAccountRepository;
import io.github.kizulog_community.kizulog.domain.tenantaccount.port.TenantAccountRoleRepository;
import io.github.kizulog_community.kizulog.domain.tenantaccount.port.TenantAccountRoleStatusRepository;
import io.github.kizulog_community.kizulog.domain.tenantaccount.port.TenantAccountStatusRepository;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.exception.TenantInvitationError;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.exception.TenantInvitationException;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantAdminInvitation;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.port.TenantAdminInvitationRepository;

/**
 * TenantAdminAcceptanceService の単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantAdminAcceptanceServiceTest {

    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    private static final String TENANT_A = "tenant-A";
    private static final String TENANT_B = "tenant-B";
    private static final String ISS = "https://auth.example/realms/acme";
    private static final String AUD = "client-x";
    private static final String SUB = "sub-12345";

    private TenantAccountRepository accountRepository;
    private TenantAccountStatusRepository accountStatusRepository;
    private TenantAccountIdentityRepository identityRepository;
    private TenantAccountIdentityStatusRepository identityStatusRepository;
    private TenantAccountRoleRepository roleRepository;
    private TenantAccountRoleStatusRepository roleStatusRepository;
    private TenantAdminInvitationRepository invitationRepository;
    private TenantAdminInvitationService invitationService;
    private TenantAdminAcceptanceService service;

    @BeforeEach
    void setUp() {
        accountRepository = mock(TenantAccountRepository.class);
        accountStatusRepository = mock(TenantAccountStatusRepository.class);
        identityRepository = mock(TenantAccountIdentityRepository.class);
        identityStatusRepository = mock(TenantAccountIdentityStatusRepository.class);
        roleRepository = mock(TenantAccountRoleRepository.class);
        roleStatusRepository = mock(TenantAccountRoleStatusRepository.class);
        invitationRepository = mock(TenantAdminInvitationRepository.class);
        invitationService = mock(TenantAdminInvitationService.class);

        service = new TenantAdminAcceptanceService(
                accountRepository,
                accountStatusRepository,
                identityRepository,
                identityStatusRepository,
                roleRepository,
                roleStatusRepository,
                invitationRepository,
                invitationService);
    }

    /**
     * 招待取得のスタブ。デフォルトで identity 重複なし。
     */
    private void stubInvitation(String invitationId, String tenantId) {
        when(invitationRepository.findLatestByInvitationId(invitationId))
                .thenReturn(Optional.of(new TenantAdminInvitation(
                        invitationId, BASE_TIME, tenantId, "hash",
                        BASE_TIME.plusDays(1), "display", BASE_TIME, "creator")));
        when(identityRepository.findLatestByTenantIdAndIssAndAudAndSub(
                Mockito.eq(tenantId), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("acceptInvitation: 正常系 - account/status/identity/identityStatus/role/roleStatus + markAsUsedが順次呼ばれる")
    void acceptInvitation_savesAllEntitiesAndMarksAsUsed() {
        stubInvitation("inv-001", TENANT_A);
        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);

        TenantAccountIdentity result = service.acceptInvitation(
                "inv-001", TENANT_A, ISS, AUD, SUB);

        OffsetDateTime after = OffsetDateTime.now(ZoneOffset.UTC);

        // 戻り値はidentity(accountIdとidentityIdを含む)
        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isNotBlank();
        assertThat(result.getIdentityId()).isNotBlank();
        assertThat(result.getTenantId()).isEqualTo(TENANT_A);
        assertThat(result.getIss()).isEqualTo(ISS);
        assertThat(result.getAud()).isEqualTo(AUD);
        assertThat(result.getSub()).isEqualTo(SUB);

        // TenantAccount
        ArgumentCaptor<TenantAccount> accountCap = ArgumentCaptor.forClass(TenantAccount.class);
        verify(accountRepository).save(accountCap.capture());
        TenantAccount savedAccount = accountCap.getValue();
        assertThat(savedAccount.getAccountId()).isEqualTo(result.getAccountId());
        assertThat(savedAccount.getTenantId()).isEqualTo(TENANT_A);
        assertThat(savedAccount.getCreatedBy()).isEqualTo("tenant:invite:inv-001");

        // TenantAccountStatus
        ArgumentCaptor<TenantAccountStatus> accountStatusCap =
                ArgumentCaptor.forClass(TenantAccountStatus.class);
        verify(accountStatusRepository).save(accountStatusCap.capture());
        TenantAccountStatus savedAccountStatus = accountStatusCap.getValue();
        assertThat(savedAccountStatus.getAccountId()).isEqualTo(result.getAccountId());
        assertThat(savedAccountStatus.getStatus()).isEqualTo(TenantAccountStatusValue.ACTIVE);

        // TenantAccountIdentity
        ArgumentCaptor<TenantAccountIdentity> identityCap =
                ArgumentCaptor.forClass(TenantAccountIdentity.class);
        verify(identityRepository).save(identityCap.capture());
        TenantAccountIdentity savedIdentity = identityCap.getValue();
        assertThat(savedIdentity.getIdentityId()).isEqualTo(result.getIdentityId());
        assertThat(savedIdentity.getAccountId()).isEqualTo(result.getAccountId());
        assertThat(savedIdentity.getTenantId()).isEqualTo(TENANT_A);
        assertThat(savedIdentity.getIss()).isEqualTo(ISS);

        // TenantAccountIdentityStatus
        ArgumentCaptor<TenantAccountIdentityStatus> identityStatusCap =
                ArgumentCaptor.forClass(TenantAccountIdentityStatus.class);
        verify(identityStatusRepository).save(identityStatusCap.capture());
        assertThat(identityStatusCap.getValue().getStatus())
                .isEqualTo(TenantAccountStatusValue.ACTIVE);

        // TenantAccountRole（TENANT_ADMIN）
        ArgumentCaptor<TenantAccountRole> roleCap =
                ArgumentCaptor.forClass(TenantAccountRole.class);
        verify(roleRepository).save(roleCap.capture());
        TenantAccountRole savedRole = roleCap.getValue();
        assertThat(savedRole.getAccountId()).isEqualTo(result.getAccountId());
        assertThat(savedRole.getRole()).isEqualTo(TenantRole.TENANT_ADMIN);

        // TenantAccountRoleStatus
        ArgumentCaptor<TenantAccountRoleStatus> roleStatusCap =
                ArgumentCaptor.forClass(TenantAccountRoleStatus.class);
        verify(roleStatusRepository).save(roleStatusCap.capture());
        assertThat(roleStatusCap.getValue().getStatus())
                .isEqualTo(TenantAccountStatusValue.ACTIVE);

        // markAsUsed（tenantId付きで呼ばれる）
        verify(invitationService).markAsUsed(TENANT_A, "inv-001", "tenant:invite:inv-001");

        // 全て同一バージョン(同一時刻)
        OffsetDateTime version = savedAccount.getVersion();
        assertThat(version).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
        assertThat(savedAccountStatus.getVersion()).isEqualTo(version);
        assertThat(savedIdentity.getVersion()).isEqualTo(version);
        assertThat(identityStatusCap.getValue().getVersion()).isEqualTo(version);
        assertThat(savedRole.getVersion()).isEqualTo(version);
        assertThat(roleStatusCap.getValue().getVersion()).isEqualTo(version);
    }

    @Test
    @DisplayName("acceptInvitation: account_id/identity_id/role_idはUUID形式で発行される")
    void acceptInvitation_generatesUuidIds() {
        stubInvitation("inv-002", TENANT_A);

        TenantAccountIdentity result = service.acceptInvitation(
                "inv-002", TENANT_A, ISS, AUD, SUB);

        String uuidRegex =
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
        assertThat(result.getAccountId()).matches(uuidRegex);
        assertThat(result.getIdentityId()).matches(uuidRegex);

        ArgumentCaptor<TenantAccountRole> roleCap =
                ArgumentCaptor.forClass(TenantAccountRole.class);
        verify(roleRepository).save(roleCap.capture());
        assertThat(roleCap.getValue().getRoleId()).matches(uuidRegex);
    }

    @Test
    @DisplayName("acceptInvitation: account_idはaccount/status/identity/roleで一致する")
    void acceptInvitation_accountIdConsistentAcrossAllEntities() {
        stubInvitation("inv-003", TENANT_A);

        service.acceptInvitation("inv-003", TENANT_A, ISS, AUD, SUB);

        ArgumentCaptor<TenantAccount> accountCap = ArgumentCaptor.forClass(TenantAccount.class);
        ArgumentCaptor<TenantAccountStatus> accountStatusCap =
                ArgumentCaptor.forClass(TenantAccountStatus.class);
        ArgumentCaptor<TenantAccountIdentity> identityCap =
                ArgumentCaptor.forClass(TenantAccountIdentity.class);
        ArgumentCaptor<TenantAccountRole> roleCap =
                ArgumentCaptor.forClass(TenantAccountRole.class);
        verify(accountRepository).save(accountCap.capture());
        verify(accountStatusRepository).save(accountStatusCap.capture());
        verify(identityRepository).save(identityCap.capture());
        verify(roleRepository).save(roleCap.capture());

        String accountId = accountCap.getValue().getAccountId();
        assertThat(accountStatusCap.getValue().getAccountId()).isEqualTo(accountId);
        assertThat(identityCap.getValue().getAccountId()).isEqualTo(accountId);
        assertThat(roleCap.getValue().getAccountId()).isEqualTo(accountId);
    }

    @Test
    @DisplayName("acceptInvitation: identity_idはidentity/identityStatusで一致する")
    void acceptInvitation_identityIdConsistentAcrossIdentityEntities() {
        stubInvitation("inv-004", TENANT_A);

        service.acceptInvitation("inv-004", TENANT_A, ISS, AUD, SUB);

        ArgumentCaptor<TenantAccountIdentity> identityCap =
                ArgumentCaptor.forClass(TenantAccountIdentity.class);
        ArgumentCaptor<TenantAccountIdentityStatus> identityStatusCap =
                ArgumentCaptor.forClass(TenantAccountIdentityStatus.class);
        verify(identityRepository).save(identityCap.capture());
        verify(identityStatusRepository).save(identityStatusCap.capture());

        String identityId = identityCap.getValue().getIdentityId();
        assertThat(identityStatusCap.getValue().getIdentityId()).isEqualTo(identityId);
    }

    @Test
    @DisplayName("acceptInvitation: role_idはrole/roleStatusで一致する")
    void acceptInvitation_roleIdConsistentAcrossRoleEntities() {
        stubInvitation("inv-005", TENANT_A);

        service.acceptInvitation("inv-005", TENANT_A, ISS, AUD, SUB);

        ArgumentCaptor<TenantAccountRole> roleCap =
                ArgumentCaptor.forClass(TenantAccountRole.class);
        ArgumentCaptor<TenantAccountRoleStatus> roleStatusCap =
                ArgumentCaptor.forClass(TenantAccountRoleStatus.class);
        verify(roleRepository).save(roleCap.capture());
        verify(roleStatusRepository).save(roleStatusCap.capture());

        String roleId = roleCap.getValue().getRoleId();
        assertThat(roleStatusCap.getValue().getRoleId()).isEqualTo(roleId);
    }

    @Test
    @DisplayName("acceptInvitation: 同一トランザクション内で順序通り呼ばれる(account→status→identity→identityStatus→role→roleStatus→markAsUsed)")
    void acceptInvitation_callsRepositoriesInOrder() {
        stubInvitation("inv-006", TENANT_A);

        service.acceptInvitation("inv-006", TENANT_A, ISS, AUD, SUB);

        var inOrder = Mockito.inOrder(
                accountRepository, accountStatusRepository,
                identityRepository, identityStatusRepository,
                roleRepository, roleStatusRepository,
                invitationService);
        inOrder.verify(accountRepository).save(Mockito.any());
        inOrder.verify(accountStatusRepository).save(Mockito.any());
        inOrder.verify(identityRepository).save(Mockito.any());
        inOrder.verify(identityStatusRepository).save(Mockito.any());
        inOrder.verify(roleRepository).save(Mockito.any());
        inOrder.verify(roleStatusRepository).save(Mockito.any());
        inOrder.verify(invitationService)
                .markAsUsed(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    @DisplayName("acceptInvitation: 招待が存在しない場合、INVITATION_NOT_FOUNDエラー / アカウント生成は行わない")
    void acceptInvitation_throwsNotFound_whenInvitationMissing() {
        when(invitationRepository.findLatestByInvitationId("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.acceptInvitation("missing", TENANT_A, ISS, AUD, SUB))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.INVITATION_NOT_FOUND);

        verify(accountRepository, never()).save(Mockito.any());
        verify(invitationService, never())
                .markAsUsed(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    @DisplayName("acceptInvitation: 招待のテナントとホスト解決テナントが不一致の場合、TENANT_MISMATCHエラー / アカウント生成は行わない")
    void acceptInvitation_throwsTenantMismatch_whenTenantDiffers() {
        // 招待は tenant-B のものだが、tenant-A のホストで受諾しようとする
        when(invitationRepository.findLatestByInvitationId("inv-007"))
                .thenReturn(Optional.of(new TenantAdminInvitation(
                        "inv-007", BASE_TIME, TENANT_B, "hash",
                        BASE_TIME.plusDays(1), "display", BASE_TIME, "creator")));

        assertThatThrownBy(() ->
                service.acceptInvitation("inv-007", TENANT_A, ISS, AUD, SUB))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.TENANT_MISMATCH);

        verify(accountRepository, never()).save(Mockito.any());
        verify(identityRepository, never()).save(Mockito.any());
        verify(invitationService, never())
                .markAsUsed(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    @DisplayName("acceptInvitation: 同一テナント内に同じiss/aud/subのidentityが既存の場合、IDENTITY_EXISTSエラー / アカウント生成は行わない")
    void acceptInvitation_throwsIdentityExists_whenIdentityAlreadyExists() {
        when(invitationRepository.findLatestByInvitationId("inv-008"))
                .thenReturn(Optional.of(new TenantAdminInvitation(
                        "inv-008", BASE_TIME, TENANT_A, "hash",
                        BASE_TIME.plusDays(1), "display", BASE_TIME, "creator")));
        // 既存 identity が見つかる
        when(identityRepository.findLatestByTenantIdAndIssAndAudAndSub(
                TENANT_A, ISS, AUD, SUB))
                .thenReturn(Optional.of(new TenantAccountIdentity(
                        "existing-id", BASE_TIME, "existing-account", TENANT_A,
                        ISS, AUD, SUB, BASE_TIME, "creator")));

        assertThatThrownBy(() ->
                service.acceptInvitation("inv-008", TENANT_A, ISS, AUD, SUB))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.IDENTITY_EXISTS);

        verify(accountRepository, never()).save(Mockito.any());
        verify(invitationService, never())
                .markAsUsed(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    @DisplayName("acceptInvitation: markAsUsedがINVITATION_NOT_FOUNDで失敗した場合、例外がそのまま伝播される")
    void acceptInvitation_propagatesException_whenMarkAsUsedFails() {
        stubInvitation("inv-009", TENANT_A);
        doThrow(new TenantInvitationException(TenantInvitationError.INVITATION_NOT_FOUND))
                .when(invitationService)
                .markAsUsed(TENANT_A, "inv-009", "tenant:invite:inv-009");

        assertThatThrownBy(() ->
                service.acceptInvitation("inv-009", TENANT_A, ISS, AUD, SUB))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.INVITATION_NOT_FOUND);

        // 例外伝播前に6種の保存は呼ばれている(トランザクション境界はSpringがロールバックする想定)
        verify(accountRepository).save(Mockito.any());
        verify(accountStatusRepository).save(Mockito.any());
        verify(identityRepository).save(Mockito.any());
        verify(identityStatusRepository).save(Mockito.any());
        verify(roleRepository).save(Mockito.any());
        verify(roleStatusRepository).save(Mockito.any());
    }

    @Test
    @DisplayName("acceptInvitation: status reasonはnull、createdBy接頭辞「tenant:invite:」がinvitationIdに付く")
    void acceptInvitation_statusReasonIsNullAndCreatedByHasPrefix() {
        stubInvitation("inv-010", TENANT_A);

        service.acceptInvitation("inv-010", TENANT_A, ISS, AUD, SUB);

        ArgumentCaptor<TenantAccountStatus> accountStatusCap =
                ArgumentCaptor.forClass(TenantAccountStatus.class);
        verify(accountStatusRepository).save(accountStatusCap.capture());
        assertThat(accountStatusCap.getValue().getReason()).isNull();
        assertThat(accountStatusCap.getValue().getCreatedBy())
                .isEqualTo("tenant:invite:inv-010");

        verify(invitationService).markAsUsed(TENANT_A, "inv-010", "tenant:invite:inv-010");
    }

    @Test
    @DisplayName("acceptInvitation: 連続呼び出しで毎回異なるaccountId/identityId/roleIdが生成される")
    void acceptInvitation_generatesUniqueIdsAcrossCalls() {
        stubInvitation("inv-A", TENANT_A);
        TenantAccountIdentity first =
                service.acceptInvitation("inv-A", TENANT_A, ISS, AUD, "sub-a");
        String firstAccountId = first.getAccountId();
        String firstIdentityId = first.getIdentityId();

        // mockを作り直し
        setUp();
        stubInvitation("inv-B", TENANT_A);
        TenantAccountIdentity second =
                service.acceptInvitation("inv-B", TENANT_A, ISS, AUD, "sub-b");

        assertThat(second.getAccountId()).isNotEqualTo(firstAccountId);
        assertThat(second.getIdentityId()).isNotEqualTo(firstIdentityId);
    }

}
