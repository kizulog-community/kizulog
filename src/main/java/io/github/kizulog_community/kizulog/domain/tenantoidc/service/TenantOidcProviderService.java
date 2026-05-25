package io.github.kizulog_community.kizulog.domain.tenantoidc.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kizulog_community.kizulog.domain.port.CryptoPort;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantRepository;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderRegistrationError;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderRegistrationException;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderStatusChangeError;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderStatusChangeException;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderUpdateError;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderUpdateException;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.DecryptedTenantOidcProvider;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.EnabledTenantOidcProviderView;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProvider;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderDetailView;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderListItemView;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderStatus;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderStatusHistoryEntry;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderStatusValue;
import io.github.kizulog_community.kizulog.domain.tenantoidc.port.TenantOidcProviderRepository;
import io.github.kizulog_community.kizulog.domain.tenantoidc.port.TenantOidcProviderStatusRepository;
import lombok.RequiredArgsConstructor;

/**
 * 業務テナントOIDCプロバイダー管理サービス
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class TenantOidcProviderService {

    /** provider_id 形式: 半角小文字英数字とハイフン、1-32文字 */
    private static final Pattern PROVIDER_ID_PATTERN =
            Pattern.compile("^[a-z0-9-]{1,32}$");

    /** iss URI 形式: http:// または https:// で始まる */
    private static final Pattern ISS_URI_PATTERN =
            Pattern.compile("^https?://.+$");

    /** display_name 最大長 */
    private static final int DISPLAY_NAME_MAX_LENGTH = 100;

    /** iss 最大長 */
    private static final int ISS_MAX_LENGTH = 500;

    /** aud 最大長 */
    private static final int AUD_MAX_LENGTH = 255;

    /** client_id 最大長 */
    private static final int CLIENT_ID_MAX_LENGTH = 255;

    /** reason 最大長 */
    private static final int REASON_MAX_LENGTH = 1000;

    private final TenantRepository tenantRepository;
    private final TenantOidcProviderRepository providerRepository;
    private final TenantOidcProviderStatusRepository statusRepository;
    private final CryptoPort cryptoPort;

    /**
     * テナントの全プロバイダー一覧をビュー形式で取得する。
     *
     * @param tenantId テナントID
     * @return 一覧用ビューのリスト（display_name 昇順）
     */
    @Transactional(readOnly = true)
    public List<TenantOidcProviderListItemView> listAllByTenantId(String tenantId) {
        List<TenantOidcProvider> providers =
                providerRepository.findAllLatestByTenantId(tenantId);

        List<TenantOidcProviderListItemView> result = new ArrayList<>();
        for (TenantOidcProvider provider : providers) {
            Optional<TenantOidcProviderStatus> statusOpt =
                    statusRepository.findLatestByTenantIdAndProviderId(
                            tenantId, provider.getProviderId());
            TenantOidcProviderStatusValue status = statusOpt
                    .map(TenantOidcProviderStatus::getStatus)
                    .orElse(TenantOidcProviderStatusValue.DISABLED);
            result.add(new TenantOidcProviderListItemView(
                    provider.getTenantId(),
                    provider.getProviderId(),
                    provider.getDisplayName(),
                    provider.getIss(),
                    provider.getAud(),
                    status,
                    provider.getCreatedAt()));
        }

        result.sort(Comparator.comparing(TenantOidcProviderListItemView::getDisplayName));
        return result;
    }

    /**
     * テナントの指定プロバイダーの詳細を取得する。
     *
     * @param tenantId テナントID
     * @param providerId プロバイダー識別子
     * @return 詳細ビュー、存在しなければ空Optional
     */
    @Transactional(readOnly = true)
    public Optional<TenantOidcProviderDetailView> findDetail(
            String tenantId, String providerId) {
        Optional<TenantOidcProvider> providerOpt =
                providerRepository.findLatestByTenantIdAndProviderId(tenantId, providerId);
        if (providerOpt.isEmpty()) {
            return Optional.empty();
        }
        TenantOidcProvider provider = providerOpt.get();

        Optional<TenantOidcProviderStatus> currentStatusOpt =
                statusRepository.findLatestByTenantIdAndProviderId(tenantId, providerId);
        TenantOidcProviderStatusValue currentStatus = currentStatusOpt
                .map(TenantOidcProviderStatus::getStatus)
                .orElse(TenantOidcProviderStatusValue.DISABLED);

        List<TenantOidcProviderStatus> history =
                statusRepository.findAllByTenantIdAndProviderIdOrderByVersionDesc(
                        tenantId, providerId);
        List<TenantOidcProviderStatusHistoryEntry> historyEntries = new ArrayList<>();
        for (TenantOidcProviderStatus h : history) {
            historyEntries.add(new TenantOidcProviderStatusHistoryEntry(
                    h.getVersion(),
                    h.getStatus(),
                    h.getReason(),
                    h.getCreatedBy()));
        }

        return Optional.of(new TenantOidcProviderDetailView(
                provider.getTenantId(),
                provider.getProviderId(),
                provider.getDisplayName(),
                provider.getIss(),
                provider.getAud(),
                provider.getClientId(),
                provider.getVersion(),
                provider.getCreatedAt(),
                provider.getCreatedBy(),
                currentStatus,
                currentStatusOpt.map(TenantOidcProviderStatus::getReason).orElse(null),
                currentStatusOpt.map(TenantOidcProviderStatus::getVersion).orElse(null),
                currentStatusOpt.map(TenantOidcProviderStatus::getCreatedBy).orElse(null),
                historyEntries));
    }

    /**
     * テナントの ENABLED プロバイダー一覧をログイン画面用ビューで取得する。
     *
     * @param tenantId テナントID
     * @return ENABLEDプロバイダーのリスト（display_name 昇順）
     */
    @Transactional(readOnly = true)
    public List<EnabledTenantOidcProviderView> findAllEnabledByTenantId(String tenantId) {
        List<TenantOidcProviderStatus> enabledStatuses =
                statusRepository.findAllLatestEnabledByTenantId(tenantId);

        List<EnabledTenantOidcProviderView> result = new ArrayList<>();
        for (TenantOidcProviderStatus s : enabledStatuses) {
            Optional<TenantOidcProvider> providerOpt =
                    providerRepository.findLatestByTenantIdAndProviderId(
                            s.getTenantId(), s.getProviderId());
            if (providerOpt.isPresent()) {
                TenantOidcProvider p = providerOpt.get();
                result.add(new EnabledTenantOidcProviderView(
                        p.getProviderId(), p.getDisplayName()));
            }
        }

        result.sort(Comparator.comparing(EnabledTenantOidcProviderView::getDisplayName));
        return result;
    }

    /**
     * テナントの指定プロバイダーの復号済みDTOを取得する（認証フロー用）
     *
     * @param tenantId テナントID
     * @param providerId プロバイダー識別子
     * @return 復号済みDTO、存在しなければ空Optional
     */
    @Transactional(readOnly = true)
    public Optional<DecryptedTenantOidcProvider> findDecryptedByTenantIdAndProviderId(
            String tenantId, String providerId) {
        return providerRepository.findLatestByTenantIdAndProviderId(tenantId, providerId)
                .map(this::toDecrypted);
    }

    /**
     * テナントの指定プロバイダーが認証に利用可能（ENABLED）な場合のみ復号済みDTOを取得する。
     *
     * @param tenantId テナントID
     * @param providerId プロバイダー識別子
     * @return ENABLEDな場合の復号済みDTO、それ以外は空Optional
     */
    @Transactional(readOnly = true)
    public Optional<DecryptedTenantOidcProvider> findEnabledForAuthentication(
            String tenantId, String providerId) {
        Optional<TenantOidcProviderStatus> statusOpt =
                statusRepository.findLatestByTenantIdAndProviderId(tenantId, providerId);
        if (statusOpt.isEmpty()
                || statusOpt.get().getStatus() != TenantOidcProviderStatusValue.ENABLED) {
            return Optional.empty();
        }
        return providerRepository.findLatestByTenantIdAndProviderId(tenantId, providerId)
                .map(this::toDecrypted);
    }

    /**
     * (iss, aud) に紐づく全テナントのプロバイダーを復号済みDTOで取得する。
     *
     * @param iss OIDC Issuer URI
     * @param aud Audience
     * @return 復号済みDTOのリスト
     */
    @Transactional(readOnly = true)
    public List<DecryptedTenantOidcProvider> findDecryptedByIssAndAud(
            String iss, String aud) {
        List<TenantOidcProvider> providers =
                providerRepository.findAllLatestByIssAndAud(iss, aud);
        List<DecryptedTenantOidcProvider> result = new ArrayList<>();
        for (TenantOidcProvider p : providers) {
            result.add(toDecrypted(p));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 新規プロバイダーを登録する。
     *
     * @param tenantId テナントID
     * @param providerId プロバイダー識別子
     * @param displayName 表示名
     * @param iss OIDC Issuer URI
     * @param aud Audience
     * @param clientId クライアントID
     * @param plainClientSecret 平文クライアントシークレット
     * @param reason 登録理由
     * @param operatorId 操作者accountId
     */
    @Transactional
    public void registerProvider(
            String tenantId,
            String providerId,
            String displayName,
            String iss,
            String aud,
            String clientId,
            String plainClientSecret,
            String reason,
            String operatorId) {

        // 親テナント存在確認
        if (tenantRepository.findLatestByTenantId(tenantId).isEmpty()) {
            throw new TenantOidcProviderRegistrationException(
                    TenantOidcProviderRegistrationError.TENANT_NOT_FOUND);
        }

        // 各種バリデーション
        validateProviderIdForRegistration(providerId);
        validateDisplayName(displayName,
                TenantOidcProviderRegistrationError.DISPLAY_NAME_INVALID);
        validateIss(iss);
        validateAud(aud);
        validateClientId(clientId,
                TenantOidcProviderRegistrationError.CLIENT_ID_INVALID);
        if (plainClientSecret == null || plainClientSecret.isEmpty()) {
            throw new TenantOidcProviderRegistrationException(
                    TenantOidcProviderRegistrationError.CLIENT_SECRET_INVALID);
        }
        validateReason(reason,
                TenantOidcProviderRegistrationError.REASON_INVALID);

        // 一意性チェック
        if (providerRepository.existsByTenantIdAndProviderId(tenantId, providerId)) {
            throw new TenantOidcProviderRegistrationException(
                    TenantOidcProviderRegistrationError.PROVIDER_ID_DUPLICATE);
        }
        if (existsIssAudInTenant(tenantId, iss, aud)) {
            throw new TenantOidcProviderRegistrationException(
                    TenantOidcProviderRegistrationError.ISS_AUD_DUPLICATE);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String encryptedSecret = cryptoPort.encrypt(plainClientSecret);

        // プロバイダー本体保存
        TenantOidcProvider provider = new TenantOidcProvider(
                tenantId, providerId, now,
                displayName, iss, aud, clientId, encryptedSecret,
                now, "system:tenant-oidc-register:" + operatorId);
        providerRepository.save(provider);

        // 初期ステータス（ENABLED）保存
        TenantOidcProviderStatus status = new TenantOidcProviderStatus(
                tenantId, providerId, now,
                TenantOidcProviderStatusValue.ENABLED,
                reason,
                now, "system:tenant-oidc-register:" + operatorId);
        statusRepository.save(status);
    }

    /**
     * プロバイダーの編集可能項目（display_name / client_id / client_secret）を更新する。
     *
     * @param tenantId テナントID
     * @param providerId プロバイダー識別子
     * @param displayName 新しい表示名
     * @param clientId 新しいクライアントID
     * @param plainClientSecret 新しい平文クライアントシークレット（null/空なら現在値維持）
     * @param reason 変更理由
     * @param operatorId 操作者accountId
     */
    @Transactional
    public void updateProvider(
            String tenantId,
            String providerId,
            String displayName,
            String clientId,
            String plainClientSecret,
            String reason,
            String operatorId) {

        // 親テナント存在確認
        if (tenantRepository.findLatestByTenantId(tenantId).isEmpty()) {
            throw new TenantOidcProviderUpdateException(
                    TenantOidcProviderUpdateError.TENANT_NOT_FOUND);
        }

        // 対象プロバイダー取得
        Optional<TenantOidcProvider> currentOpt =
                providerRepository.findLatestByTenantIdAndProviderId(tenantId, providerId);
        if (currentOpt.isEmpty()) {
            throw new TenantOidcProviderUpdateException(
                    TenantOidcProviderUpdateError.PROVIDER_NOT_FOUND);
        }
        TenantOidcProvider current = currentOpt.get();

        // バリデーション
        validateDisplayName(displayName,
                TenantOidcProviderUpdateError.DISPLAY_NAME_INVALID);
        validateClientId(clientId,
                TenantOidcProviderUpdateError.CLIENT_ID_INVALID);
        validateReason(reason,
                TenantOidcProviderUpdateError.REASON_INVALID);

        // client_secret: 空なら現在値維持、入力ありなら暗号化
        String encryptedSecret;
        if (plainClientSecret == null || plainClientSecret.isEmpty()) {
            encryptedSecret = current.getClientSecret(); // 暗号化済み値をそのまま継承
        } else {
            encryptedSecret = cryptoPort.encrypt(plainClientSecret);
        }

        // 新version保存（iss/aud は不変）
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        TenantOidcProvider updated = new TenantOidcProvider(
                tenantId, providerId, now,
                displayName, current.getIss(), current.getAud(),
                clientId, encryptedSecret,
                now, "system:tenant-oidc-update:" + operatorId);
        providerRepository.save(updated);
    }

    /**
     * プロバイダーのステータス（ENABLED / DISABLED）を変更する。
     *
     * @param tenantId テナントID
     * @param providerId プロバイダー識別子
     * @param targetStatus 変更先ステータス
     * @param reason 変更理由
     * @param operatorId 操作者accountId
     */
    @Transactional
    public void changeStatus(
            String tenantId,
            String providerId,
            TenantOidcProviderStatusValue targetStatus,
            String reason,
            String operatorId) {

        // 親テナント存在確認
        if (tenantRepository.findLatestByTenantId(tenantId).isEmpty()) {
            throw new TenantOidcProviderStatusChangeException(
                    TenantOidcProviderStatusChangeError.TENANT_NOT_FOUND);
        }

        // 対象プロバイダー取得
        Optional<TenantOidcProvider> providerOpt =
                providerRepository.findLatestByTenantIdAndProviderId(tenantId, providerId);
        if (providerOpt.isEmpty()) {
            throw new TenantOidcProviderStatusChangeException(
                    TenantOidcProviderStatusChangeError.PROVIDER_NOT_FOUND);
        }

        // バリデーション
        validateReason(reason,
                TenantOidcProviderStatusChangeError.REASON_INVALID);

        // 同一ステータス遷移防止
        Optional<TenantOidcProviderStatus> currentStatusOpt =
                statusRepository.findLatestByTenantIdAndProviderId(tenantId, providerId);
        if (currentStatusOpt.isPresent()
                && currentStatusOpt.get().getStatus() == targetStatus) {
            throw new TenantOidcProviderStatusChangeException(
                    TenantOidcProviderStatusChangeError.ALREADY_IN_TARGET_STATUS);
        }

        // 新version保存
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        TenantOidcProviderStatus newStatus = new TenantOidcProviderStatus(
                tenantId, providerId, now,
                targetStatus, reason,
                now, "system:tenant-oidc-status-change:" + operatorId);
        statusRepository.save(newStatus);
    }

    private DecryptedTenantOidcProvider toDecrypted(TenantOidcProvider p) {
        String plainSecret = cryptoPort.decrypt(p.getClientSecret());
        return new DecryptedTenantOidcProvider(
                p.getTenantId(),
                p.getProviderId(),
                p.getVersion(),
                p.getDisplayName(),
                p.getIss(),
                p.getAud(),
                p.getClientId(),
                plainSecret);
    }

    /**
     * 同一テナント内に同一 (iss, aud) のプロバイダーが既に存在するかを判定する。
     */
    private boolean existsIssAudInTenant(String tenantId, String iss, String aud) {
        List<TenantOidcProvider> sameIssAud =
                providerRepository.findAllLatestByIssAndAud(iss, aud);
        for (TenantOidcProvider p : sameIssAud) {
            if (tenantId.equals(p.getTenantId())) {
                return true;
            }
        }
        return false;
    }

    private void validateProviderIdForRegistration(String providerId) {
        if (providerId == null
                || !PROVIDER_ID_PATTERN.matcher(providerId).matches()) {
            throw new TenantOidcProviderRegistrationException(
                    TenantOidcProviderRegistrationError.PROVIDER_ID_INVALID);
        }
    }

    private void validateDisplayName(
            String value, TenantOidcProviderRegistrationError error) {
        if (value == null
                || value.trim().isEmpty()
                || value.length() > DISPLAY_NAME_MAX_LENGTH) {
            throw new TenantOidcProviderRegistrationException(error);
        }
    }

    private void validateDisplayName(
            String value, TenantOidcProviderUpdateError error) {
        if (value == null
                || value.trim().isEmpty()
                || value.length() > DISPLAY_NAME_MAX_LENGTH) {
            throw new TenantOidcProviderUpdateException(error);
        }
    }

    private void validateIss(String iss) {
        if (iss == null
                || iss.trim().isEmpty()
                || iss.length() > ISS_MAX_LENGTH
                || !ISS_URI_PATTERN.matcher(iss).matches()) {
            throw new TenantOidcProviderRegistrationException(
                    TenantOidcProviderRegistrationError.ISS_INVALID);
        }
    }

    private void validateAud(String aud) {
        if (aud == null
                || aud.trim().isEmpty()
                || aud.length() > AUD_MAX_LENGTH) {
            throw new TenantOidcProviderRegistrationException(
                    TenantOidcProviderRegistrationError.AUD_INVALID);
        }
    }

    private void validateClientId(
            String value, TenantOidcProviderRegistrationError error) {
        if (value == null
                || value.trim().isEmpty()
                || value.length() > CLIENT_ID_MAX_LENGTH) {
            throw new TenantOidcProviderRegistrationException(error);
        }
    }

    private void validateClientId(
            String value, TenantOidcProviderUpdateError error) {
        if (value == null
                || value.trim().isEmpty()
                || value.length() > CLIENT_ID_MAX_LENGTH) {
            throw new TenantOidcProviderUpdateException(error);
        }
    }

    private void validateReason(
            String reason, TenantOidcProviderRegistrationError error) {
        if (reason == null
                || reason.trim().isEmpty()
                || reason.length() > REASON_MAX_LENGTH) {
            throw new TenantOidcProviderRegistrationException(error);
        }
    }

    private void validateReason(
            String reason, TenantOidcProviderUpdateError error) {
        if (reason == null
                || reason.trim().isEmpty()
                || reason.length() > REASON_MAX_LENGTH) {
            throw new TenantOidcProviderUpdateException(error);
        }
    }

    private void validateReason(
            String reason, TenantOidcProviderStatusChangeError error) {
        if (reason == null
                || reason.trim().isEmpty()
                || reason.length() > REASON_MAX_LENGTH) {
            throw new TenantOidcProviderStatusChangeException(error);
        }
    }

}
