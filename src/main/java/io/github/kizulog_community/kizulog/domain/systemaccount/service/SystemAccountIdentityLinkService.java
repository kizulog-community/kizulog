package io.github.kizulog_community.kizulog.domain.systemaccount.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kizulog_community.kizulog.domain.systemaccount.exception.IdentityLinkError;
import io.github.kizulog_community.kizulog.domain.systemaccount.exception.IdentityLinkException;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.LinkedIdentityView;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentity;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentityStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityStatusRepository;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.DecryptedOidcProvider;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.OidcProviderStatusValue;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.ProviderWithStatus;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.SystemOidcProviderService;
import lombok.RequiredArgsConstructor;

/**
 * システム管理アカウントへの追加identityリンク・解除サービス
 *
 * <p>「ログイン中のシステム管理者が、別のOIDCプロバイダーを追加連携する」
 * および「追加済みのidentityを連携解除する」ためのドメインサービス。</p>
 *
 * <p>追加（linkIdentity）の事前チェック:</p>
 * <ol>
 * <li>provider_idがENABLEDで存在する</li>
 * <li>当該accountに同一provider(同一iss)のACTIVE identityが存在しない</li>
 * <li>iss/aud/subが既に他のidentity（自account・他account問わず）で使用中でない</li>
 * </ol>
 * <p>いずれかが満たされない場合は、IdentityLinkException を投げる。</p>
 *
 * <p>解除（unlinkIdentity）の事前チェック:</p>
 * <ol>
 * <li>identityが存在する</li>
 * <li>identityのaccountIdが指定accountIdと一致（権限）</li>
 * <li>identityが現セッションで使用中でない</li>
 * <li>当該accountのACTIVE identityが解除後も1件以上残る</li>
 * <li>identityが既にINACTIVEでない</li>
 * </ol>
 * <p>いずれかが満たされない場合は {@link IdentityLinkException} を投げる。</p>
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class SystemAccountIdentityLinkService {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(SystemAccountIdentityLinkService.class);

    /** identity追加時の作成者プレフィックス */
    private static final String LINK_CREATED_BY_PREFIX = "system:identity-link:";

    /** identity解除時の更新者プレフィックス */
    private static final String UNLINK_UPDATED_BY_PREFIX = "system:identity-unlink:";

    /** identityリポジトリ */
    private final SystemAccountIdentityRepository identityRepository;

    /** identityステータスリポジトリ */
    private final SystemAccountIdentityStatusRepository identityStatusRepository;

    /** OIDCプロバイダーサービス（プロバイダー検証・一覧表示用） */
    private final SystemOidcProviderService systemOidcProviderService;

    /**
     * 既存accountに新規identityを追加リンクする。
     *
     * @param accountId 紐付け先のシステム管理アカウントID
     * @param providerId 連携対象のprovider_id（ENABLED必須）
     * @param iss OIDC Issuer URI（OIDCコールバックで取得した値）
     * @param aud OIDC Audience（OIDCコールバックで取得した値）
     * @param sub OIDC Subject（OIDCコールバックで取得した値）
     * @return 作成された identity
     * @throws IdentityLinkException 事前チェック失敗時
     */
    @Transactional
    public SystemAccountIdentity linkIdentity(
            String accountId,
            String providerId,
            String iss,
            String aud,
            String sub) {

        // 1. provider_id がENABLEDで存在することを確認
        DecryptedOidcProvider provider = systemOidcProviderService
                .findEnabledForAuthentication(providerId)
                .orElseThrow(() -> new IdentityLinkException(IdentityLinkError.PROVIDER_NOT_FOUND));

        // 2. 当該accountに同一iss(=同一provider)のACTIVE identityが存在しないことを確認
        if (existsActiveIdentityWithIssForAccount(accountId, provider.getUri())) {
            log.warn("追加identityリンク拒否: 同一プロバイダーのACTIVE identityが既に存在: "
                    + "accountId={}, providerId={}, iss={}",
                    accountId, providerId, provider.getUri());
            throw new IdentityLinkException(IdentityLinkError.PROVIDER_ALREADY_LINKED);
        }

        // 3. iss/aud/sub の組み合わせが他のidentityで使用中でないことを確認
        Optional<SystemAccountIdentity> existing =
                identityRepository.findLatestByIssAndAudAndSub(iss, aud, sub);
        if (existing.isPresent()) {
            log.warn("追加identityリンク拒否: 既存identityが存在: "
                    + "requestedAccountId={}, existingAccountId={}, existingIdentityId={}, "
                    + "iss={}, aud={}, sub={}",
                    accountId, existing.get().getAccountId(),
                    existing.get().getIdentityId(), iss, aud, sub);
            throw new IdentityLinkException(IdentityLinkError.IDENTITY_ALREADY_LINKED);
        }

        // 4. identity + identity_status(ACTIVE) を作成
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String identityId = UUID.randomUUID().toString();
        String createdBy = LINK_CREATED_BY_PREFIX + accountId;

        SystemAccountIdentity identity = new SystemAccountIdentity(
                identityId, now, accountId, iss, aud, sub, now, createdBy);
        identityRepository.save(identity);

        identityStatusRepository.save(new SystemAccountIdentityStatus(
                identityId, now, AccountStatus.ACTIVE, null, now, createdBy));

        log.info("追加identityをリンクしました: "
                        + "accountId={}, identityId={}, providerId={}, iss={}",
                accountId, identityId, providerId, iss);

        return identity;
    }

    /**
     * 指定identityをINACTIVE化（連携解除）する。
     *
     * @param accountId 操作者のaccountId（権限チェック用）
     * @param identityId 解除対象のidentityId
     * @param currentSessionIdentityId 現在の認証セッションで使用中のidentityId（nullable）
     * @param reason 解除理由（任意）
     * @throws IdentityLinkException 事前チェック失敗時
     */
    @Transactional
    public void unlinkIdentity(
            String accountId,
            String identityId,
            String currentSessionIdentityId,
            String reason) {

        // 1. identity が存在することを確認
        SystemAccountIdentity target = identityRepository.findLatestByIdentityId(identityId)
                .orElseThrow(() -> new IdentityLinkException(IdentityLinkError.IDENTITY_NOT_FOUND));

        // 2. 当該identityが操作者のaccountのものであることを確認
        if (!accountId.equals(target.getAccountId())) {
            log.warn("identity解除拒否: 他accountのidentityへのアクセス: "
                    + "operatorAccountId={}, targetAccountId={}, identityId={}",
                    accountId, target.getAccountId(), identityId);
            throw new IdentityLinkException(IdentityLinkError.IDENTITY_NOT_OWNED);
        }

        // 3. 現セッションで使用中のidentityでないことを確認
        if (currentSessionIdentityId != null
                && currentSessionIdentityId.equals(identityId)) {
            log.warn("identity解除拒否: 現セッションで使用中: "
                    + "accountId={}, identityId={}",
                    accountId, identityId);
            throw new IdentityLinkException(IdentityLinkError.CANNOT_UNLINK_CURRENT_SESSION);
        }

        // 4. 現ステータスを取得 (既にINACTIVEなら拒否)
        SystemAccountIdentityStatus currentStatus = identityStatusRepository
                .findLatestByIdentityId(identityId)
                .orElseThrow(() -> new IdentityLinkException(IdentityLinkError.IDENTITY_NOT_FOUND));
        if (currentStatus.getStatus() != AccountStatus.ACTIVE) {
            log.warn("identity解除拒否: 既にINACTIVE: "
                    + "accountId={}, identityId={}",
                    accountId, identityId);
            throw new IdentityLinkException(IdentityLinkError.ALREADY_INACTIVE);
        }

        // 5. 解除後もACTIVE identityが1件以上残ることを確認
        int activeCountAfter = countActiveIdentitiesForAccount(accountId) - 1;
        if (activeCountAfter < 1) {
            log.warn("identity解除拒否: 最後のACTIVE identity: "
                    + "accountId={}, identityId={}",
                    accountId, identityId);
            throw new IdentityLinkException(IdentityLinkError.CANNOT_UNLINK_LAST_ACTIVE);
        }

        // 6. INACTIVE化（status_tableに新規レコード追加・本体は変更しない）
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String updatedBy = UNLINK_UPDATED_BY_PREFIX + accountId;

        identityStatusRepository.save(new SystemAccountIdentityStatus(
                identityId, now, AccountStatus.INACTIVE, reason, now, updatedBy));

        log.info("identityをINACTIVE化（連携解除）しました: "
                        + "accountId={}, identityId={}, reason={}",
                accountId, identityId, reason);
    }

    /**
     * 指定accountに紐付くidentity一覧を、プロバイダー情報・status・現セッションフラグ付きで返す。
     *
     * @param accountId アカウントID
     * @param currentSessionIdentityId 現セッションのidentityId（マーキング用、nullable）
     * @return リンク済みidentity一覧（空リスト返却あり）
     */
    @Transactional(readOnly = true)
    public List<LinkedIdentityView> listLinkedIdentities(
            String accountId, String currentSessionIdentityId) {

        List<SystemAccountIdentity> identities = identityRepository.findLatestByAccountId(accountId);

        // 全プロバイダー（管理用一覧）を1回だけ取得し、issに対応するProviderを引けるようにする
        List<ProviderWithStatus> allProviders = systemOidcProviderService.listAll();

        List<LinkedIdentityView> views = new ArrayList<>();
        for (SystemAccountIdentity identity : identities) {
            Optional<ProviderWithStatus> providerOpt = findProviderByIss(allProviders, identity.getIss());

            SystemAccountIdentityStatus status = identityStatusRepository
                    .findLatestByIdentityId(identity.getIdentityId())
                    .orElse(null);

            boolean active = status != null
                    && status.getStatus() == AccountStatus.ACTIVE;
            boolean currentSession = currentSessionIdentityId != null
                    && currentSessionIdentityId.equals(identity.getIdentityId());

            String providerId = providerOpt
                    .map(pws -> pws.getProvider().getProviderId())
                    .orElse(null);
            String providerDisplayName = providerOpt
                    .map(pws -> pws.getProvider().getDisplayName())
                    .orElse(null);
            boolean providerEnabled = providerOpt
                    .map(pws -> pws.getStatus() != null
                            && pws.getStatus().getStatus() == OidcProviderStatusValue.ENABLED)
                    .orElse(false);

            OffsetDateTime statusVersion = status == null ? identity.getVersion() : status.getVersion();

            views.add(new LinkedIdentityView(
                    identity.getIdentityId(),
                    identity.getIss(),
                    identity.getSub(),
                    providerId,
                    providerDisplayName,
                    providerEnabled,
                    active,
                    currentSession,
                    identity.getCreatedAt(),
                    statusVersion));
        }

        // 並び順: ACTIVE優先 → providerDisplayName昇順(null最後) → identity作成日時昇順
        views.sort(Comparator
                .<LinkedIdentityView, Integer>comparing(v -> v.isActive() ? 0 : 1)
                .thenComparing(LinkedIdentityView::getProviderDisplayName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(LinkedIdentityView::getCreatedAt));

        return views;
    }

    /**
     * 指定accountに対応するENABLEDプロバイダーのうち、まだリンクされていないものを返す。
     *
     * @param accountId アカウントID
     * @return リンク可能なENABLEDプロバイダー一覧（表示名昇順、空リスト返却あり）
     */
    @Transactional(readOnly = true)
    public List<ProviderWithStatus> listLinkableProvidersForAccount(String accountId) {
        // 当該accountが既にACTIVEで連携済のissセット
        List<String> linkedActiveIsses = identityRepository.findLatestByAccountId(accountId).stream()
                .filter(identity -> identityStatusRepository
                        .findLatestByIdentityId(identity.getIdentityId())
                        .map(s -> s.getStatus() == AccountStatus.ACTIVE)
                        .orElse(false))
                .map(SystemAccountIdentity::getIss)
                .toList();

        return systemOidcProviderService.listAll().stream()
                .filter(pws -> pws.getStatus() != null
                        && pws.getStatus().getStatus() == OidcProviderStatusValue.ENABLED)
                .filter(pws -> !linkedActiveIsses.contains(pws.getProvider().getUri()))
                .sorted(Comparator.comparing(
                        pws -> pws.getProvider().getDisplayName(),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    /**
     * 当該accountに、指定issのACTIVE identityが既に存在するか判定する。
     */
    private boolean existsActiveIdentityWithIssForAccount(String accountId, String iss) {
        List<SystemAccountIdentity> identities = identityRepository.findLatestByAccountId(accountId);
        for (SystemAccountIdentity identity : identities) {
            if (!iss.equals(identity.getIss())) {
                continue;
            }
            Optional<SystemAccountIdentityStatus> statusOpt =
                    identityStatusRepository.findLatestByIdentityId(identity.getIdentityId());
            if (statusOpt.isPresent()
                    && statusOpt.get().getStatus() == AccountStatus.ACTIVE) {
                return true;
            }
        }
        return false;
    }

    /**
     * 指定accountにおけるACTIVE identityの件数を返す。
     */
    private int countActiveIdentitiesForAccount(String accountId) {
        int count = 0;
        for (SystemAccountIdentity identity : identityRepository.findLatestByAccountId(accountId)) {
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
     * 全プロバイダー一覧から指定issのプロバイダーを探す。
     *
     * <p>system_oidc_providers.uri は正規化済（normalizeUri適用後）なので
     * identity.iss と直接 equals 比較で一致する想定。</p>
     */
    private Optional<ProviderWithStatus> findProviderByIss(
            List<ProviderWithStatus> allProviders, String iss) {
        for (ProviderWithStatus pws : allProviders) {
            if (iss.equals(pws.getProvider().getUri())) {
                return Optional.of(pws);
            }
        }
        return Optional.empty();
    }

}
