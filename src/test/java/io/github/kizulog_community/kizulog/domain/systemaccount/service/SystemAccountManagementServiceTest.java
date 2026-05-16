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

import io.github.kizulog_community.kizulog.domain.systemaccount.exception.AccountStatusChangeError;
import io.github.kizulog_community.kizulog.domain.systemaccount.exception.AccountStatusChangeException;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountDetailView;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountListItemView;
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

/**
 * SystemAccountManagementServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemAccountManagementServiceTest {

    private static final OffsetDateTime T0 = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private SystemAccountRepository accountRepository;
    private SystemAccountStatusRepository accountStatusRepository;
    private SystemAccountIdentityRepository identityRepository;
    private SystemAccountIdentityStatusRepository identityStatusRepository;
    private SystemAccountRoleRepository roleRepository;
    private SystemAccountRoleStatusRepository roleStatusRepository;
    private SystemAccountManagementService sut;

    @BeforeEach
    void setUp() {
        accountRepository = mock(SystemAccountRepository.class);
        accountStatusRepository = mock(SystemAccountStatusRepository.class);
        identityRepository = mock(SystemAccountIdentityRepository.class);
        identityStatusRepository = mock(SystemAccountIdentityStatusRepository.class);
        roleRepository = mock(SystemAccountRoleRepository.class);
        roleStatusRepository = mock(SystemAccountRoleStatusRepository.class);

        sut = new SystemAccountManagementService(
                accountRepository,
                accountStatusRepository,
                identityRepository,
                identityStatusRepository,
                roleRepository,
                roleStatusRepository);
    }

    @Test
    @DisplayName("listAllAccounts: 全アカウントを並び順 ACTIVE→SUSPENDED→INACTIVE で返す")
    void listAllAccounts_sortsByStatusOrder() {
        SystemAccount acc1 = new SystemAccount("acc-1", T0, T0, "u:1");
        SystemAccount acc2 = new SystemAccount("acc-2", T0.plusHours(1), T0.plusHours(1), "u:2");
        SystemAccount acc3 = new SystemAccount("acc-3", T0.plusHours(2), T0.plusHours(2), "u:3");
        when(accountRepository.findAllLatest()).thenReturn(List.of(acc1, acc2, acc3));

        when(accountStatusRepository.findLatestByAccountId("acc-1"))
                .thenReturn(Optional.of(status("acc-1", AccountStatus.INACTIVE)));
        when(accountStatusRepository.findLatestByAccountId("acc-2"))
                .thenReturn(Optional.of(status("acc-2", AccountStatus.ACTIVE)));
        when(accountStatusRepository.findLatestByAccountId("acc-3"))
                .thenReturn(Optional.of(status("acc-3", AccountStatus.SUSPENDED)));

        // identity / role はすべて空
        when(identityRepository.findLatestByAccountId(any())).thenReturn(List.of());
        when(roleRepository.findLatestByAccountId(any())).thenReturn(List.of());

        List<AccountListItemView> result = sut.listAllAccounts("acc-99");

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getAccountId()).isEqualTo("acc-2"); // ACTIVE
        assertThat(result.get(1).getAccountId()).isEqualTo("acc-3"); // SUSPENDED
        assertThat(result.get(2).getAccountId()).isEqualTo("acc-1"); // INACTIVE
    }

    @Test
    @DisplayName("listAllAccounts: operatorと同じaccountIdはself=trueでマーク")
    void listAllAccounts_marksSelf() {
        SystemAccount acc = new SystemAccount("acc-self", T0, T0, "u:1");
        when(accountRepository.findAllLatest()).thenReturn(List.of(acc));
        when(accountStatusRepository.findLatestByAccountId("acc-self"))
                .thenReturn(Optional.of(status("acc-self", AccountStatus.ACTIVE)));
        when(identityRepository.findLatestByAccountId("acc-self")).thenReturn(List.of());
        when(roleRepository.findLatestByAccountId("acc-self")).thenReturn(List.of());

        List<AccountListItemView> result = sut.listAllAccounts("acc-self");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isSelf()).isTrue();
    }

    @Test
    @DisplayName("listAllAccounts: activeIdentityCountとsystemAdminRoleActiveを集計する")
    void listAllAccounts_aggregatesIdentityAndRole() {
        SystemAccount acc = new SystemAccount("acc-1", T0, T0, "u:1");
        when(accountRepository.findAllLatest()).thenReturn(List.of(acc));
        when(accountStatusRepository.findLatestByAccountId("acc-1"))
                .thenReturn(Optional.of(status("acc-1", AccountStatus.ACTIVE)));

        SystemAccountIdentity id1 = identity("id-1", "acc-1");
        SystemAccountIdentity id2 = identity("id-2", "acc-1");
        when(identityRepository.findLatestByAccountId("acc-1"))
                .thenReturn(List.of(id1, id2));
        when(identityStatusRepository.findLatestByIdentityId("id-1"))
                .thenReturn(Optional.of(identityStatus("id-1", AccountStatus.ACTIVE)));
        when(identityStatusRepository.findLatestByIdentityId("id-2"))
                .thenReturn(Optional.of(identityStatus("id-2", AccountStatus.INACTIVE)));

        SystemAccountRole role = role("role-1", "acc-1");
        when(roleRepository.findLatestByAccountId("acc-1")).thenReturn(List.of(role));
        when(roleStatusRepository.findLatestByRoleId("role-1"))
                .thenReturn(Optional.of(roleStatus("role-1", AccountStatus.ACTIVE)));

        List<AccountListItemView> result = sut.listAllAccounts("acc-99");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActiveIdentityCount()).isEqualTo(1);
        assertThat(result.get(0).isSystemAdminRoleActive()).isTrue();
    }

    @Test
    @DisplayName("listAllAccounts: account_status未登録なら INACTIVE 扱い")
    void listAllAccounts_unknownStatusBecomesInactive() {
        SystemAccount acc = new SystemAccount("acc-x", T0, T0, "u:1");
        when(accountRepository.findAllLatest()).thenReturn(List.of(acc));
        when(accountStatusRepository.findLatestByAccountId("acc-x"))
                .thenReturn(Optional.empty());
        when(identityRepository.findLatestByAccountId("acc-x")).thenReturn(List.of());
        when(roleRepository.findLatestByAccountId("acc-x")).thenReturn(List.of());

        List<AccountListItemView> result = sut.listAllAccounts(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrentStatus()).isEqualTo(AccountStatus.INACTIVE);
    }

    @Test
    @DisplayName("findAccountDetail: 存在しないaccountIdは空Optional")
    void findAccountDetail_returnsEmpty_whenAccountMissing() {
        when(accountRepository.findLatestByAccountId("missing"))
                .thenReturn(Optional.empty());

        Optional<AccountDetailView> result = sut.findAccountDetail("missing", "op");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAccountDetail: 履歴・identity数・self判定を全て返す")
    void findAccountDetail_returnsFullView() {
        SystemAccount acc = new SystemAccount("acc-1", T0, T0, "creator");
        when(accountRepository.findLatestByAccountId("acc-1"))
                .thenReturn(Optional.of(acc));
        when(accountStatusRepository.findLatestByAccountId("acc-1"))
                .thenReturn(Optional.of(new SystemAccountStatus(
                        "acc-1", T0.plusHours(2), AccountStatus.SUSPENDED,
                        "セキュリティ違反", T0.plusHours(2), "u:op")));
        when(accountStatusRepository.findAllByAccountIdOrderByVersionDesc("acc-1"))
                .thenReturn(List.of(
                        new SystemAccountStatus("acc-1", T0.plusHours(2),
                                AccountStatus.SUSPENDED, "セキュリティ違反",
                                T0.plusHours(2), "u:op"),
                        new SystemAccountStatus("acc-1", T0,
                                AccountStatus.ACTIVE, null, T0, "u:init")));

        SystemAccountIdentity id = identity("id-1", "acc-1");
        when(identityRepository.findLatestByAccountId("acc-1"))
                .thenReturn(List.of(id));
        when(identityStatusRepository.findLatestByIdentityId("id-1"))
                .thenReturn(Optional.of(identityStatus("id-1", AccountStatus.ACTIVE)));

        SystemAccountRole role = role("role-1", "acc-1");
        when(roleRepository.findLatestByAccountId("acc-1")).thenReturn(List.of(role));
        when(roleStatusRepository.findLatestByRoleId("role-1"))
                .thenReturn(Optional.of(roleStatus("role-1", AccountStatus.ACTIVE)));

        Optional<AccountDetailView> resultOpt = sut.findAccountDetail("acc-1", "acc-1");

        assertThat(resultOpt).isPresent();
        AccountDetailView v = resultOpt.get();
        assertThat(v.getAccountId()).isEqualTo("acc-1");
        assertThat(v.getCurrentStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(v.getCurrentStatusReason()).isEqualTo("セキュリティ違反");
        assertThat(v.isSystemAdminRoleActive()).isTrue();
        assertThat(v.getActiveIdentityCount()).isEqualTo(1);
        assertThat(v.getTotalIdentityCount()).isEqualTo(1);
        assertThat(v.getStatusHistory()).hasSize(2);
        assertThat(v.getStatusHistory().get(0).getStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(v.getStatusHistory().get(1).getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(v.isSelf()).isTrue();
    }

    @Test
    @DisplayName("findAccountDetail: account_status未登録なら currentStatus=INACTIVE")
    void findAccountDetail_unknownStatusBecomesInactive() {
        SystemAccount acc = new SystemAccount("acc-1", T0, T0, "creator");
        when(accountRepository.findLatestByAccountId("acc-1"))
                .thenReturn(Optional.of(acc));
        when(accountStatusRepository.findLatestByAccountId("acc-1"))
                .thenReturn(Optional.empty());
        when(accountStatusRepository.findAllByAccountIdOrderByVersionDesc("acc-1"))
                .thenReturn(List.of());
        when(identityRepository.findLatestByAccountId("acc-1")).thenReturn(List.of());
        when(roleRepository.findLatestByAccountId("acc-1")).thenReturn(List.of());

        Optional<AccountDetailView> result = sut.findAccountDetail("acc-1", "op");

        assertThat(result).isPresent();
        assertThat(result.get().getCurrentStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(result.get().getStatusHistory()).isEmpty();
    }

    @Test
    @DisplayName("changeAccountStatus: 正常系: 新規versionのstatusが保存される")
    void changeAccountStatus_success() {
        givenAccountExists("acc-1");
        givenCurrentStatus("acc-1", AccountStatus.SUSPENDED);
        // ACTIVE管理者が他に2人いる
        givenActiveSystemAdminCount(2);

        sut.changeAccountStatus("acc-1", AccountStatus.ACTIVE, "復帰", "acc-op");

        ArgumentCaptor<SystemAccountStatus> captor =
                ArgumentCaptor.forClass(SystemAccountStatus.class);
        verify(accountStatusRepository).save(captor.capture());
        SystemAccountStatus saved = captor.getValue();
        assertThat(saved.getAccountId()).isEqualTo("acc-1");
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(saved.getReason()).isEqualTo("復帰");
        assertThat(saved.getCreatedBy()).isEqualTo("system:account-status-change:acc-op");
    }

    @Test
    @DisplayName("changeAccountStatus: 対象アカウント不在→ACCOUNT_NOT_FOUND・書込みなし")
    void changeAccountStatus_accountNotFound() {
        when(accountRepository.findLatestByAccountId("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.changeAccountStatus(
                "missing", AccountStatus.INACTIVE, "reason", "acc-op"))
                .isInstanceOf(AccountStatusChangeException.class)
                .extracting("error")
                .isEqualTo(AccountStatusChangeError.ACCOUNT_NOT_FOUND);
        verify(accountStatusRepository, never()).save(any());
    }

    @Test
    @DisplayName("changeAccountStatus: 自分自身→CANNOT_CHANGE_SELF・書込みなし")
    void changeAccountStatus_cannotChangeSelf() {
        givenAccountExists("acc-self");

        assertThatThrownBy(() -> sut.changeAccountStatus(
                "acc-self", AccountStatus.INACTIVE, "reason", "acc-self"))
                .isInstanceOf(AccountStatusChangeException.class)
                .extracting("error")
                .isEqualTo(AccountStatusChangeError.CANNOT_CHANGE_SELF);
        verify(accountStatusRepository, never()).save(any());
    }

    @Test
    @DisplayName("changeAccountStatus: 既に同一ステータス→ALREADY_IN_TARGET_STATUS・書込みなし")
    void changeAccountStatus_alreadyInTargetStatus() {
        givenAccountExists("acc-1");
        givenCurrentStatus("acc-1", AccountStatus.ACTIVE);

        assertThatThrownBy(() -> sut.changeAccountStatus(
                "acc-1", AccountStatus.ACTIVE, "reason", "acc-op"))
                .isInstanceOf(AccountStatusChangeException.class)
                .extracting("error")
                .isEqualTo(AccountStatusChangeError.ALREADY_IN_TARGET_STATUS);
        verify(accountStatusRepository, never()).save(any());
    }

    @Test
    @DisplayName("changeAccountStatus: 最後のACTIVE管理者をINACTIVEにしようとして→CANNOT_DEACTIVATE_LAST_ACTIVE_ADMIN")
    void changeAccountStatus_lastActiveAdmin_blocksInactive() {
        givenAccountExists("acc-1");
        givenCurrentStatus("acc-1", AccountStatus.ACTIVE);
        givenActiveSystemAdminCount(1); // 対象1名のみ

        assertThatThrownBy(() -> sut.changeAccountStatus(
                "acc-1", AccountStatus.INACTIVE, "reason", "acc-op"))
                .isInstanceOf(AccountStatusChangeException.class)
                .extracting("error")
                .isEqualTo(AccountStatusChangeError.CANNOT_DEACTIVATE_LAST_ACTIVE_ADMIN);
        verify(accountStatusRepository, never()).save(any());
    }

    @Test
    @DisplayName("changeAccountStatus: 最後のACTIVE管理者をSUSPENDEDにしようとして→CANNOT_DEACTIVATE_LAST_ACTIVE_ADMIN")
    void changeAccountStatus_lastActiveAdmin_blocksSuspended() {
        givenAccountExists("acc-1");
        givenCurrentStatus("acc-1", AccountStatus.ACTIVE);
        givenActiveSystemAdminCount(1);

        assertThatThrownBy(() -> sut.changeAccountStatus(
                "acc-1", AccountStatus.SUSPENDED, "reason", "acc-op"))
                .isInstanceOf(AccountStatusChangeException.class)
                .extracting("error")
                .isEqualTo(AccountStatusChangeError.CANNOT_DEACTIVATE_LAST_ACTIVE_ADMIN);
        verify(accountStatusRepository, never()).save(any());
    }

    @Test
    @DisplayName("changeAccountStatus: 最後のACTIVE管理者でもACTIVE→ACTIVEは同一なのでLAST_ACTIVEより先にALREADY_IN_TARGETで弾く")
    void changeAccountStatus_lastActive_butSameStatus_returnsAlreadyInTarget() {
        givenAccountExists("acc-1");
        givenCurrentStatus("acc-1", AccountStatus.ACTIVE);
        givenActiveSystemAdminCount(1);

        assertThatThrownBy(() -> sut.changeAccountStatus(
                "acc-1", AccountStatus.ACTIVE, "reason", "acc-op"))
                .isInstanceOf(AccountStatusChangeException.class)
                .extracting("error")
                .isEqualTo(AccountStatusChangeError.ALREADY_IN_TARGET_STATUS);
    }

    @Test
    @DisplayName("changeAccountStatus: INACTIVE→ACTIVE はLAST_ACTIVE保護対象外（カウント増える方向）")
    void changeAccountStatus_inactiveToActive_allowedRegardlessOfAdminCount() {
        givenAccountExists("acc-1");
        givenCurrentStatus("acc-1", AccountStatus.INACTIVE);
        givenActiveSystemAdminCount(0); // ACTIVE管理者が0でもOK

        sut.changeAccountStatus("acc-1", AccountStatus.ACTIVE, "復活", "acc-op");

        verify(accountStatusRepository).save(any(SystemAccountStatus.class));
    }

    @Test
    @DisplayName("changeAccountStatus: SUSPENDED→INACTIVE はLAST_ACTIVE保護対象外（既に非ACTIVE）")
    void changeAccountStatus_suspendedToInactive_allowedRegardlessOfAdminCount() {
        givenAccountExists("acc-1");
        givenCurrentStatus("acc-1", AccountStatus.SUSPENDED);
        givenActiveSystemAdminCount(0);

        sut.changeAccountStatus("acc-1", AccountStatus.INACTIVE, "確定退職", "acc-op");

        verify(accountStatusRepository).save(any(SystemAccountStatus.class));
    }

    private void givenAccountExists(String accountId) {
        when(accountRepository.findLatestByAccountId(accountId))
                .thenReturn(Optional.of(new SystemAccount(accountId, T0, T0, "creator")));
    }

    private void givenCurrentStatus(String accountId, AccountStatus status) {
        when(accountStatusRepository.findLatestByAccountId(accountId))
                .thenReturn(Optional.of(new SystemAccountStatus(
                        accountId, T0, status, null, T0, "u:init")));
    }

    /**
     * 「ACTIVE な system_accounts かつ SYSTEM_ADMIN ロール ACTIVE」の件数を擬似的に作る。
     *
     * <p>countActiveSystemAdmins() が見るのは findAllLatest()→status→role→roleStatus の連鎖。
     * シンプルに count 件の「ACTIVEアカウント + ACTIVE SYSTEM_ADMINロール」を仕込む。</p>
     */
    private void givenActiveSystemAdminCount(int count) {
        List<SystemAccount> accounts = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = "active-admin-" + i;
            accounts.add(new SystemAccount(id, T0, T0, "u:i"));
            when(accountStatusRepository.findLatestByAccountId(id))
                    .thenReturn(Optional.of(status(id, AccountStatus.ACTIVE)));
            String roleId = "active-role-" + i;
            when(roleRepository.findLatestByAccountId(id))
                    .thenReturn(List.of(new SystemAccountRole(
                            roleId, T0, id, SystemRole.SYSTEM_ADMIN, T0, "u:r")));
            when(roleStatusRepository.findLatestByRoleId(roleId))
                    .thenReturn(Optional.of(roleStatus(roleId, AccountStatus.ACTIVE)));
        }
        when(accountRepository.findAllLatest()).thenReturn(accounts);
    }

    private static SystemAccountStatus status(String accountId, AccountStatus status) {
        return new SystemAccountStatus(accountId, T0, status, null, T0, "u:init");
    }

    private static SystemAccountIdentity identity(String identityId, String accountId) {
        return new SystemAccountIdentity(
                identityId, T0, accountId,
                "https://iss/", "aud", "sub-" + identityId, T0, "u:init");
    }

    private static SystemAccountIdentityStatus identityStatus(
            String identityId, AccountStatus status) {
        return new SystemAccountIdentityStatus(
                identityId, T0, status, null, T0, "u:init");
    }

    private static SystemAccountRole role(String roleId, String accountId) {
        return new SystemAccountRole(
                roleId, T0, accountId, SystemRole.SYSTEM_ADMIN, T0, "u:init");
    }

    private static SystemAccountRoleStatus roleStatus(String roleId, AccountStatus status) {
        return new SystemAccountRoleStatus(
                roleId, T0, status, null, T0, "u:init");
    }

}
