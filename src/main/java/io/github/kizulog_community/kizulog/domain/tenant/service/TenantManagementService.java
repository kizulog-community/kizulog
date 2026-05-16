package io.github.kizulog_community.kizulog.domain.tenant.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantHostError;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantHostException;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantRegistrationError;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantRegistrationException;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantStatusChangeError;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantStatusChangeException;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantUpdateError;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantUpdateException;
import io.github.kizulog_community.kizulog.domain.tenant.model.Tenant;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantDetailView;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHost;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostStatus;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostStatusHistoryEntry;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostStatusValue;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostView;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantListItemView;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantStatus;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantStatusHistoryEntry;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantStatusValue;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantHostRepository;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantHostStatusRepository;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantRepository;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantStatusRepository;
import lombok.RequiredArgsConstructor;

/**
 * 業務テナント管理サービス
 *
 * <p>SYSTEM_ADMIN が業務テナント（顧客企業）を管理するためのドメインサービス。
 * 一覧/詳細取得、新規作成、name編集、ステータス変更、host追加/無効化/再有効化を提供する。</p>
 *
 * <p>テナント識別方式: 方式3「全URLにテナント識別子」。
 * URLパターン: https://{host}/t/{slug}/...
 * 1テナント:Nホスト、複数テナント:1ホストのM:N関係を tenant_hosts で表現する。</p>
 *
 * <p>テナント新規作成は1トランザクション内で以下を保存する:</p>
 * <ol>
 * <li>tenants (tenant_id=UUID, slug=NanoID 21文字)</li>
 * <li>tenant_status (status=ACTIVE)</li>
 * <li>tenant_hosts (要求された各host)</li>
 * <li>tenant_host_status (各host=ACTIVE)</li>
 * </ol>
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class TenantManagementService {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(TenantManagementService.class);

    /** テナント名の最大長 */
    private static final int NAME_MAX_LENGTH = 100;

    /** reasonの最大長 */
    private static final int REASON_MAX_LENGTH = 1000;

    /** slug衝突発生時の再試行回数（NanoID 21文字なので実質1回で生成成功する） */
    private static final int SLUG_GENERATION_MAX_ATTEMPTS = 10;

    /** 作成者プレフィックス: テナント新規登録 */
    private static final String CREATED_BY_REGISTER = "system:tenant-register:";

    /** 作成者プレフィックス: name更新 */
    private static final String CREATED_BY_NAME_UPDATE = "system:tenant-name-update:";

    /** 作成者プレフィックス: ステータス変更 */
    private static final String CREATED_BY_STATUS_CHANGE = "system:tenant-status-change:";

    /** 作成者プレフィックス: host追加 */
    private static final String CREATED_BY_HOST_ADD = "system:tenant-host-add:";

    /** 作成者プレフィックス: hostステータス変更 */
    private static final String CREATED_BY_HOST_STATUS_CHANGE =
            "system:tenant-host-status-change:";

    /** テナントリポジトリ */
    private final TenantRepository tenantRepository;

    /** テナントステータスリポジトリ */
    private final TenantStatusRepository tenantStatusRepository;

    /** テナントホストリポジトリ */
    private final TenantHostRepository tenantHostRepository;

    /** テナントホストステータスリポジトリ */
    private final TenantHostStatusRepository tenantHostStatusRepository;

    /** slug生成サービス */
    private final SlugGenerator slugGenerator;

    /** host検証/正規化サービス */
    private final HostNormalizer hostNormalizer;

    /**
     * 全テナントの一覧を表示用ビューで取得する。
     *
     * @return 一覧
     */
    @Transactional(readOnly = true)
    public List<TenantListItemView> listAllTenants() {
        List<Tenant> tenants = tenantRepository.findAllLatest();
        List<TenantListItemView> views = new ArrayList<>();
        for (Tenant tenant : tenants) {
            TenantStatusValue currentStatus = tenantStatusRepository
                    .findLatestByTenantId(tenant.getTenantId())
                    .map(TenantStatus::getStatus)
                    .orElse(TenantStatusValue.INACTIVE);

            List<TenantHost> hosts =
                    tenantHostRepository.findAllLatestByTenantId(tenant.getTenantId());
            int totalHostCount = hosts.size();
            List<String> activeHosts = new ArrayList<>();
            for (TenantHost host : hosts) {
                Optional<TenantHostStatus> hostStatusOpt = tenantHostStatusRepository
                        .findLatestByTenantIdAndHost(host.getTenantId(), host.getHost());
                if (hostStatusOpt.isPresent()
                        && hostStatusOpt.get().getStatus() == TenantHostStatusValue.ACTIVE) {
                    activeHosts.add(host.getHost());
                }
            }
            activeHosts.sort(Comparator.naturalOrder());

            views.add(new TenantListItemView(
                    tenant.getTenantId(),
                    tenant.getName(),
                    tenant.getSlug(),
                    currentStatus,
                    tenant.getCreatedAt(),
                    activeHosts,
                    totalHostCount));
        }

        views.sort(Comparator
                .<TenantListItemView, Integer>comparing(v -> statusOrder(v.getCurrentStatus()))
                .thenComparing(TenantListItemView::getCreatedAt, Comparator.reverseOrder()));

        return views;
    }

    /**
     * 指定テナントの詳細ビューを取得する。
     *
     * @param tenantId テナントID
     * @return 詳細ビュー。存在しない場合は空のOptional
     */
    @Transactional(readOnly = true)
    public Optional<TenantDetailView> findTenantDetail(String tenantId) {
        Optional<Tenant> tenantOpt = tenantRepository.findLatestByTenantId(tenantId);
        if (tenantOpt.isEmpty()) {
            return Optional.empty();
        }
        Tenant tenant = tenantOpt.get();

        Optional<TenantStatus> currentStatusOpt =
                tenantStatusRepository.findLatestByTenantId(tenantId);
        TenantStatusValue currentStatus = currentStatusOpt
                .map(TenantStatus::getStatus)
                .orElse(TenantStatusValue.INACTIVE);
        String currentStatusReason = currentStatusOpt.map(TenantStatus::getReason).orElse(null);
        OffsetDateTime currentStatusVersion =
                currentStatusOpt.map(TenantStatus::getVersion).orElse(null);
        String currentStatusUpdatedBy =
                currentStatusOpt.map(TenantStatus::getCreatedBy).orElse(null);

        // ステータス履歴
        List<TenantStatus> statusHistoryDomain =
                tenantStatusRepository.findAllByTenantIdOrderByVersionDesc(tenantId);
        List<TenantStatusHistoryEntry> statusHistory = new ArrayList<>();
        for (TenantStatus h : statusHistoryDomain) {
            statusHistory.add(new TenantStatusHistoryEntry(
                    h.getVersion(),
                    h.getStatus(),
                    h.getReason(),
                    h.getCreatedAt(),
                    h.getCreatedBy()));
        }

        // host一覧（履歴含む）
        List<TenantHost> hosts = tenantHostRepository.findAllLatestByTenantId(tenantId);
        List<TenantHostView> hostViews = new ArrayList<>();
        for (TenantHost host : hosts) {
            Optional<TenantHostStatus> hostStatusOpt = tenantHostStatusRepository
                    .findLatestByTenantIdAndHost(host.getTenantId(), host.getHost());
            TenantHostStatusValue hostCurrentStatus = hostStatusOpt
                    .map(TenantHostStatus::getStatus)
                    .orElse(TenantHostStatusValue.INACTIVE);
            String hostCurrentReason =
                    hostStatusOpt.map(TenantHostStatus::getReason).orElse(null);
            OffsetDateTime hostCurrentVersion =
                    hostStatusOpt.map(TenantHostStatus::getVersion).orElse(null);
            String hostCurrentUpdatedBy =
                    hostStatusOpt.map(TenantHostStatus::getCreatedBy).orElse(null);

            List<TenantHostStatus> hostStatusHistoryDomain = tenantHostStatusRepository
                    .findAllByTenantIdAndHostOrderByVersionDesc(
                            host.getTenantId(), host.getHost());
            List<TenantHostStatusHistoryEntry> hostStatusHistory = new ArrayList<>();
            for (TenantHostStatus h : hostStatusHistoryDomain) {
                hostStatusHistory.add(new TenantHostStatusHistoryEntry(
                        h.getVersion(),
                        h.getStatus(),
                        h.getReason(),
                        h.getCreatedAt(),
                        h.getCreatedBy()));
            }

            hostViews.add(new TenantHostView(
                    host.getHost(),
                    host.getCreatedAt(),
                    host.getCreatedBy(),
                    hostCurrentStatus,
                    hostCurrentReason,
                    hostCurrentVersion,
                    hostCurrentUpdatedBy,
                    hostStatusHistory));
        }
        // host表示順: ACTIVE優先 → host名のアルファベット昇順
        hostViews.sort(Comparator
                .<TenantHostView, Integer>comparing(v ->
                        v.getCurrentStatus() == TenantHostStatusValue.ACTIVE ? 0 : 1)
                .thenComparing(TenantHostView::getHost, Comparator.naturalOrder()));

        return Optional.of(new TenantDetailView(
                tenant.getTenantId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getCreatedAt(),
                tenant.getCreatedBy(),
                currentStatus,
                currentStatusReason,
                currentStatusVersion,
                currentStatusUpdatedBy,
                statusHistory,
                hostViews));
    }

    /**
     * slugで最新のテナントを取得する。
     *
     * @param slug URL用slug
     * @return 該当テナント。存在しない場合は空のOptional
     */
    @Transactional(readOnly = true)
    public Optional<Tenant> findTenantBySlug(String slug) {
        return tenantRepository.findLatestBySlug(slug);
    }

    /**
     * 新規テナントを登録する。
     *
     * <p>1トランザクション内で以下を保存:</p>
     * <ol>
     * <li>tenants (tenant_id=UUID, slug=NanoID)</li>
     * <li>tenant_status (status=ACTIVE, reason)</li>
     * <li>tenant_hosts (要求された各host)</li>
     * <li>tenant_host_status (各host=ACTIVE, reason)</li>
     * </ol>
     *
     * @param name テナント名（1-100文字、必須）
     * @param rawHosts 登録するhost文字列のリスト（1個以上、各host検証）
     * @param reason 変更理由（1-1000文字、必須）
     * @param operatorAccountId 操作者のアカウントID（createdBy用）
     * @return 採番された tenant_id
     * @throws TenantRegistrationException 検証エラー時
     */
    @Transactional
    public String registerTenant(
            String name,
            List<String> rawHosts,
            String reason,
            String operatorAccountId) {

        validateName(name, () -> new TenantRegistrationException(
                TenantRegistrationError.NAME_INVALID));
        validateReason(reason, () -> new TenantRegistrationException(
                TenantRegistrationError.REASON_INVALID));

        if (rawHosts == null || rawHosts.isEmpty()) {
            log.warn("テナント新規登録拒否: host指定なし");
            throw new TenantRegistrationException(TenantRegistrationError.HOST_EMPTY);
        }

        // 各hostを正規化＋検証。重複は早期に検出する。
        List<String> normalizedHosts = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String raw : rawHosts) {
            String normalized;
            try {
                normalized = hostNormalizer.normalizeAndValidate(raw);
            } catch (IllegalArgumentException e) {
                log.warn("テナント新規登録拒否: host検証失敗: raw={}, error={}", raw, e.getMessage());
                throw new TenantRegistrationException(TenantRegistrationError.HOST_INVALID);
            }
            if (!seen.add(normalized)) {
                log.warn("テナント新規登録拒否: 同一リクエスト内でhost重複: {}", normalized);
                throw new TenantRegistrationException(
                        TenantRegistrationError.HOST_DUPLICATE_IN_REQUEST);
            }
            normalizedHosts.add(normalized);
        }

        // tenant_id 採番
        String tenantId = UUID.randomUUID().toString();

        // slug 採番（衝突時は再試行、ただし通常1回で成功する）
        String slug = generateUniqueSlug();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String createdBy = CREATED_BY_REGISTER + operatorAccountId;

        // 1. tenants
        tenantRepository.save(new Tenant(tenantId, now, name.trim(), slug, now, createdBy));

        // 2. tenant_status (ACTIVE)
        tenantStatusRepository.save(new TenantStatus(
                tenantId, now, TenantStatusValue.ACTIVE, reason.trim(), now, createdBy));

        // 3. tenant_hosts + 4. tenant_host_status (各ACTIVE)
        for (String host : normalizedHosts) {
            tenantHostRepository.save(new TenantHost(tenantId, host, now, now, createdBy));
            tenantHostStatusRepository.save(new TenantHostStatus(
                    tenantId, host, now,
                    TenantHostStatusValue.ACTIVE,
                    reason.trim(), now, createdBy));
        }

        log.info("テナントを新規登録しました: tenantId={}, slug={}, name={}, hosts={}, "
                        + "operatorAccountId={}",
                tenantId, slug, name, normalizedHosts, operatorAccountId);

        return tenantId;
    }

    /**
     * テナントnameを更新する。
     *
     * @param tenantId 対象テナントID
     * @param newName 新しいテナント名（1-100文字、必須）
     * @param reason 変更理由（1-1000文字、必須）
     * @param operatorAccountId 操作者のアカウントID
     * @throws TenantUpdateException 検証エラー時
     */
    @Transactional
    public void updateTenantName(
            String tenantId,
            String newName,
            String reason,
            String operatorAccountId) {

        Optional<Tenant> currentOpt = tenantRepository.findLatestByTenantId(tenantId);
        if (currentOpt.isEmpty()) {
            log.warn("テナントname更新拒否: テナント不在: tenantId={}", tenantId);
            throw new TenantUpdateException(TenantUpdateError.TENANT_NOT_FOUND);
        }
        Tenant current = currentOpt.get();

        validateName(newName, () -> new TenantUpdateException(
                TenantUpdateError.NAME_INVALID));
        validateReason(reason, () -> new TenantUpdateException(
                TenantUpdateError.REASON_INVALID));

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String createdBy = CREATED_BY_NAME_UPDATE + operatorAccountId;
        tenantRepository.save(new Tenant(
                tenantId, now, newName.trim(), current.getSlug(), now, createdBy));

        log.info("テナントnameを更新しました: tenantId={}, oldName={}, newName={}, "
                        + "reason={}, operatorAccountId={}",
                tenantId, current.getName(), newName, reason, operatorAccountId);
    }

    /**
     * テナントのステータスを変更する。
     *
     * @param tenantId 対象テナントID
     * @param newStatus 新しいステータス
     * @param reason 変更理由（1-1000文字、必須）
     * @param operatorAccountId 操作者のアカウントID
     * @throws TenantStatusChangeException 検証エラー時
     */
    @Transactional
    public void changeTenantStatus(
            String tenantId,
            TenantStatusValue newStatus,
            String reason,
            String operatorAccountId) {

        Optional<Tenant> tenantOpt = tenantRepository.findLatestByTenantId(tenantId);
        if (tenantOpt.isEmpty()) {
            log.warn("テナントステータス変更拒否: テナント不在: tenantId={}", tenantId);
            throw new TenantStatusChangeException(TenantStatusChangeError.TENANT_NOT_FOUND);
        }

        validateReason(reason, () -> new TenantStatusChangeException(
                TenantStatusChangeError.REASON_INVALID));

        TenantStatusValue currentStatus = tenantStatusRepository
                .findLatestByTenantId(tenantId)
                .map(TenantStatus::getStatus)
                .orElse(TenantStatusValue.INACTIVE);
        if (currentStatus == newStatus) {
            log.warn("テナントステータス変更拒否: 既に同一ステータス: "
                    + "tenantId={}, status={}", tenantId, newStatus);
            throw new TenantStatusChangeException(
                    TenantStatusChangeError.ALREADY_IN_TARGET_STATUS);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String createdBy = CREATED_BY_STATUS_CHANGE + operatorAccountId;
        tenantStatusRepository.save(new TenantStatus(
                tenantId, now, newStatus, reason.trim(), now, createdBy));

        log.info("テナントステータスを変更しました: tenantId={}, from={}, to={}, "
                        + "operatorAccountId={}",
                tenantId, currentStatus, newStatus, operatorAccountId);
    }

    /**
     * テナントに新しいhostを追加する。
     *
     * @param tenantId 対象テナントID
     * @param rawHost ホスト名（検証＋正規化される）
     * @param reason 変更理由（1-1000文字、必須）
     * @param operatorAccountId 操作者のアカウントID
     * @throws TenantHostException 検証エラー時
     */
    @Transactional
    public void addHost(
            String tenantId,
            String rawHost,
            String reason,
            String operatorAccountId) {

        Optional<Tenant> tenantOpt = tenantRepository.findLatestByTenantId(tenantId);
        if (tenantOpt.isEmpty()) {
            log.warn("host追加拒否: テナント不在: tenantId={}", tenantId);
            throw new TenantHostException(TenantHostError.TENANT_NOT_FOUND);
        }

        validateReason(reason, () -> new TenantHostException(TenantHostError.REASON_INVALID));

        String normalizedHost;
        try {
            normalizedHost = hostNormalizer.normalizeAndValidate(rawHost);
        } catch (IllegalArgumentException e) {
            log.warn("host追加拒否: host検証失敗: tenantId={}, raw={}, error={}",
                    tenantId, rawHost, e.getMessage());
            throw new TenantHostException(TenantHostError.HOST_INVALID);
        }

        if (tenantHostRepository.existsByTenantIdAndHost(tenantId, normalizedHost)) {
            log.warn("host追加拒否: 同一テナントへの重複: tenantId={}, host={}",
                    tenantId, normalizedHost);
            throw new TenantHostException(TenantHostError.HOST_DUPLICATE);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String createdBy = CREATED_BY_HOST_ADD + operatorAccountId;

        tenantHostRepository.save(new TenantHost(
                tenantId, normalizedHost, now, now, createdBy));
        tenantHostStatusRepository.save(new TenantHostStatus(
                tenantId, normalizedHost, now,
                TenantHostStatusValue.ACTIVE,
                reason.trim(), now, createdBy));

        log.info("テナントhostを追加しました: tenantId={}, host={}, operatorAccountId={}",
                tenantId, normalizedHost, operatorAccountId);
    }

    /**
     * 既存のテナントhostを無効化（INACTIVE）する。
     *
     * @param tenantId 対象テナントID
     * @param rawHost ホスト名（検証＋正規化される）
     * @param reason 変更理由（1-1000文字、必須）
     * @param operatorAccountId 操作者のアカウントID
     * @throws TenantHostException 検証エラー時
     */
    @Transactional
    public void disableHost(
            String tenantId,
            String rawHost,
            String reason,
            String operatorAccountId) {
        changeHostStatus(
                tenantId, rawHost, TenantHostStatusValue.INACTIVE,
                reason, operatorAccountId);
    }

    /**
     * INACTIVE状態のテナントhostを再有効化（ACTIVE）する。
     *
     * @param tenantId 対象テナントID
     * @param rawHost ホスト名（検証＋正規化される）
     * @param reason 変更理由（1-1000文字、必須）
     * @param operatorAccountId 操作者のアカウントID
     * @throws TenantHostException 検証エラー時
     */
    @Transactional
    public void enableHost(
            String tenantId,
            String rawHost,
            String reason,
            String operatorAccountId) {
        changeHostStatus(
                tenantId, rawHost, TenantHostStatusValue.ACTIVE,
                reason, operatorAccountId);
    }

    /**
     * hostステータス変更の共通ロジック（無効化・再有効化）
     */
    private void changeHostStatus(
            String tenantId,
            String rawHost,
            TenantHostStatusValue newStatus,
            String reason,
            String operatorAccountId) {

        if (tenantRepository.findLatestByTenantId(tenantId).isEmpty()) {
            log.warn("hostステータス変更拒否: テナント不在: tenantId={}", tenantId);
            throw new TenantHostException(TenantHostError.TENANT_NOT_FOUND);
        }

        validateReason(reason, () -> new TenantHostException(TenantHostError.REASON_INVALID));

        String normalizedHost;
        try {
            normalizedHost = hostNormalizer.normalizeAndValidate(rawHost);
        } catch (IllegalArgumentException e) {
            log.warn("hostステータス変更拒否: host検証失敗: tenantId={}, raw={}, error={}",
                    tenantId, rawHost, e.getMessage());
            throw new TenantHostException(TenantHostError.HOST_INVALID);
        }

        if (tenantHostRepository.findLatestByTenantIdAndHost(tenantId, normalizedHost)
                .isEmpty()) {
            log.warn("hostステータス変更拒否: host不在: tenantId={}, host={}",
                    tenantId, normalizedHost);
            throw new TenantHostException(TenantHostError.HOST_NOT_FOUND);
        }

        TenantHostStatusValue currentStatus = tenantHostStatusRepository
                .findLatestByTenantIdAndHost(tenantId, normalizedHost)
                .map(TenantHostStatus::getStatus)
                .orElse(TenantHostStatusValue.INACTIVE);

        if (currentStatus == newStatus) {
            log.warn("hostステータス変更拒否: 既に同一ステータス: "
                    + "tenantId={}, host={}, status={}",
                    tenantId, normalizedHost, newStatus);
            throw new TenantHostException(TenantHostError.ALREADY_IN_TARGET_STATUS);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String createdBy = CREATED_BY_HOST_STATUS_CHANGE + operatorAccountId;
        tenantHostStatusRepository.save(new TenantHostStatus(
                tenantId, normalizedHost, now,
                newStatus, reason.trim(), now, createdBy));

        log.info("テナントhostステータスを変更しました: tenantId={}, host={}, "
                        + "from={}, to={}, operatorAccountId={}",
                tenantId, normalizedHost, currentStatus, newStatus, operatorAccountId);
    }

    /**
     * 衝突しないslugを生成する。
     *
     * @return 重複しないslug
     * @throws TenantRegistrationException 規定回数試行しても衝突が解消できない場合
     */
    private String generateUniqueSlug() {
        for (int attempt = 0; attempt < SLUG_GENERATION_MAX_ATTEMPTS; attempt++) {
            String candidate = slugGenerator.generate();
            if (!tenantRepository.existsBySlug(candidate)) {
                return candidate;
            }
            log.warn("slug衝突発生 (再試行): attempt={}, slug={}", attempt + 1, candidate);
        }
        log.error("slug生成に失敗: {}回試行しても衝突が解消できず", SLUG_GENERATION_MAX_ATTEMPTS);
        throw new TenantRegistrationException(TenantRegistrationError.SLUG_GENERATION_FAILED);
    }

    /**
     * テナント名の検証。
     *
     * @param name 検証対象
     * @param exceptionSupplier 検証失敗時にスローする例外を生成するサプライヤ
     */
    private void validateName(String name, java.util.function.Supplier<RuntimeException> exceptionSupplier) {
        if (name == null) {
            throw exceptionSupplier.get();
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw exceptionSupplier.get();
        }
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * reason の検証。
     *
     * @param reason 検証対象
     * @param exceptionSupplier 検証失敗時にスローする例外を生成するサプライヤ
     */
    private void validateReason(String reason, java.util.function.Supplier<RuntimeException> exceptionSupplier) {
        if (reason == null) {
            throw exceptionSupplier.get();
        }
        String trimmed = reason.trim();
        if (trimmed.isEmpty()) {
            throw exceptionSupplier.get();
        }
        if (trimmed.length() > REASON_MAX_LENGTH) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 一覧並び順用のステータス優先度。
     */
    private static int statusOrder(TenantStatusValue status) {
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
