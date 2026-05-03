package io.github.kizulog_community.kizulog.domain.systemauth.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccount;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountRole;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemRole;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountRoleRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountStatusRepository;
import io.github.kizulog_community.kizulog.domain.systemauth.exception.SystemAuthenticationErrorType;
import io.github.kizulog_community.kizulog.domain.systemauth.exception.SystemAuthenticationException;
import lombok.RequiredArgsConstructor;

/**
 * システム管理者認証サービス
 *
 * <p>OIDCプロバイダーから検証済みの iss/aud/sub を受け取り、
 * KizuLogのアカウント情報（system_accounts）と突合して認証可否を判定するドメインサービス。</p>
 *
 * <p>認証成功の条件:
 * <ol>
 *   <li>iss/aud/subで一意のアカウントが見つかる</li>
 *   <li>そのアカウントの最新ステータスが認証可能な状態（ACTIVE）</li>
 *   <li>SYSTEM_ADMINロールが付与されている</li>
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

    /** システム管理アカウントロールリポジトリ */
    private final SystemAccountRoleRepository systemAccountRoleRepository;

    /**
     * OIDCのiss/aud/subから、システム管理者として認証可能か判定する。
     *
     * @param iss OIDC Issuer
     * @param aud OIDC Audience
     * @param sub OIDC Subject
     * @return 認証成功時のシステム管理アカウント
     * @throws SystemAuthenticationException 認証失敗時
     */
    @Transactional(readOnly = true)
    public SystemAccount authenticate(String iss, String aud, String sub) {
        SystemAccount account = findAccount(iss, aud, sub);
        verifyActive(account.getAccountId());
        verifySystemAdminRole(account.getAccountId());
        return account;
    }

    /**
     * iss/aud/subでアカウントを検索する。見つからなければ例外。
     *
     * @param iss OIDC Issuer
     * @param aud OIDC Audience
     * @param sub OIDC Subject
     * @return システム管理アカウント
     * @throws SystemAuthenticationException アカウントが見つからない場合
     */
    private SystemAccount findAccount(String iss, String aud, String sub) {
        return systemAccountRepository.findLatestByIssAndAudAndSub(iss, aud, sub)
                .orElseThrow(() -> new SystemAuthenticationException(
                        SystemAuthenticationErrorType.ACCOUNT_NOT_FOUND));
    }

    /**
     * アカウントの最新ステータスが認証可能な状態か検証する。
     *
     * <p>認証可能な状態の判定は AccountStatus#isAuthenticatable に委譲する。</p>
     *
     * @param accountId アカウントID
     * @throws SystemAuthenticationException ステータスが存在しないか、認証不可の状態の場合
     */
    private void verifyActive(String accountId) {
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
     * アカウントにSYSTEM_ADMINロールが付与されているか検証する。
     *
     * @param accountId アカウントID
     * @throws SystemAuthenticationException SYSTEM_ADMINロールが付与されていない場合
     */
    private void verifySystemAdminRole(String accountId) {
        List<SystemAccountRole> roles =
                systemAccountRoleRepository.findLatestByAccountId(accountId);

        boolean hasSystemAdmin = roles.stream()
                .anyMatch(r -> r.getRole() == SystemRole.SYSTEM_ADMIN);
        if (!hasSystemAdmin) {
            throw new SystemAuthenticationException(
                    SystemAuthenticationErrorType.ROLE_NOT_GRANTED);
        }
    }

}
