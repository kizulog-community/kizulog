package io.github.kizulog_community.kizulog.domain.tenantauth.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountIdentity;
import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountIdentityStatus;
import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountRole;
import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountStatus;
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
import lombok.RequiredArgsConstructor;

/**
 * テナント利用者認証サービス
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class TenantAuthenticationService {

    /** テナントアカウントリポジトリ */
    private final TenantAccountRepository tenantAccountRepository;

    /** テナントアカウントステータスリポジトリ */
    private final TenantAccountStatusRepository tenantAccountStatusRepository;

    /** テナントアカウント認証方法リポジトリ */
    private final TenantAccountIdentityRepository tenantAccountIdentityRepository;

    /** テナントアカウント認証方法ステータスリポジトリ */
    private final TenantAccountIdentityStatusRepository tenantAccountIdentityStatusRepository;

    /** テナントアカウントロールリポジトリ */
    private final TenantAccountRoleRepository tenantAccountRoleRepository;

    /** テナントアカウントロールステータスリポジトリ */
    private final TenantAccountRoleStatusRepository tenantAccountRoleStatusRepository;

    /**
     * OIDCのiss/aud/subとtenantIdから、テナント利用者として認証可能か判定する。
     *
     * @param tenantId ホストから解決済みのテナントID
     * @param iss OIDC Issuer
     * @param aud OIDC Audience
     * @param sub OIDC Subject
     * @return 認証成功時の結果（identity + 有効ロール集合）
     * @throws TenantAuthenticationException 認証失敗時
     */
    @Transactional(readOnly = true)
    public TenantAuthenticationResult authenticate(
            String tenantId, String iss, String aud, String sub) {
        TenantAccountIdentity identity = findIdentity(tenantId, iss, aud, sub);
        verifyIdentityActive(identity.getIdentityId());
        verifyAccountExists(identity.getAccountId());
        verifyAccountActive(identity.getAccountId());
        Set<TenantRole> activeRoles = collectActiveRoles(identity.getAccountId());
        if (activeRoles.isEmpty()) {
            throw new TenantAuthenticationException(
                    TenantAuthenticationErrorType.ROLE_NOT_GRANTED);
        }
        return new TenantAuthenticationResult(identity, activeRoles);
    }

    /**
     * (tenantId, iss, aud, sub) でidentityを検索する。見つからなければ例外。
     */
    private TenantAccountIdentity findIdentity(
            String tenantId, String iss, String aud, String sub) {
        return tenantAccountIdentityRepository
                .findLatestByTenantIdAndIssAndAudAndSub(tenantId, iss, aud, sub)
                .orElseThrow(() -> new TenantAuthenticationException(
                        TenantAuthenticationErrorType.ACCOUNT_NOT_FOUND));
    }

    /**
     * identityの最新ステータスがACTIVEか検証する。
     */
    private void verifyIdentityActive(String identityId) {
        TenantAccountIdentityStatus status = tenantAccountIdentityStatusRepository
                .findLatestByIdentityId(identityId)
                .orElseThrow(() -> new TenantAuthenticationException(
                        TenantAuthenticationErrorType.IDENTITY_INACTIVE));

        if (!status.getStatus().isAuthenticatable()) {
            throw new TenantAuthenticationException(
                    TenantAuthenticationErrorType.IDENTITY_INACTIVE);
        }
    }

    /**
     * accountIdでアカウントが存在することを検証する。
     */
    private void verifyAccountExists(String accountId) {
        tenantAccountRepository.findLatestByAccountId(accountId)
                .orElseThrow(() -> new TenantAuthenticationException(
                        TenantAuthenticationErrorType.ACCOUNT_NOT_FOUND));
    }

    /**
     * アカウントの最新ステータスが認証可能な状態か検証する。
     */
    private void verifyAccountActive(String accountId) {
        TenantAccountStatus status = tenantAccountStatusRepository
                .findLatestByAccountId(accountId)
                .orElseThrow(() -> new TenantAuthenticationException(
                        TenantAuthenticationErrorType.ACCOUNT_INACTIVE));

        if (!status.getStatus().isAuthenticatable()) {
            throw new TenantAuthenticationException(
                    TenantAuthenticationErrorType.ACCOUNT_INACTIVE);
        }
    }

    /**
     * アカウントに付与されている有効な（ACTIVEな）ロールの集合を収集する。
     *
     * @param accountId アカウントID
     * @return 有効ロールの集合（該当なしの場合は空集合）
     */
    private Set<TenantRole> collectActiveRoles(String accountId) {
        List<TenantAccountRole> roles =
                tenantAccountRoleRepository.findLatestByAccountId(accountId);

        Set<TenantRole> activeRoles = EnumSet.noneOf(TenantRole.class);
        for (TenantAccountRole role : roles) {
            if (isRoleActive(role.getRoleId())) {
                activeRoles.add(role.getRole());
            }
        }
        return activeRoles;
    }

    /**
     * 指定されたrole_idのロールがACTIVEかを判定する。
     */
    private boolean isRoleActive(String roleId) {
        return tenantAccountRoleStatusRepository.findLatestByRoleId(roleId)
                .map(s -> s.getStatus().isAuthenticatable())
                .orElse(false);
    }

}
