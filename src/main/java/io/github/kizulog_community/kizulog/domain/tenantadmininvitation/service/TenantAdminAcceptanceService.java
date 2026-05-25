package io.github.kizulog_community.kizulog.domain.tenantadmininvitation.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import lombok.RequiredArgsConstructor;

/**
 * テナント管理者招待受諾オーケストレーションサービス
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class TenantAdminAcceptanceService {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(TenantAdminAcceptanceService.class);

    /** 受諾フローにおける作成者プレフィックス */
    private static final String CREATED_BY_PREFIX = "tenant:invite:";

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

    /** 招待本体リポジトリ（テナント境界検証用） */
    private final TenantAdminInvitationRepository invitationRepository;

    /** 招待サービス（USED化用） */
    private final TenantAdminInvitationService invitationService;

    /**
     * 招待を受諾し、新規テナント管理者アカウントを作成する。
     *
     * @param invitationId 検証済み招待ID
     * @param resolvedTenantId 受諾アクセス元ホストから解決したテナントID
     * @param iss OIDC Issuer
     * @param aud OIDC Audience（client_id）
     * @param sub OIDC Subject
     * @return 作成された TenantAccountIdentity（accountId・identityId を含む）
     * @throws TenantInvitationException 招待が見つからない / テナント不一致 / identity 既存の場合
     */
    @Transactional
    public TenantAccountIdentity acceptInvitation(
            String invitationId, String resolvedTenantId,
            String iss, String aud, String sub) {

        // 1. 招待を取得し、テナント境界を検証
        TenantAdminInvitation invitation = invitationRepository
                .findLatestByInvitationId(invitationId)
                .orElseThrow(() ->
                        new TenantInvitationException(
                                TenantInvitationError.INVITATION_NOT_FOUND));

        String tenantId = invitation.getTenantId();
        if (!tenantId.equals(resolvedTenantId)) {
            log.warn("テナント不一致の受諾を検出: invitationId={}, "
                            + "invitationTenantId={}, resolvedTenantId={}",
                    invitationId, tenantId, resolvedTenantId);
            throw new TenantInvitationException(TenantInvitationError.TENANT_MISMATCH);
        }

        // 2. identity 重複チェック（同一テナント内で同じ iss/aud/sub が既存でないか）
        tenantAccountIdentityRepository
                .findLatestByTenantIdAndIssAndAudAndSub(tenantId, iss, aud, sub)
                .ifPresent(_ -> {
                    log.warn("既存 identity の受諾を検出: tenantId={}, iss={}, aud={}, sub={}",
                            tenantId, iss, aud, sub);
                    throw new TenantInvitationException(
                            TenantInvitationError.IDENTITY_EXISTS);
                });

        OffsetDateTime version = OffsetDateTime.now(ZoneOffset.UTC);
        String createdBy = CREATED_BY_PREFIX + invitationId;

        String accountId = UUID.randomUUID().toString();
        String identityId = UUID.randomUUID().toString();
        String roleId = UUID.randomUUID().toString();

        // 3. TenantAccount 本体
        tenantAccountRepository.save(new TenantAccount(
                accountId, version, tenantId, version, createdBy));

        // 4. TenantAccountStatus（ACTIVE）
        tenantAccountStatusRepository.save(new TenantAccountStatus(
                accountId, version, TenantAccountStatusValue.ACTIVE, null,
                version, createdBy));

        // 5. TenantAccountIdentity（tenant_id・OIDC情報）
        TenantAccountIdentity identity = new TenantAccountIdentity(
                identityId, version, accountId, tenantId, iss, aud, sub,
                version, createdBy);
        tenantAccountIdentityRepository.save(identity);

        // 6. TenantAccountIdentityStatus（ACTIVE）
        tenantAccountIdentityStatusRepository.save(new TenantAccountIdentityStatus(
                identityId, version, TenantAccountStatusValue.ACTIVE, null,
                version, createdBy));

        // 7. TenantAccountRole（TENANT_ADMIN）
        tenantAccountRoleRepository.save(new TenantAccountRole(
                roleId, version, accountId, TenantRole.TENANT_ADMIN, version, createdBy));

        // 8. TenantAccountRoleStatus（ACTIVE）
        tenantAccountRoleStatusRepository.save(new TenantAccountRoleStatus(
                roleId, version, TenantAccountStatusValue.ACTIVE, null,
                version, createdBy));

        // 9. 招待を USED 状態に更新（同一トランザクションに参加、越境チェック付き）
        invitationService.markAsUsed(tenantId, invitationId, createdBy);

        log.info("招待を受諾して新規テナント管理者を作成しました: "
                        + "invitationId={}, tenantId={}, accountId={}, identityId={}",
                invitationId, tenantId, accountId, identityId);

        return identity;
    }

}
