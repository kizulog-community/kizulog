package io.github.kizulog_community.kizulog.domain.systemaccount.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kizulog_community.kizulog.domain.systemaccount.exception.AccountStatusChangeError;
import io.github.kizulog_community.kizulog.domain.systemaccount.exception.AccountStatusChangeException;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountDetailView;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountListItemView;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountStatusHistoryEntry;
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
 * システム管理アカウント管理サービス
 *
 * <p>SYSTEM_ADMIN が他のシステム管理者アカウントを管理するためのドメインサービス。
 * 一覧/詳細取得とステータス変更（ACTIVE/INACTIVE/SUSPENDED 間の任意遷移）を提供する。</p>
 *
 * <p>ステータス変更の事前チェック:</p>
 * <ol>
 * <li>対象アカウントが存在する（AccountStatusChangeError#ACCOUNT_NOT_FOUND）</li>
 * <li>操作者と対象が異なる（自爆禁止、AccountStatusChangeError#CANNOT_CHANGE_SELF）</li>
 * <li>現在ステータスと希望ステータスが異なる（AccountStatusChangeError#ALREADY_IN_TARGET_STATUS）</li>
 * <li>ACTIVE→非ACTIVE の場合、変更後も「ACTIVEアカウント かつ ACTIVEな SYSTEM_ADMINロールを持つ」アカウントが1件以上残る（AccountStatusChangeError#CANNOT_DEACTIVATE_LAST_ACTIVE_ADMIN）</li>
 * </ol>
 *
 * <p>ステータス変更は新規 version レコードを system_account_status に追加することで表現し、過去レコードは決して削除・更新しない（version-based history）</p>
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class SystemAccountManagementService {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(SystemAccountManagementService.class);

    /** ステータス変更時の作成者プレフィックス */
    private static final String STATUS_CHANGE_CREATED_BY_PREFIX =
            "system:account-status-change:";

    /** アカウントリポジトリ */
    private final SystemAccountRepository accountRepository;

    /** アカウントステータスリポジトリ */
    private final SystemAccountStatusRepository accountStatusRepository;

    /** identityリポジトリ */
    private final SystemAccountIdentityRepository identityRepository;

    /** identityステータスリポジトリ */
    private final SystemAccountIdentityStatusRepository identityStatusRepository;

    /** ロールリポジトリ */
    private final SystemAccountRoleRepository roleRepository;

    /** ロールステータスリポジトリ */
    private final SystemAccountRoleStatusRepository roleStatusRepository;

    /**
     * 全システム管理アカウントの一覧を表示用ビューで取得する。
     *
     * @param operatorAccountId 操作者のアカウントID（self 判定用、null許容しない）
     * @return 一覧
     */
    @Transactional(readOnly = true)
    public List<AccountListItemView> listAllAccounts(String operatorAccountId) {
        List<SystemAccount> accounts = accountRepository.findAllLatest();
        List<AccountListItemView> views = new ArrayList<>();
        for (SystemAccount account : accounts) {
            AccountStatus currentStatus = accountStatusRepository
                    .findLatestByAccountId(account.getAccountId())
                    .map(SystemAccountStatus::getStatus)
                    .orElse(AccountStatus.INACTIVE);
            int activeIdentityCount = countActiveIdentities(account.getAccountId());
            boolean systemAdminActive = isSystemAdminRoleActive(account.getAccountId());
            boolean self = account.getAccountId().equals(operatorAccountId);
            views.add(new AccountListItemView(
                    account.getAccountId(),
                    currentStatus,
                    account.getCreatedAt(),
                    activeIdentityCount,
                    systemAdminActive,
                    self));
        }

        views.sort(Comparator
                .<AccountListItemView, Integer>comparing(v -> statusOrder(v.getCurrentStatus()))
                .thenComparing(AccountListItemView::getCreatedAt, Comparator.reverseOrder()));

        return views;
    }

    /**
     * 指定アカウントの詳細ビューを取得する。
     *
     * @param accountId アカウントID
     * @param operatorAccountId 操作者のアカウントID（self 判定用）
     * @return 詳細ビュー。アカウントが存在しない場合は空
     */
    @Transactional(readOnly = true)
    public Optional<AccountDetailView> findAccountDetail(
            String accountId, String operatorAccountId) {

        Optional<SystemAccount> accountOpt =
                accountRepository.findLatestByAccountId(accountId);
        if (accountOpt.isEmpty()) {
            return Optional.empty();
        }
        SystemAccount account = accountOpt.get();

        Optional<SystemAccountStatus> currentStatusOpt =
                accountStatusRepository.findLatestByAccountId(accountId);
        AccountStatus currentStatus = currentStatusOpt
                .map(SystemAccountStatus::getStatus)
                .orElse(AccountStatus.INACTIVE);
        String currentStatusReason = currentStatusOpt
                .map(SystemAccountStatus::getReason)
                .orElse(null);
        OffsetDateTime currentStatusVersion = currentStatusOpt
                .map(SystemAccountStatus::getVersion)
                .orElse(null);
        String currentStatusUpdatedBy = currentStatusOpt
                .map(SystemAccountStatus::getCreatedBy)
                .orElse(null);

        boolean systemAdminActive = isSystemAdminRoleActive(accountId);

        List<SystemAccountIdentity> identities =
                identityRepository.findLatestByAccountId(accountId);
        int totalIdentityCount = identities.size();
        int activeIdentityCount = 0;
        for (SystemAccountIdentity identity : identities) {
            Optional<SystemAccountIdentityStatus> identityStatus =
                    identityStatusRepository.findLatestByIdentityId(identity.getIdentityId());
            if (identityStatus.isPresent()
                    && identityStatus.get().getStatus() == AccountStatus.ACTIVE) {
                activeIdentityCount++;
            }
        }

        List<SystemAccountStatus> historyDomain =
                accountStatusRepository.findAllByAccountIdOrderByVersionDesc(accountId);
        List<AccountStatusHistoryEntry> history = new ArrayList<>();
        for (SystemAccountStatus h : historyDomain) {
            history.add(new AccountStatusHistoryEntry(
                    h.getVersion(),
                    h.getStatus(),
                    h.getReason(),
                    h.getCreatedAt(),
                    h.getCreatedBy()));
        }

        boolean self = accountId.equals(operatorAccountId);

        return Optional.of(new AccountDetailView(
                account.getAccountId(),
                account.getCreatedAt(),
                account.getCreatedBy(),
                currentStatus,
                currentStatusReason,
                currentStatusVersion,
                currentStatusUpdatedBy,
                systemAdminActive,
                activeIdentityCount,
                totalIdentityCount,
                history,
                self));
    }

    /**
     * 指定アカウントのステータスを変更する。
     *
     * @param targetAccountId 変更対象のアカウントID
     * @param newStatus 新しいステータス
     * @param reason 変更理由（必須、空・null不可）
     * @param operatorAccountId 操作者のアカウントID（自爆禁止判定とcreatedBy用）
     * @throws AccountStatusChangeException 事前チェック失敗時
     */
    @Transactional
    public void changeAccountStatus(
            String targetAccountId,
            AccountStatus newStatus,
            String reason,
            String operatorAccountId) {

        // 1. 対象アカウントが存在することを確認
        Optional<SystemAccount> accountOpt =
                accountRepository.findLatestByAccountId(targetAccountId);
        if (accountOpt.isEmpty()) {
            log.warn("ステータス変更拒否: アカウント不在: targetAccountId={}", targetAccountId);
            throw new AccountStatusChangeException(
                    AccountStatusChangeError.ACCOUNT_NOT_FOUND);
        }

        // 2. 自爆禁止: 操作者と対象が同一の場合は拒否
        if (targetAccountId.equals(operatorAccountId)) {
            log.warn("ステータス変更拒否: 自分自身の変更（自爆禁止）: "
                    + "operatorAccountId={}", operatorAccountId);
            throw new AccountStatusChangeException(
                    AccountStatusChangeError.CANNOT_CHANGE_SELF);
        }

        // 3. 現在ステータスと希望ステータスが同一なら拒否
        AccountStatus currentStatus = accountStatusRepository
                .findLatestByAccountId(targetAccountId)
                .map(SystemAccountStatus::getStatus)
                .orElse(AccountStatus.INACTIVE);
        if (currentStatus == newStatus) {
            log.warn("ステータス変更拒否: 既に同一ステータス: "
                    + "targetAccountId={}, status={}", targetAccountId, newStatus);
            throw new AccountStatusChangeException(
                    AccountStatusChangeError.ALREADY_IN_TARGET_STATUS);
        }

        // 4. ACTIVE → 非ACTIVE への遷移時のみ、最後のACTIVE管理者保護を実施
        if (currentStatus == AccountStatus.ACTIVE && newStatus != AccountStatus.ACTIVE) {
            int activeAdminCountAfter = countActiveSystemAdmins() - 1;
            if (activeAdminCountAfter < 1) {
                log.warn("ステータス変更拒否: 最後のACTIVE管理者: "
                        + "targetAccountId={}", targetAccountId);
                throw new AccountStatusChangeException(
                        AccountStatusChangeError.CANNOT_DEACTIVATE_LAST_ACTIVE_ADMIN);
            }
        }

        // 5. 新規 version レコードを追加（履歴保全）
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String createdBy = STATUS_CHANGE_CREATED_BY_PREFIX + operatorAccountId;
        accountStatusRepository.save(new SystemAccountStatus(
                targetAccountId, now, newStatus, reason, now, createdBy));

        log.info("アカウントステータスを変更しました: "
                        + "targetAccountId={}, from={}, to={}, operatorAccountId={}",
                targetAccountId, currentStatus, newStatus, operatorAccountId);
    }

    /**
     * 指定 accountId の現在 ACTIVE な identity 数を数える。
     */
    private int countActiveIdentities(String accountId) {
        int count = 0;
        for (SystemAccountIdentity identity
                : identityRepository.findLatestByAccountId(accountId)) {
            Optional<SystemAccountIdentityStatus> statusOpt =
                    identityStatusRepository.findLatestByIdentityId(identity.getIdentityId());
            if (statusOpt.isPresent()
                    && statusOpt.get().getStatus() == AccountStatus.ACTIVE) {
                count++;
            }
        }
        return count;
    }

    /**
     * 指定 accountId に SYSTEM_ADMIN ロールが ACTIVE で付与されているかを判定する。
     */
    private boolean isSystemAdminRoleActive(String accountId) {
        for (SystemAccountRole role : roleRepository.findLatestByAccountId(accountId)) {
            if (role.getRole() != SystemRole.SYSTEM_ADMIN) {
                continue;
            }
            Optional<SystemAccountRoleStatus> statusOpt =
                    roleStatusRepository.findLatestByRoleId(role.getRoleId());
            if (statusOpt.isPresent()
                    && statusOpt.get().getStatus() == AccountStatus.ACTIVE) {
                return true;
            }
        }
        return false;
    }

    /**
     * ACTIVE な system_accounts
     * かつ ACTIVE な SYSTEM_ADMIN ロールを持つ アカウントの件数を返す。
     * 最後のACTIVE管理者保護に用いる。
     */
    private int countActiveSystemAdmins() {
        int count = 0;
        for (SystemAccount account : accountRepository.findAllLatest()) {
            AccountStatus status = accountStatusRepository
                    .findLatestByAccountId(account.getAccountId())
                    .map(SystemAccountStatus::getStatus)
                    .orElse(AccountStatus.INACTIVE);
            if (status != AccountStatus.ACTIVE) {
                continue;
            }
            if (isSystemAdminRoleActive(account.getAccountId())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 一覧並び順用のステータス優先度。
     */
    private static int statusOrder(AccountStatus status) {
        if (status == null) {
            return 9;
        }
        switch (status) {
            case ACTIVE:
                return 0;
            case SUSPENDED:
                return 1;
            case INACTIVE:
                return 2;
            default:
                return 9;
        }
    }

}
