package io.github.kizulog_community.kizulog.domain.systemauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
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
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountRole;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemRole;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountRoleRepository;
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
    private static final OffsetDateTime VERSION =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private SystemAccountRepository accountRepo;
    private SystemAccountStatusRepository statusRepo;
    private SystemAccountRoleRepository roleRepo;
    private SystemAuthenticationService service;

    @BeforeEach
    void setUp() {
        accountRepo = mock(SystemAccountRepository.class);
        statusRepo = mock(SystemAccountStatusRepository.class);
        roleRepo = mock(SystemAccountRoleRepository.class);
        service = new SystemAuthenticationService(accountRepo, statusRepo, roleRepo);
    }

    private SystemAccount account() {
        return new SystemAccount(ACCOUNT_ID, VERSION, ISS, AUD, SUB, VERSION, "system:setup");
    }

    private SystemAccountStatus status(AccountStatus statusValue) {
        return new SystemAccountStatus(
                ACCOUNT_ID, VERSION, statusValue, null, VERSION, "system:setup");
    }

    private SystemAccountRole role(SystemRole roleValue) {
        return new SystemAccountRole(
                ACCOUNT_ID, roleValue, VERSION, VERSION, "system:setup");
    }

    @Test
    @DisplayName("authenticate: 全条件を満たす場合、SystemAccountが返る")
    void authenticate_succeedsWithValidAccount() {
        when(accountRepo.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.of(account()));
        when(statusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(status(AccountStatus.ACTIVE)));
        when(roleRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(List.of(role(SystemRole.SYSTEM_ADMIN)));

        SystemAccount result = service.authenticate(ISS, AUD, SUB);

        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.getIss()).isEqualTo(ISS);
        assertThat(result.getAud()).isEqualTo(AUD);
        assertThat(result.getSub()).isEqualTo(SUB);
    }

    @Test
    @DisplayName("authenticate: アカウントが見つからない場合、ACCOUNT_NOT_FOUNDで例外")
    void authenticate_throwsAccountNotFound_whenNoAccount() {
        when(accountRepo.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(ISS, AUD, SUB))
                .isInstanceOf(SystemAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(SystemAuthenticationErrorType.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("authenticate: ステータスレコードが存在しない場合、ACCOUNT_INACTIVEで例外")
    void authenticate_throwsAccountInactive_whenNoStatus() {
        when(accountRepo.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.of(account()));
        when(statusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(ISS, AUD, SUB))
                .isInstanceOf(SystemAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(SystemAuthenticationErrorType.ACCOUNT_INACTIVE);
    }

    @Test
    @DisplayName("authenticate: ステータスがINACTIVEの場合、ACCOUNT_INACTIVEで例外")
    void authenticate_throwsAccountInactive_whenStatusIsInactive() {
        when(accountRepo.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.of(account()));
        when(statusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(status(AccountStatus.INACTIVE)));

        assertThatThrownBy(() -> service.authenticate(ISS, AUD, SUB))
                .isInstanceOf(SystemAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(SystemAuthenticationErrorType.ACCOUNT_INACTIVE);
    }

    @Test
    @DisplayName("authenticate: ステータスがSUSPENDEDの場合、ACCOUNT_INACTIVEで例外")
    void authenticate_throwsAccountInactive_whenStatusIsSuspended() {
        when(accountRepo.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.of(account()));
        when(statusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(status(AccountStatus.SUSPENDED)));

        assertThatThrownBy(() -> service.authenticate(ISS, AUD, SUB))
                .isInstanceOf(SystemAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(SystemAuthenticationErrorType.ACCOUNT_INACTIVE);
    }

    @Test
    @DisplayName("authenticate: ロールが空の場合、ROLE_NOT_GRANTEDで例外")
    void authenticate_throwsRoleNotGranted_whenNoRoles() {
        when(accountRepo.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.of(account()));
        when(statusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(status(AccountStatus.ACTIVE)));
        when(roleRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.authenticate(ISS, AUD, SUB))
                .isInstanceOf(SystemAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(SystemAuthenticationErrorType.ROLE_NOT_GRANTED);
    }

    @Test
    @DisplayName("authenticate: 各Repositoryが正しい引数で呼ばれる")
    void authenticate_callsRepositoriesWithCorrectArguments() {
        when(accountRepo.findLatestByIssAndAudAndSub(ISS, AUD, SUB))
                .thenReturn(Optional.of(account()));
        when(statusRepo.findLatestByAccountId(anyString()))
                .thenReturn(Optional.of(status(AccountStatus.ACTIVE)));
        when(roleRepo.findLatestByAccountId(anyString()))
                .thenReturn(List.of(role(SystemRole.SYSTEM_ADMIN)));

        service.authenticate(ISS, AUD, SUB);

        verify(accountRepo).findLatestByIssAndAudAndSub(ISS, AUD, SUB);
        verify(statusRepo).findLatestByAccountId(ACCOUNT_ID);
        verify(roleRepo).findLatestByAccountId(ACCOUNT_ID);
    }

}
