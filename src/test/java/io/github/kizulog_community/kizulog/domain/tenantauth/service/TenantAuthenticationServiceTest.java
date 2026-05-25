package io.github.kizulog_community.kizulog.domain.tenantauth.service;

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
import io.github.kizulog_community.kizulog.domain.tenantauth.exception.TenantAuthenticationErrorType;
import io.github.kizulog_community.kizulog.domain.tenantauth.exception.TenantAuthenticationException;
import io.github.kizulog_community.kizulog.domain.tenantauth.model.TenantAuthenticationResult;

/**
 * TenantAuthenticationServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantAuthenticationServiceTest {

    private static final String TENANT_ID = "tenant-A";
    private static final String ISS = "https://auth.example/realms/acme";
    private static final String AUD = "kizulog-acme";
    private static final String SUB = "user-uuid-123";
    private static final String ACCOUNT_ID = "acc-1";
    private static final String IDENTITY_ID = "identity-1";
    private static final String ROLE_ID = "role-1";
    private static final String ROLE_ID_2 = "role-2";
    private static final OffsetDateTime VERSION =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private TenantAccountRepository accountRepo;
    private TenantAccountStatusRepository accountStatusRepo;
    private TenantAccountIdentityRepository identityRepo;
    private TenantAccountIdentityStatusRepository identityStatusRepo;
    private TenantAccountRoleRepository roleRepo;
    private TenantAccountRoleStatusRepository roleStatusRepo;
    private TenantAuthenticationService service;

    @BeforeEach
    void setUp() {
        accountRepo = mock(TenantAccountRepository.class);
        accountStatusRepo = mock(TenantAccountStatusRepository.class);
        identityRepo = mock(TenantAccountIdentityRepository.class);
        identityStatusRepo = mock(TenantAccountIdentityStatusRepository.class);
        roleRepo = mock(TenantAccountRoleRepository.class);
        roleStatusRepo = mock(TenantAccountRoleStatusRepository.class);
        service = new TenantAuthenticationService(
                accountRepo, accountStatusRepo,
                identityRepo, identityStatusRepo,
                roleRepo, roleStatusRepo);
    }

    private TenantAccount account() {
        return new TenantAccount(ACCOUNT_ID, VERSION, TENANT_ID, VERSION, "tenant:invite:x");
    }

    private TenantAccountStatus accountStatus(TenantAccountStatusValue statusValue) {
        return new TenantAccountStatus(
                ACCOUNT_ID, VERSION, statusValue, null, VERSION, "tenant:invite:x");
    }

    private TenantAccountIdentity identity() {
        return new TenantAccountIdentity(
                IDENTITY_ID, VERSION, ACCOUNT_ID, TENANT_ID, ISS, AUD, SUB,
                VERSION, "tenant:invite:x");
    }

    private TenantAccountIdentityStatus identityStatus(TenantAccountStatusValue statusValue) {
        return new TenantAccountIdentityStatus(
                IDENTITY_ID, VERSION, statusValue, null, VERSION, "tenant:invite:x");
    }

    private TenantAccountRole role(String roleId, TenantRole roleValue) {
        return new TenantAccountRole(
                roleId, VERSION, ACCOUNT_ID, roleValue, VERSION, "tenant:invite:x");
    }

    private TenantAccountRoleStatus roleStatus(
            String roleId, TenantAccountStatusValue statusValue) {
        return new TenantAccountRoleStatus(
                roleId, VERSION, statusValue, null, VERSION, "tenant:invite:x");
    }

    /**
     * 認証成功シナリオ（TENANT_ADMIN単独）の全モックをセットアップする。
     */
    private void setupAllActiveTenantAdmin() {
        when(identityRepo.findLatestByTenantIdAndIssAndAudAndSub(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(TenantAccountStatusValue.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(account()));
        when(accountStatusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(accountStatus(TenantAccountStatusValue.ACTIVE)));
        when(roleRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(List.of(role(ROLE_ID, TenantRole.TENANT_ADMIN)));
        when(roleStatusRepo.findLatestByRoleId(ROLE_ID))
                .thenReturn(Optional.of(roleStatus(ROLE_ID, TenantAccountStatusValue.ACTIVE)));
    }

    @Test
    @DisplayName("authenticate: 全条件を満たす場合(TENANT_ADMIN)、identityと有効ロール集合が返る")
    void authenticate_succeedsWithTenantAdmin() {
        setupAllActiveTenantAdmin();

        TenantAuthenticationResult result = service.authenticate(TENANT_ID, ISS, AUD, SUB);

        assertThat(result.getIdentity().getIdentityId()).isEqualTo(IDENTITY_ID);
        assertThat(result.getIdentity().getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.getIdentity().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result.getActiveRoles()).containsExactly(TenantRole.TENANT_ADMIN);
    }

    @Test
    @DisplayName("authenticate: EMPLOYEE単独でも認証成功し、EMPLOYEEロールが返る")
    void authenticate_succeedsWithEmployeeOnly() {
        when(identityRepo.findLatestByTenantIdAndIssAndAudAndSub(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(TenantAccountStatusValue.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(account()));
        when(accountStatusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(accountStatus(TenantAccountStatusValue.ACTIVE)));
        when(roleRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(List.of(role(ROLE_ID, TenantRole.EMPLOYEE)));
        when(roleStatusRepo.findLatestByRoleId(ROLE_ID))
                .thenReturn(Optional.of(roleStatus(ROLE_ID, TenantAccountStatusValue.ACTIVE)));

        TenantAuthenticationResult result = service.authenticate(TENANT_ID, ISS, AUD, SUB);

        assertThat(result.getActiveRoles()).containsExactly(TenantRole.EMPLOYEE);
    }

    @Test
    @DisplayName("authenticate: TENANT_ADMINとEMPLOYEE両方が有効な場合、両ロールが返る")
    void authenticate_returnsBothRoles_whenBothActive() {
        when(identityRepo.findLatestByTenantIdAndIssAndAudAndSub(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(TenantAccountStatusValue.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(account()));
        when(accountStatusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(accountStatus(TenantAccountStatusValue.ACTIVE)));
        when(roleRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(List.of(
                        role(ROLE_ID, TenantRole.TENANT_ADMIN),
                        role(ROLE_ID_2, TenantRole.EMPLOYEE)));
        when(roleStatusRepo.findLatestByRoleId(ROLE_ID))
                .thenReturn(Optional.of(roleStatus(ROLE_ID, TenantAccountStatusValue.ACTIVE)));
        when(roleStatusRepo.findLatestByRoleId(ROLE_ID_2))
                .thenReturn(Optional.of(roleStatus(ROLE_ID_2, TenantAccountStatusValue.ACTIVE)));

        TenantAuthenticationResult result = service.authenticate(TENANT_ID, ISS, AUD, SUB);

        assertThat(result.getActiveRoles())
                .containsExactlyInAnyOrder(TenantRole.TENANT_ADMIN, TenantRole.EMPLOYEE);
    }

    @Test
    @DisplayName("authenticate: 一方のロールがINACTIVEなら、有効な方のみ返る")
    void authenticate_returnsOnlyActiveRole_whenOneInactive() {
        when(identityRepo.findLatestByTenantIdAndIssAndAudAndSub(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(TenantAccountStatusValue.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(account()));
        when(accountStatusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(accountStatus(TenantAccountStatusValue.ACTIVE)));
        when(roleRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(List.of(
                        role(ROLE_ID, TenantRole.TENANT_ADMIN),
                        role(ROLE_ID_2, TenantRole.EMPLOYEE)));
        // TENANT_ADMIN は INACTIVE、EMPLOYEE は ACTIVE
        when(roleStatusRepo.findLatestByRoleId(ROLE_ID))
                .thenReturn(Optional.of(roleStatus(ROLE_ID, TenantAccountStatusValue.INACTIVE)));
        when(roleStatusRepo.findLatestByRoleId(ROLE_ID_2))
                .thenReturn(Optional.of(roleStatus(ROLE_ID_2, TenantAccountStatusValue.ACTIVE)));

        TenantAuthenticationResult result = service.authenticate(TENANT_ID, ISS, AUD, SUB);

        assertThat(result.getActiveRoles()).containsExactly(TenantRole.EMPLOYEE);
    }

    @Test
    @DisplayName("authenticate: 同一ロール種が複数role_idで存在し片方ACTIVEなら、そのロール種は有効")
    void authenticate_treatsRoleActive_whenAnyRoleIdActive() {
        when(identityRepo.findLatestByTenantIdAndIssAndAudAndSub(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(TenantAccountStatusValue.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(account()));
        when(accountStatusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(accountStatus(TenantAccountStatusValue.ACTIVE)));
        // 同じ TENANT_ADMIN が role-1(INACTIVE) と role-2(ACTIVE) で存在（取消→再付与）
        when(roleRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(List.of(
                        role(ROLE_ID, TenantRole.TENANT_ADMIN),
                        role(ROLE_ID_2, TenantRole.TENANT_ADMIN)));
        when(roleStatusRepo.findLatestByRoleId(ROLE_ID))
                .thenReturn(Optional.of(roleStatus(ROLE_ID, TenantAccountStatusValue.INACTIVE)));
        when(roleStatusRepo.findLatestByRoleId(ROLE_ID_2))
                .thenReturn(Optional.of(roleStatus(ROLE_ID_2, TenantAccountStatusValue.ACTIVE)));

        TenantAuthenticationResult result = service.authenticate(TENANT_ID, ISS, AUD, SUB);

        assertThat(result.getActiveRoles()).containsExactly(TenantRole.TENANT_ADMIN);
    }

    @Test
    @DisplayName("authenticate: identityが見つからない場合、ACCOUNT_NOT_FOUNDで例外")
    void authenticate_throwsAccountNotFound_whenNoIdentity() {
        when(identityRepo.findLatestByTenantIdAndIssAndAudAndSub(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(TENANT_ID, ISS, AUD, SUB))
                .isInstanceOf(TenantAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(TenantAuthenticationErrorType.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("authenticate: identity statusが存在しない場合、IDENTITY_INACTIVEで例外")
    void authenticate_throwsIdentityInactive_whenNoIdentityStatus() {
        when(identityRepo.findLatestByTenantIdAndIssAndAudAndSub(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(TENANT_ID, ISS, AUD, SUB))
                .isInstanceOf(TenantAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(TenantAuthenticationErrorType.IDENTITY_INACTIVE);
    }

    @Test
    @DisplayName("authenticate: identity statusがINACTIVEの場合、IDENTITY_INACTIVEで例外")
    void authenticate_throwsIdentityInactive_whenIdentityStatusIsInactive() {
        when(identityRepo.findLatestByTenantIdAndIssAndAudAndSub(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(TenantAccountStatusValue.INACTIVE)));

        assertThatThrownBy(() -> service.authenticate(TENANT_ID, ISS, AUD, SUB))
                .isInstanceOf(TenantAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(TenantAuthenticationErrorType.IDENTITY_INACTIVE);
    }

    @Test
    @DisplayName("authenticate: identity statusがSUSPENDEDの場合、IDENTITY_INACTIVEで例外")
    void authenticate_throwsIdentityInactive_whenIdentityStatusIsSuspended() {
        when(identityRepo.findLatestByTenantIdAndIssAndAudAndSub(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(TenantAccountStatusValue.SUSPENDED)));

        assertThatThrownBy(() -> service.authenticate(TENANT_ID, ISS, AUD, SUB))
                .isInstanceOf(TenantAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(TenantAuthenticationErrorType.IDENTITY_INACTIVE);
    }

    @Test
    @DisplayName("authenticate: アカウント本体が見つからない場合、ACCOUNT_NOT_FOUNDで例外")
    void authenticate_throwsAccountNotFound_whenNoAccount() {
        when(identityRepo.findLatestByTenantIdAndIssAndAudAndSub(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(TenantAccountStatusValue.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(TENANT_ID, ISS, AUD, SUB))
                .isInstanceOf(TenantAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(TenantAuthenticationErrorType.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("authenticate: アカウント statusが存在しない場合、ACCOUNT_INACTIVEで例外")
    void authenticate_throwsAccountInactive_whenNoAccountStatus() {
        when(identityRepo.findLatestByTenantIdAndIssAndAudAndSub(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(TenantAccountStatusValue.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(account()));
        when(accountStatusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(TENANT_ID, ISS, AUD, SUB))
                .isInstanceOf(TenantAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(TenantAuthenticationErrorType.ACCOUNT_INACTIVE);
    }

    @Test
    @DisplayName("authenticate: アカウント statusがINACTIVEの場合、ACCOUNT_INACTIVEで例外")
    void authenticate_throwsAccountInactive_whenAccountStatusIsInactive() {
        when(identityRepo.findLatestByTenantIdAndIssAndAudAndSub(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(TenantAccountStatusValue.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(account()));
        when(accountStatusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(accountStatus(TenantAccountStatusValue.INACTIVE)));

        assertThatThrownBy(() -> service.authenticate(TENANT_ID, ISS, AUD, SUB))
                .isInstanceOf(TenantAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(TenantAuthenticationErrorType.ACCOUNT_INACTIVE);
    }

    @Test
    @DisplayName("authenticate: アカウント statusがSUSPENDEDの場合、ACCOUNT_INACTIVEで例外")
    void authenticate_throwsAccountInactive_whenAccountStatusIsSuspended() {
        when(identityRepo.findLatestByTenantIdAndIssAndAudAndSub(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(TenantAccountStatusValue.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(account()));
        when(accountStatusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(accountStatus(TenantAccountStatusValue.SUSPENDED)));

        assertThatThrownBy(() -> service.authenticate(TENANT_ID, ISS, AUD, SUB))
                .isInstanceOf(TenantAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(TenantAuthenticationErrorType.ACCOUNT_INACTIVE);
    }

    @Test
    @DisplayName("authenticate: ロールが空の場合、ROLE_NOT_GRANTEDで例外")
    void authenticate_throwsRoleNotGranted_whenNoRoles() {
        when(identityRepo.findLatestByTenantIdAndIssAndAudAndSub(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(TenantAccountStatusValue.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(account()));
        when(accountStatusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(accountStatus(TenantAccountStatusValue.ACTIVE)));
        when(roleRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.authenticate(TENANT_ID, ISS, AUD, SUB))
                .isInstanceOf(TenantAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(TenantAuthenticationErrorType.ROLE_NOT_GRANTED);
    }

    @Test
    @DisplayName("authenticate: 全ロールがINACTIVEの場合、ROLE_NOT_GRANTEDで例外")
    void authenticate_throwsRoleNotGranted_whenAllRolesInactive() {
        when(identityRepo.findLatestByTenantIdAndIssAndAudAndSub(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(TenantAccountStatusValue.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(account()));
        when(accountStatusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(accountStatus(TenantAccountStatusValue.ACTIVE)));
        when(roleRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(List.of(
                        role(ROLE_ID, TenantRole.TENANT_ADMIN),
                        role(ROLE_ID_2, TenantRole.EMPLOYEE)));
        when(roleStatusRepo.findLatestByRoleId(ROLE_ID))
                .thenReturn(Optional.of(roleStatus(ROLE_ID, TenantAccountStatusValue.INACTIVE)));
        when(roleStatusRepo.findLatestByRoleId(ROLE_ID_2))
                .thenReturn(Optional.of(roleStatus(ROLE_ID_2, TenantAccountStatusValue.INACTIVE)));

        assertThatThrownBy(() -> service.authenticate(TENANT_ID, ISS, AUD, SUB))
                .isInstanceOf(TenantAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(TenantAuthenticationErrorType.ROLE_NOT_GRANTED);
    }

    @Test
    @DisplayName("authenticate: ロールはあるがrole_statusレコードがない場合、ROLE_NOT_GRANTEDで例外")
    void authenticate_throwsRoleNotGranted_whenRoleStatusMissing() {
        when(identityRepo.findLatestByTenantIdAndIssAndAudAndSub(TENANT_ID, ISS, AUD, SUB))
                .thenReturn(Optional.of(identity()));
        when(identityStatusRepo.findLatestByIdentityId(IDENTITY_ID))
                .thenReturn(Optional.of(identityStatus(TenantAccountStatusValue.ACTIVE)));
        when(accountRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(account()));
        when(accountStatusRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(accountStatus(TenantAccountStatusValue.ACTIVE)));
        when(roleRepo.findLatestByAccountId(ACCOUNT_ID))
                .thenReturn(List.of(role(ROLE_ID, TenantRole.TENANT_ADMIN)));
        when(roleStatusRepo.findLatestByRoleId(ROLE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(TENANT_ID, ISS, AUD, SUB))
                .isInstanceOf(TenantAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(TenantAuthenticationErrorType.ROLE_NOT_GRANTED);
    }

    @Test
    @DisplayName("authenticate: identity検索はtenantId境界つきで呼ばれる")
    void authenticate_callsIdentitySearchWithTenantId() {
        setupAllActiveTenantAdmin();

        service.authenticate(TENANT_ID, ISS, AUD, SUB);

        verify(identityRepo).findLatestByTenantIdAndIssAndAudAndSub(TENANT_ID, ISS, AUD, SUB);
        verify(identityStatusRepo).findLatestByIdentityId(IDENTITY_ID);
        verify(accountRepo).findLatestByAccountId(ACCOUNT_ID);
        verify(accountStatusRepo).findLatestByAccountId(ACCOUNT_ID);
        verify(roleRepo).findLatestByAccountId(ACCOUNT_ID);
        verify(roleStatusRepo).findLatestByRoleId(ROLE_ID);
    }

    @Test
    @DisplayName("authenticate: 別テナントIDでは別のidentity検索キーが使われる")
    void authenticate_usesGivenTenantIdAsSearchKey() {
        String otherTenant = "tenant-B";
        when(identityRepo.findLatestByTenantIdAndIssAndAudAndSub(otherTenant, ISS, AUD, SUB))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(otherTenant, ISS, AUD, SUB))
                .isInstanceOf(TenantAuthenticationException.class)
                .extracting("errorType")
                .isEqualTo(TenantAuthenticationErrorType.ACCOUNT_NOT_FOUND);

        verify(identityRepo)
                .findLatestByTenantIdAndIssAndAudAndSub(otherTenant, ISS, AUD, SUB);
    }

}
