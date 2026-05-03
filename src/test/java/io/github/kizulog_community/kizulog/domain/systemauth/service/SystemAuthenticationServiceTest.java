package io.github.kizulog_community.kizulog.domain.systemauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
import io.github.kizulog_community.kizulog.domain.systemauth.exception.SystemAuthenticationErrorType;
import io.github.kizulog_community.kizulog.domain.systemauth.exception.SystemAuthenticationException;

/**
 * SystemAuthenticationServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemAuthenticationServiceTest {

    private static final String ISS = "https://auth.example/realms/master";
    private static final String AUD = "kizulog-master";
    private static final String SUB = "user-uuid-123";
    private static final String ACCOUNT_ID = "acc-1";
    private static final String IDENTITY_ID = "identity-1";
    private static final String ROLE_ID = "role-1";
    private static final OffsetDateTime VERSION =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private SystemAccountRepository accountRepo;
    private SystemAccountStatusRepository accountStatusRepo;
    private SystemAccountIdentityRepository identityRepo;
    private SystemAccountIdentityStatusRepository identityStatusRepo;
    private SystemAccountRoleRepository roleRepo;
    private SystemAccountRoleStatusRepository roleStatusRepo;
    private SystemAuthenticationService service;

    @BeforeEach
    void setUp() {
        accountRepo = mock(SystemAccountRepository.class);
        accountStatusRepo = mock(SystemAccountStatusRepository.class);
        identityRepo = mock(SystemAccountIdentityRepository.class);
        identityStatusRepo = mock(SystemAccountIdentityStatusRepository.class);
        roleRepo = mock(SystemAccountRoleRepository.class);
        roleStatusRepo = mock(SystemAccountRoleStatusRepository.class);
        service = new SystemAuthenticationService(
                accountRepo, accountStatusRepo,
                identityRepo, identityStatusRepo,
                roleRepo, roleStatusRepo);
    }

    private SystemAccount account() {
        return new SystemAccount(ACCOUNT_ID, VERSION, VERSION, "system:setup");
    }

    private SystemAccountStatus accountStatus(AccountStatus statusValue) {
        return new SystemAccountStatus(
                ACCOUNT_ID, VERSION, statusValue, null, VERSION, "system:setup");
    }

    private SystemAccountIdentity identity() {
        return new SystemAccountIdentity(
                IDENTITY_ID, VERSION, ACCOUNT_ID, ISS, AUD, SUB, VERSION, "system:setup");
    }

    private SystemAccountIdentityStatus identityStatus(AccountStatus statusValue) {
        return new SystemAccountIdentityStatus(
                IDENTITY_ID, VERSION, statusValue, null, VERSION, "system:setup");
    }

    private SystemAccountRole role(SystemRole roleValue) {
        return new SystemAccountRole(
                ROLE_ID, VERSION, ACCOUNT_ID, roleValue, VERSION, "system:setup");
    }

    private SystemAccountRoleStatus roleStatus(AccountStatus statusValue) {
        return new SystemAccountRoleStatus(
                ROLE_ID, VERSION, statusValue, null, VERSION, "system:setup");
    }

    /**
     * 認証成功シナリオの全モックをセットアップする。
     */
    private void setupAllActive() {
        when(identityRepo.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(AccountStatus.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(account()));
        when(accountStatusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(accountStatus(AccountStatus.ACTIVE)));
        when(roleRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(List.of(role(SystemRole.SYSTEM_ADMIN)));
        when(roleStatusRepo.findLatestByRoleId(ROLE_ID))
                .thenReturn(Optional.of(roleStatus(AccountStatus.ACTIVE)));
    }

    @Test
    @DisplayName("authenticate: 全条件を満たす場合、SystemAccountIdentityが返る")
    void authenticate_succeedsWithValidIdentity() {
        setupAllActive();

        SystemAccountIdentity result = service.authenticate(ISS, AUD, SUB);

        assertThat(result.getIdentityId()).isEqualTo(IDENTITY_ID);
        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.getIss()).isEqualTo(ISS);
        assertThat(result.getAud()).isEqualTo(AUD);
        assertThat(result.getSub()).isEqualTo(SUB);
    }

    @Test
    @DisplayName("authenticate: identityが見つからない場合、ACCOUNT_NOT_FOUNDで例外")
    void authenticate_throwsAccountNotFound_whenNoIdentity() {
        when(identityRepo.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(ISS, AUD, SUB))
                .isInstanceOf(SystemAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(SystemAuthenticationErrorType.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("authenticate: identity statusが存在しない場合、IDENTITY_INACTIVEで例外")
    void authenticate_throwsIdentityInactive_whenNoIdentityStatus() {
        when(identityRepo.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(ISS, AUD, SUB))
                .isInstanceOf(SystemAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(SystemAuthenticationErrorType.IDENTITY_INACTIVE);
    }

    @Test
    @DisplayName("authenticate: identity statusがINACTIVEの場合、IDENTITY_INACTIVEで例外")
    void authenticate_throwsIdentityInactive_whenIdentityStatusIsInactive() {
        when(identityRepo.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(AccountStatus.INACTIVE)));

        assertThatThrownBy(() -> service.authenticate(ISS, AUD, SUB))
                .isInstanceOf(SystemAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(SystemAuthenticationErrorType.IDENTITY_INACTIVE);
    }

    @Test
    @DisplayName("authenticate: アカウント本体が見つからない場合、ACCOUNT_NOT_FOUNDで例外")
    void authenticate_throwsAccountNotFound_whenNoAccount() {
        when(identityRepo.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(AccountStatus.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(ISS, AUD, SUB))
                .isInstanceOf(SystemAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(SystemAuthenticationErrorType.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("authenticate: アカウント statusが存在しない場合、ACCOUNT_INACTIVEで例外")
    void authenticate_throwsAccountInactive_whenNoAccountStatus() {
        when(identityRepo.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(AccountStatus.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(account()));
        when(accountStatusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(ISS, AUD, SUB))
                .isInstanceOf(SystemAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(SystemAuthenticationErrorType.ACCOUNT_INACTIVE);
    }

    @Test
    @DisplayName("authenticate: アカウント statusがINACTIVEの場合、ACCOUNT_INACTIVEで例外")
    void authenticate_throwsAccountInactive_whenAccountStatusIsInactive() {
        when(identityRepo.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(AccountStatus.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(account()));
        when(accountStatusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(accountStatus(AccountStatus.INACTIVE)));

        assertThatThrownBy(() -> service.authenticate(ISS, AUD, SUB))
                .isInstanceOf(SystemAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(SystemAuthenticationErrorType.ACCOUNT_INACTIVE);
    }

    @Test
    @DisplayName("authenticate: アカウント statusがSUSPENDEDの場合、ACCOUNT_INACTIVEで例外")
    void authenticate_throwsAccountInactive_whenAccountStatusIsSuspended() {
        when(identityRepo.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(AccountStatus.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(account()));
        when(accountStatusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(accountStatus(AccountStatus.SUSPENDED)));

        assertThatThrownBy(() -> service.authenticate(ISS, AUD, SUB))
                .isInstanceOf(SystemAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(SystemAuthenticationErrorType.ACCOUNT_INACTIVE);
    }

    @Test
    @DisplayName("authenticate: ロールが空の場合、ROLE_NOT_GRANTEDで例外")
    void authenticate_throwsRoleNotGranted_whenNoRoles() {
        when(identityRepo.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(AccountStatus.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(account()));
        when(accountStatusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(accountStatus(AccountStatus.ACTIVE)));
        when(roleRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.authenticate(ISS, AUD, SUB))
                .isInstanceOf(SystemAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(SystemAuthenticationErrorType.ROLE_NOT_GRANTED);
    }

    @Test
    @DisplayName("authenticate: ロールはあるがrole_statusがINACTIVEの場合、ROLE_NOT_GRANTEDで例外")
    void authenticate_throwsRoleNotGranted_whenRoleStatusInactive() {
        when(identityRepo.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(AccountStatus.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(account()));
        when(accountStatusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(accountStatus(AccountStatus.ACTIVE)));
        when(roleRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(List.of(role(SystemRole.SYSTEM_ADMIN)));
        when(roleStatusRepo.findLatestByRoleId(ROLE_ID))
                .thenReturn(Optional.of(roleStatus(AccountStatus.INACTIVE)));

        assertThatThrownBy(() -> service.authenticate(ISS, AUD, SUB))
                .isInstanceOf(SystemAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(SystemAuthenticationErrorType.ROLE_NOT_GRANTED);
    }

    @Test
    @DisplayName("authenticate: ロールはあるがrole_statusレコードがない場合、ROLE_NOT_GRANTEDで例外")
    void authenticate_throwsRoleNotGranted_whenRoleStatusMissing() {
        when(identityRepo.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(AccountStatus.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(account()));
        when(accountStatusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(accountStatus(AccountStatus.ACTIVE)));
        when(roleRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(List.of(role(SystemRole.SYSTEM_ADMIN)));
        when(roleStatusRepo.findLatestByRoleId(ROLE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(ISS, AUD, SUB))
                .isInstanceOf(SystemAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(SystemAuthenticationErrorType.ROLE_NOT_GRANTED);
    }

    @Test
    @DisplayName("authenticate: 各Repositoryが正しい引数で呼ばれる")
    void authenticate_callsRepositoriesWithCorrectArguments() {
        setupAllActive();

        service.authenticate(ISS, AUD, SUB);

        verify(identityRepo).findLatestByIssAndAudAndSub(ISS, AUD, SUB);
        verify(identityStatusRepo).findLatestByIdentityId(IDENTITY_ID);
        verify(accountRepo).findLatestByAccountId(ACCOUNT_ID);
        verify(accountStatusRepo).findLatestByAccountId(ACCOUNT_ID);
        verify(roleRepo).findLatestByAccountId(ACCOUNT_ID);
        verify(roleStatusRepo).findLatestByRoleId(ROLE_ID);
    }

}
