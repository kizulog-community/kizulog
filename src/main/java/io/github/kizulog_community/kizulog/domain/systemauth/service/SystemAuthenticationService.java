package io.github.kizulog_community.kizulog.domain.systemauth.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentity;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentityStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountRole;
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
import lombok.RequiredArgsConstructor;

/**
 * システム管理者認証サービス
 *
 * <p>OIDCプロバイダーから検証済みの iss/aud/sub を受け取り、
 * KizuLogのOIDC → アカウント情報と突合して認証可否を判定するドメインサービス。</p>
 *
 * <p>認証成功の条件:
 * <ol>
 *   <li>iss/aud/subで一意のidentityが見つかる</li>
 *   <li>そのidentityの最新ステータスがACTIVE</li>
 *   <li>OIDCに紐付くアカウントの最新ステータスがACTIVE</li>
 *   <li>SYSTEM_ADMINロールが有効な状態で付与されている</li>
 * </ol>
 * いずれかが満たされない場合は SystemAuthenticationException を投げる。</p>
 *
 * <p>本サービスはSpring Securityには依存せず、純粋なドメインロジックを表現する。
 * Spring Securityとの連携は本サービスの呼び出し元（SystemOidcUserService等）で行う。</p>
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class SystemAuthenticationService {

    /** システム管理アカウントリポジトリ */
    private final SystemAccountRepository systemAccountRepository;

    /** システム管理アカウントステータスリポジトリ */
    private final SystemAccountStatusRepository systemAccountStatusRepository;

    /** システム管理アカウント認証方法リポジトリ */
    private final SystemAccountIdentityRepository systemAccountIdentityRepository;

    /** システム管理アカウント認証方法ステータスリポジトリ */
    private final SystemAccountIdentityStatusRepository systemAccountIdentityStatusRepository;

    /** システム管理アカウントロールリポジトリ */
    private final SystemAccountRoleRepository systemAccountRoleRepository;

    /** システム管理アカウントロールステータスリポジトリ */
    private final SystemAccountRoleStatusRepository systemAccountRoleStatusRepository;

    /**
     * OIDCのiss/aud/subから、システム管理者として認証可能か判定する。
     *
     * <p>認証に使用された認証方法を返す。
     * 呼び出し元はidentity.getAccountId()でアカウントIDを取得できる。</p>
     *
     * @param iss OIDC Issuer
     * @param aud OIDC Audience
     * @param sub OIDC Subject
     * @return 認証成功時のidentity（accountIdを含む）
     * @throws SystemAuthenticationException 認証失敗時
     */
    @Transactional(readOnly = true)
    public SystemAccountIdentity authenticate(String iss, String aud, String sub) {
        SystemAccountIdentity identity = findIdentity(iss, aud, sub);
        verifyIdentityActive(identity.getIdentityId());
        verifyAccountExists(identity.getAccountId());
        verifyAccountActive(identity.getAccountId());
        verifySystemAdminRoleActive(identity.getAccountId());
        return identity;
    }

    /**
     * iss/aud/subでidentityを検索する。見つからなければ例外。
     */
    private SystemAccountIdentity findIdentity(String iss, String aud, String sub) {
        return systemAccountIdentityRepository.findLatestByIssAndAudAndSub(iss, aud, sub)
                .orElseThrow(() -> new SystemAuthenticationException(
                        SystemAuthenticationErrorType.ACCOUNT_NOT_FOUND));
    }

    /**
     * identityの最新ステータスがACTIVEか検証する。
     */
    private void verifyIdentityActive(String identityId) {
        SystemAccountIdentityStatus status = systemAccountIdentityStatusRepository
                .findLatestByIdentityId(identityId)
                .orElseThrow(() -> new SystemAuthenticationException(
                        SystemAuthenticationErrorType.IDENTITY_INACTIVE));

        if (!status.getStatus().isAuthenticatable()) {
            throw new SystemAuthenticationException(
                    SystemAuthenticationErrorType.IDENTITY_INACTIVE);
        }
    }

    /**
     * accountIdでアカウントが存在することを検証する。
     */
    private void verifyAccountExists(String accountId) {
        systemAccountRepository.findLatestByAccountId(accountId)
                .orElseThrow(() -> new SystemAuthenticationException(
                        SystemAuthenticationErrorType.ACCOUNT_NOT_FOUND));
    }

    /**
     * アカウントの最新ステータスが認証可能な状態か検証する。
     */
    private void verifyAccountActive(String accountId) {
        SystemAccountStatus status = systemAccountStatusRepository
                .findLatestByAccountId(accountId)
                .orElseThrow(() -> new SystemAuthenticationException(
                        SystemAuthenticationErrorType.ACCOUNT_INACTIVE));

        if (!status.getStatus().isAuthenticatable()) {
            throw new SystemAuthenticationException(
                    SystemAuthenticationErrorType.ACCOUNT_INACTIVE);
        }
    }

    /**
     * アカウントにACTIVEなSYSTEM_ADMINロールが付与されているか検証する。
     */
    private void verifySystemAdminRoleActive(String accountId) {
        List<SystemAccountRole> roles =
                systemAccountRoleRepository.findLatestByAccountId(accountId);

        boolean hasActiveSystemAdmin = roles.stream()
                .filter(r -> r.getRole() == SystemRole.SYSTEM_ADMIN)
                .anyMatch(r -> isRoleActive(r.getRoleId()));

        if (!hasActiveSystemAdmin) {
            throw new SystemAuthenticationException(
                    SystemAuthenticationErrorType.ROLE_NOT_GRANTED);
        }
    }

    /**
     * 指定されたrole_idのロールがACTIVEかを判定する。
     */
    private boolean isRoleActive(String roleId) {
        return systemAccountRoleStatusRepository.findLatestByRoleId(roleId)
                .map(s -> s.getStatus().isAuthenticatable())
                .orElse(false);
    }

}
