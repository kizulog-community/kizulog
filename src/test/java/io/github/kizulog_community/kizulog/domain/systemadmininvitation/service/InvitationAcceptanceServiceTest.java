package io.github.kizulog_community.kizulog.domain.systemadmininvitation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccount;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentity;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentityStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountRole;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountRoleStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemRole;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityStatusRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountRoleRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountRoleStatusRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountStatusRepository;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.exception.InvitationError;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.exception.InvitationException;

/**
 * InvitationAcceptanceServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class InvitationAcceptanceServiceTest {

    private SystemAccountRepository accountRepository;
    private SystemAccountStatusRepository accountStatusRepository;
    private SystemAccountIdentityRepository identityRepository;
    private SystemAccountIdentityStatusRepository identityStatusRepository;
    private SystemAccountRoleRepository roleRepository;
    private SystemAccountRoleStatusRepository roleStatusRepository;
    private SystemAdminInvitationService invitationService;
    private InvitationAcceptanceService service;

    @BeforeEach
    void setUp() {
        accountRepository = mock(SystemAccountRepository.class);
        accountStatusRepository = mock(SystemAccountStatusRepository.class);
        identityRepository = mock(SystemAccountIdentityRepository.class);
        identityStatusRepository = mock(SystemAccountIdentityStatusRepository.class);
        roleRepository = mock(SystemAccountRoleRepository.class);
        roleStatusRepository = mock(SystemAccountRoleStatusRepository.class);
        invitationService = mock(SystemAdminInvitationService.class);

        service = new InvitationAcceptanceService(
                accountRepository,
                accountStatusRepository,
                identityRepository,
                identityStatusRepository,
                roleRepository,
                roleStatusRepository,
                invitationService);
    }

    @Test
    @DisplayName("acceptInvitation: 正常系 - account/status/identity/identityStatus/role/roleStatus + markAsUsedが順次呼ばれる")
    void acceptInvitation_savesAllSixEntitiesAndMarksAsUsed() {
        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);

        SystemAccountIdentity result = service.acceptInvitation(
                "inv-001",
                "https://auth.example/realms/kizulog",
                "client-x",
                "sub-12345");

        OffsetDateTime after = OffsetDateTime.now(ZoneOffset.UTC);

        // 戻り値はidentity(accountIdとidentityIdを含む)
        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isNotBlank();
        assertThat(result.getIdentityId()).isNotBlank();
        assertThat(result.getIss()).isEqualTo("https://auth.example/realms/kizulog");
        assertThat(result.getAud()).isEqualTo("client-x");
        assertThat(result.getSub()).isEqualTo("sub-12345");

        // 6種の保存呼び出しを検証
        ArgumentCaptor<SystemAccount> accountCap = ArgumentCaptor.forClass(SystemAccount.class);
        verify(accountRepository).save(accountCap.capture());
        SystemAccount savedAccount = accountCap.getValue();
        assertThat(savedAccount.getAccountId()).isEqualTo(result.getAccountId());
        assertThat(savedAccount.getCreatedBy()).isEqualTo("system:invite:inv-001");

        ArgumentCaptor<SystemAccountStatus> accountStatusCap =
                ArgumentCaptor.forClass(SystemAccountStatus.class);
        verify(accountStatusRepository).save(accountStatusCap.capture());
        SystemAccountStatus savedAccountStatus = accountStatusCap.getValue();
        assertThat(savedAccountStatus.getAccountId()).isEqualTo(result.getAccountId());
        assertThat(savedAccountStatus.getStatus()).isEqualTo(AccountStatus.ACTIVE);

        ArgumentCaptor<SystemAccountIdentity> identityCap =
                ArgumentCaptor.forClass(SystemAccountIdentity.class);
        verify(identityRepository).save(identityCap.capture());
        SystemAccountIdentity savedIdentity = identityCap.getValue();
        assertThat(savedIdentity.getIdentityId()).isEqualTo(result.getIdentityId());
        assertThat(savedIdentity.getAccountId()).isEqualTo(result.getAccountId());
        assertThat(savedIdentity.getIss()).isEqualTo("https://auth.example/realms/kizulog");

        ArgumentCaptor<SystemAccountIdentityStatus> identityStatusCap =
                ArgumentCaptor.forClass(SystemAccountIdentityStatus.class);
        verify(identityStatusRepository).save(identityStatusCap.capture());
        assertThat(identityStatusCap.getValue().getStatus()).isEqualTo(AccountStatus.ACTIVE);

        ArgumentCaptor<SystemAccountRole> roleCap =
                ArgumentCaptor.forClass(SystemAccountRole.class);
        verify(roleRepository).save(roleCap.capture());
        SystemAccountRole savedRole = roleCap.getValue();
        assertThat(savedRole.getAccountId()).isEqualTo(result.getAccountId());
        assertThat(savedRole.getRole()).isEqualTo(SystemRole.SYSTEM_ADMIN);

        ArgumentCaptor<SystemAccountRoleStatus> roleStatusCap =
                ArgumentCaptor.forClass(SystemAccountRoleStatus.class);
        verify(roleStatusRepository).save(roleStatusCap.capture());
        assertThat(roleStatusCap.getValue().getStatus()).isEqualTo(AccountStatus.ACTIVE);

        verify(invitationService).markAsUsed("inv-001", "system:invite:inv-001");

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
        SystemAccountIdentity result = service.acceptInvitation(
                "inv-002", "iss", "aud", "sub");

        // UUID v4 format: 8-4-4-4-12
        assertThat(result.getAccountId())
                .matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
        assertThat(result.getIdentityId())
                .matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

        ArgumentCaptor<SystemAccountRole> roleCap =
                ArgumentCaptor.forClass(SystemAccountRole.class);
        verify(roleRepository).save(roleCap.capture());
        assertThat(roleCap.getValue().getRoleId())
                .matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }

    @Test
    @DisplayName("acceptInvitation: account_idはaccount/status/identity/role/roleStatusで一致する")
    void acceptInvitation_accountIdConsistentAcrossAllEntities() {
        service.acceptInvitation("inv-003", "iss", "aud", "sub");

        ArgumentCaptor<SystemAccount> accountCap = ArgumentCaptor.forClass(SystemAccount.class);
        ArgumentCaptor<SystemAccountStatus> accountStatusCap =
                ArgumentCaptor.forClass(SystemAccountStatus.class);
        ArgumentCaptor<SystemAccountIdentity> identityCap =
                ArgumentCaptor.forClass(SystemAccountIdentity.class);
        ArgumentCaptor<SystemAccountRole> roleCap =
                ArgumentCaptor.forClass(SystemAccountRole.class);
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
        service.acceptInvitation("inv-004", "iss", "aud", "sub");

        ArgumentCaptor<SystemAccountIdentity> identityCap =
                ArgumentCaptor.forClass(SystemAccountIdentity.class);
        ArgumentCaptor<SystemAccountIdentityStatus> identityStatusCap =
                ArgumentCaptor.forClass(SystemAccountIdentityStatus.class);
        verify(identityRepository).save(identityCap.capture());
        verify(identityStatusRepository).save(identityStatusCap.capture());

        String identityId = identityCap.getValue().getIdentityId();
        assertThat(identityStatusCap.getValue().getIdentityId()).isEqualTo(identityId);
    }

    @Test
    @DisplayName("acceptInvitation: role_idはrole/roleStatusで一致する")
    void acceptInvitation_roleIdConsistentAcrossRoleEntities() {
        service.acceptInvitation("inv-005", "iss", "aud", "sub");

        ArgumentCaptor<SystemAccountRole> roleCap =
                ArgumentCaptor.forClass(SystemAccountRole.class);
        ArgumentCaptor<SystemAccountRoleStatus> roleStatusCap =
                ArgumentCaptor.forClass(SystemAccountRoleStatus.class);
        verify(roleRepository).save(roleCap.capture());
        verify(roleStatusRepository).save(roleStatusCap.capture());

        String roleId = roleCap.getValue().getRoleId();
        assertThat(roleStatusCap.getValue().getRoleId()).isEqualTo(roleId);
    }

    @Test
    @DisplayName("acceptInvitation: 同一トランザクション内で順序通り呼ばれる(account→status→identity→identityStatus→role→roleStatus→markAsUsed)")
    void acceptInvitation_callsRepositoriesInOrder() {
        service.acceptInvitation("inv-006", "iss", "aud", "sub");

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
        inOrder.verify(invitationService).markAsUsed(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    @DisplayName("acceptInvitation: markAsUsedがINVITATION_NOT_FOUNDで失敗した場合、例外がそのまま伝播される")
    void acceptInvitation_propagatesException_whenMarkAsUsedFails() {
        doThrow(new InvitationException(InvitationError.INVITATION_NOT_FOUND))
                .when(invitationService).markAsUsed("inv-007", "system:invite:inv-007");

        assertThatThrownBy(() -> service.acceptInvitation("inv-007", "iss", "aud", "sub"))
                .isInstanceOf(InvitationException.class)
                .extracting(e -> ((InvitationException) e).getError())
                .isEqualTo(InvitationError.INVITATION_NOT_FOUND);

        // 例外伝播前に6種の保存は呼ばれている(トランザクション境界はSpringがロールバックする想定)
        verify(accountRepository).save(Mockito.any());
        verify(accountStatusRepository).save(Mockito.any());
        verify(identityRepository).save(Mockito.any());
        verify(identityStatusRepository).save(Mockito.any());
        verify(roleRepository).save(Mockito.any());
        verify(roleStatusRepository).save(Mockito.any());
    }

    @Test
    @DisplayName("acceptInvitation: 連続呼び出しで毎回異なるaccountId/identityId/roleIdが生成される")
    void acceptInvitation_generatesUniqueIdsAcrossCalls() {
        SystemAccountIdentity first =
                service.acceptInvitation("inv-A", "iss", "aud", "sub-a");
        // Mockのcaptorはどんどん上書きされるのでArgumentCaptorの最後がfirst分の最終呼び出し
        // 2回目を呼ぶ前に値を退避
        String firstAccountId = first.getAccountId();
        String firstIdentityId = first.getIdentityId();

        // 別のmockに切り替えるためsetUp相当を再実行
        accountRepository = mock(SystemAccountRepository.class);
        accountStatusRepository = mock(SystemAccountStatusRepository.class);
        identityRepository = mock(SystemAccountIdentityRepository.class);
        identityStatusRepository = mock(SystemAccountIdentityStatusRepository.class);
        roleRepository = mock(SystemAccountRoleRepository.class);
        roleStatusRepository = mock(SystemAccountRoleStatusRepository.class);
        invitationService = mock(SystemAdminInvitationService.class);
        service = new InvitationAcceptanceService(
                accountRepository, accountStatusRepository,
                identityRepository, identityStatusRepository,
                roleRepository, roleStatusRepository,
                invitationService);

        SystemAccountIdentity second =
                service.acceptInvitation("inv-B", "iss", "aud", "sub-b");

        assertThat(second.getAccountId()).isNotEqualTo(firstAccountId);
        assertThat(second.getIdentityId()).isNotEqualTo(firstIdentityId);
    }

    @Test
    @DisplayName("acceptInvitation: status reasonはnull、createdBy接頭辞「system:invite:」がinvitationIdに付く")
    void acceptInvitation_statusReasonIsNullAndCreatedByHasPrefix() {
        service.acceptInvitation("inv-008", "iss", "aud", "sub");

        ArgumentCaptor<SystemAccountStatus> accountStatusCap =
                ArgumentCaptor.forClass(SystemAccountStatus.class);
        verify(accountStatusRepository).save(accountStatusCap.capture());
        assertThat(accountStatusCap.getValue().getReason()).isNull();
        assertThat(accountStatusCap.getValue().getCreatedBy()).isEqualTo("system:invite:inv-008");

        verify(invitationService).markAsUsed("inv-008", "system:invite:inv-008");
    }

}
