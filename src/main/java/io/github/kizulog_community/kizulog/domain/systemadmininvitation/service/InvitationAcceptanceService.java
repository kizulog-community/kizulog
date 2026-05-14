package io.github.kizulog_community.kizulog.domain.systemadmininvitation.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import lombok.RequiredArgsConstructor;

/**
 * 招待受諾オーケストレーションサービス
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class InvitationAcceptanceService {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(InvitationAcceptanceService.class);

    /** 受諾フローにおける作成者プレフィックス */
    private static final String CREATED_BY_PREFIX = "system:invite:";

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

    /** 招待サービス */
    private final SystemAdminInvitationService invitationService;

    /**
     * 招待を受諾し、新規システム管理者アカウントを作成する。
     *
     * <p>処理:
     * <ol>
     * <li>SystemAccount 新規作成（ACTIVE）</li>
     * <li>SystemAccountIdentity 新規作成（ACTIVE、iss/aud/sub を保存）</li>
     * <li>SystemAccountRole SYSTEM_ADMIN 付与（ACTIVE）</li>
     * <li>招待を USED 状態に更新</li>
     * </ol>
     * すべて同一トランザクション・同一バージョン。</p>
     *
     * @param invitationId 検証済み招待ID
     * @param iss OIDC Issuer
     * @param aud OIDC Audience（client_id）
     * @param sub OIDC Subject
     * @return 作成された SystemAccountIdentity（accountId・identityId を含む）
     */
    @Transactional
    public SystemAccountIdentity acceptInvitation(
            String invitationId, String iss, String aud, String sub) {

        OffsetDateTime version = OffsetDateTime.now(ZoneOffset.UTC);
        String createdBy = CREATED_BY_PREFIX + invitationId;

        String accountId = UUID.randomUUID().toString();
        String identityId = UUID.randomUUID().toString();
        String roleId = UUID.randomUUID().toString();

        // 1. SystemAccount 本体
        systemAccountRepository.save(new SystemAccount(
                accountId, version, version, createdBy));

        // 2. SystemAccountStatus（ACTIVE）
        systemAccountStatusRepository.save(new SystemAccountStatus(
                accountId, version, AccountStatus.ACTIVE, null, version, createdBy));

        // 3. SystemAccountIdentity（OIDC情報）
        SystemAccountIdentity identity = new SystemAccountIdentity(
                identityId, version, accountId, iss, aud, sub, version, createdBy);
        systemAccountIdentityRepository.save(identity);

        // 4. SystemAccountIdentityStatus（ACTIVE）
        systemAccountIdentityStatusRepository.save(new SystemAccountIdentityStatus(
                identityId, version, AccountStatus.ACTIVE, null, version, createdBy));

        // 5. SystemAccountRole（SYSTEM_ADMIN）
        systemAccountRoleRepository.save(new SystemAccountRole(
                roleId, version, accountId, SystemRole.SYSTEM_ADMIN, version, createdBy));

        // 6. SystemAccountRoleStatus（ACTIVE）
        systemAccountRoleStatusRepository.save(new SystemAccountRoleStatus(
                roleId, version, AccountStatus.ACTIVE, null, version, createdBy));

        // 7. 招待を USED 状態に更新（同一トランザクションに参加）
        invitationService.markAsUsed(invitationId, createdBy);

        log.info("招待を受諾して新規システム管理者を作成しました: "
                        + "invitationId={}, accountId={}, identityId={}",
                invitationId, accountId, identityId);

        return identity;
    }

}
