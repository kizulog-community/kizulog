package io.github.kizulog_community.kizulog.domain.systemoidc.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kizulog_community.kizulog.domain.port.CryptoPort;
import io.github.kizulog_community.kizulog.domain.systemoidc.exception.OidcProviderError;
import io.github.kizulog_community.kizulog.domain.systemoidc.exception.OidcProviderException;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.DecryptedOidcProvider;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.EnabledProviderView;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.OidcProviderStatusValue;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.ProviderWithStatus;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProvider;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProviderStatus;
import io.github.kizulog_community.kizulog.domain.systemoidc.port.SystemOidcProviderRepository;
import io.github.kizulog_community.kizulog.domain.systemoidc.port.SystemOidcProviderStatusRepository;
import lombok.RequiredArgsConstructor;

/**
 * システムOIDCプロバイダーサービス
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class SystemOidcProviderService {

    /** provider_id バリデーションパターン: 半角小文字英数字とハイフン、1-32文字 */
    private static final Pattern PROVIDER_ID_PATTERN = Pattern.compile("^[a-z0-9-]{1,32}$");

    /** OIDCプロバイダーリポジトリ */
    private final SystemOidcProviderRepository providerRepository;

    /** OIDCプロバイダーステータスリポジトリ */
    private final SystemOidcProviderStatusRepository statusRepository;

    /** 暗号化ポート */
    private final CryptoPort cryptoPort;

    /**
     * 認証フロー用のOIDCプロバイダーを取得する。
     *
     * @param providerId プロバイダーID
     * @return 復号済みOIDCプロバイダー
     */
    @Transactional(readOnly = true)
    public Optional<DecryptedOidcProvider> findEnabledForAuthentication(String providerId) {
        Optional<SystemOidcProviderStatus> statusOpt =
                statusRepository.findLatestByProviderId(providerId);
        if (statusOpt.isEmpty() || !statusOpt.get().getStatus().isUsable()) {
            return Optional.empty();
        }

        return providerRepository.findLatestByProviderId(providerId)
                .map(this::toDecrypted);
    }

    /**
     * 新規provider_idのバリデーションを実施する。
     *
     * @throws OidcProviderException バリデーション失敗時
     */
    @Transactional(readOnly = true)
    public void validateNewProviderId(String providerId) {
        if (providerId == null || !PROVIDER_ID_PATTERN.matcher(providerId).matches()) {
            throw new OidcProviderException(OidcProviderError.PROVIDER_ID_INVALID_FORMAT);
        }
        if (providerRepository.existsByProviderId(providerId)) {
            throw new OidcProviderException(OidcProviderError.PROVIDER_ID_DUPLICATE);
        }
    }

    /**
     * 新規OIDCプロバイダーを保存する（プロバイダー本体 + ステータスをセットで保存）
     */
    @Transactional
    public void register(
            String providerId,
            String displayName,
            String uri,
            String clientId,
            String plainClientSecret,
            OidcProviderStatusValue status,
            OffsetDateTime version,
            String createdBy) {
        String encryptedSecret = cryptoPort.encrypt(plainClientSecret);
        String normalizedUri = normalizeUri(uri);

        providerRepository.save(new SystemOidcProvider(
                providerId,
                version,
                displayName,
                normalizedUri,
                clientId,
                encryptedSecret,
                version,
                createdBy));

        statusRepository.save(new SystemOidcProviderStatus(
                providerId,
                version,
                status,
                null,
                version,
                createdBy));
    }

    /**
     * OIDC issuer URIを比較可能な正規形に変換する。
     *
     * <p>正規化ルール:
     * <ul>
     * <li>前後の空白を除去（フォーム入力ミス対策）</li>
     * <li>末尾スラッシュを1つだけ除去（OIDCで一般的な不一致原因）</li>
     * </ul>
     *
     * @param uri 入力URI
     * @return 正規化済みURI、nullの場合はnullを返す
     */
    static String normalizeUri(String uri) {
        if (uri == null) {
            return null;
        }
        String trimmed = uri.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    /**
     * バリデーション付きでOIDCプロバイダーを登録する。
     *
     * @throws OidcProviderException バリデーション失敗時
     */
    @Transactional
    public void registerWithValidation(
            String providerId,
            String displayName,
            String uri,
            String clientId,
            String plainClientSecret,
            String createdBy) {
        validateNewProviderId(providerId);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        register(providerId, displayName, uri, clientId, plainClientSecret,
                OidcProviderStatusValue.ENABLED, now, createdBy);
    }

    /**
     * 既存provider_idのプロバイダーで、編集可能項目を更新する。
     *
     * @throws OidcProviderException プロバイダーが見つからない場合
     */
    @Transactional
    public void updateMutableFields(
            String providerId,
            String displayName,
            String clientId,
            String plainClientSecret,
            String updatedBy) {
        SystemOidcProvider current = providerRepository.findLatestByProviderId(providerId)
                .orElseThrow(() ->
                        new OidcProviderException(OidcProviderError.PROVIDER_NOT_FOUND));

        String encryptedSecret = (plainClientSecret == null || plainClientSecret.isEmpty())
                ? current.getClientSecret()
                : cryptoPort.encrypt(plainClientSecret);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        providerRepository.save(new SystemOidcProvider(
                providerId,
                now,
                displayName,
                current.getUri(),
                clientId,
                encryptedSecret,
                now,
                updatedBy));
    }

    /**
     * provider_idの暗号化済みclient_secretを取得する。
     */
    @Transactional(readOnly = true)
    public Optional<String> findEncryptedClientSecret(String providerId) {
        return providerRepository.findLatestByProviderId(providerId)
                .map(SystemOidcProvider::getClientSecret);
    }

    /**
     * OIDCプロバイダーを有効化する。
     *
     * <p>事前条件:</p>
     * <ul>
     * <li>プロバイダーが存在する</li>
     * <li>現在のステータスがDISABLEDである（既にENABLEDならエラー）</li>
     * </ul>
     *
     * @param providerId プロバイダーID
     * @param reason 状態変更の理由（必須）
     * @param updatedBy 更新者
     * @throws OidcProviderException プロバイダーが見つからない / 既にENABLED
     */
    @Transactional
    public void enable(String providerId, String reason, String updatedBy) {
        SystemOidcProviderStatus current = statusRepository.findLatestByProviderId(providerId)
                .orElseThrow(() ->
                        new OidcProviderException(OidcProviderError.PROVIDER_NOT_FOUND));

        if (current.getStatus() == OidcProviderStatusValue.ENABLED) {
            throw new OidcProviderException(OidcProviderError.ALREADY_ENABLED);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        statusRepository.save(new SystemOidcProviderStatus(
                providerId,
                now,
                OidcProviderStatusValue.ENABLED,
                reason,
                now,
                updatedBy));
    }

    /**
     * OIDCプロバイダーを無効化する。
     *
     * <p>事前条件:</p>
     * <ul>
     * <li>プロバイダーが存在する</li>
     * <li>現在のステータスがENABLEDである（既にDISABLEDならエラー）</li>
     * <li>無効化後も最低1つのENABLEDなプロバイダーが残る</li>
     * </ul>
     *
     * @param providerId プロバイダーID
     * @param reason 状態変更の理由（必須）
     * @param updatedBy 更新者
     * @throws OidcProviderException プロバイダーが見つからない / 既にDISABLED / 最後の有効プロバイダー
     */
    @Transactional
    public void disable(String providerId, String reason, String updatedBy) {
        SystemOidcProviderStatus current = statusRepository.findLatestByProviderId(providerId)
                .orElseThrow(() ->
                        new OidcProviderException(OidcProviderError.PROVIDER_NOT_FOUND));

        if (current.getStatus() == OidcProviderStatusValue.DISABLED) {
            throw new OidcProviderException(OidcProviderError.ALREADY_DISABLED);
        }

        // 「最低1つENABLED」制約: このプロバイダーを除いた他のENABLED一覧が1件以上あること
        if (countOtherEnabled(providerId) == 0) {
            throw new OidcProviderException(OidcProviderError.LAST_ENABLED_REQUIRED);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        statusRepository.save(new SystemOidcProviderStatus(
                providerId,
                now,
                OidcProviderStatusValue.DISABLED,
                reason,
                now,
                updatedBy));
    }

    /**
     * 指定プロバイダーを除いて、現在ENABLEDな他のプロバイダー数を取得する。
     *
     * @param excludingProviderId 除外するプロバイダーID（通常は無効化対象）
     * @return ENABLED状態の他のプロバイダー数
     */
    @Transactional(readOnly = true)
    public int countOtherEnabled(String excludingProviderId) {
        List<String> enabledIds =
                statusRepository.findProviderIdsByLatestStatus(OidcProviderStatusValue.ENABLED);
        return (int) enabledIds.stream()
                .filter(id -> !id.equals(excludingProviderId))
                .count();
    }

    /**
     * ログイン画面表示用に、ENABLEDな全プロバイダーをビューで取得する。
     *
     * @return ENABLEDなプロバイダーのViewリスト
     */
    @Transactional(readOnly = true)
    public List<EnabledProviderView> listEnabledForLogin() {
        List<String> enabledProviderIds =
                statusRepository.findProviderIdsByLatestStatus(OidcProviderStatusValue.ENABLED);

        return enabledProviderIds.stream()
                .map(providerRepository::findLatestByProviderId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(p -> new EnabledProviderView(p.getProviderId(), p.getDisplayName()))
                .sorted(Comparator.comparing(
                        EnabledProviderView::getDisplayName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    /**
     * 管理画面用に全プロバイダーをステータス付きで取得する。
     */
    @Transactional(readOnly = true)
    public List<ProviderWithStatus> listAll() {
        List<SystemOidcProvider> providers = providerRepository.findAllLatest();
        Map<String, SystemOidcProviderStatus> statusMap = providers.stream()
                .map(p -> statusRepository.findLatestByProviderId(p.getProviderId()).orElse(null))
                .filter(s -> s != null)
                .collect(Collectors.toMap(SystemOidcProviderStatus::getProviderId, s -> s));

        return providers.stream()
                .map(p -> new ProviderWithStatus(p, statusMap.get(p.getProviderId())))
                .sorted(Comparator
                        .<ProviderWithStatus, Integer>comparing(pws ->
                                pws.getStatus() != null
                                        && pws.getStatus().getStatus() == OidcProviderStatusValue.ENABLED
                                        ? 0 : 1)
                        .thenComparing(pws -> pws.getProvider().getDisplayName(),
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    /**
     * 詳細画面用に1件のプロバイダーをステータス付きで取得する。
     */
    @Transactional(readOnly = true)
    public Optional<ProviderWithStatus> findDetailByProviderId(String providerId) {
        Optional<SystemOidcProvider> providerOpt =
                providerRepository.findLatestByProviderId(providerId);
        if (providerOpt.isEmpty()) {
            return Optional.empty();
        }
        SystemOidcProviderStatus status =
                statusRepository.findLatestByProviderId(providerId).orElse(null);
        return Optional.of(new ProviderWithStatus(providerOpt.get(), status));
    }

    private DecryptedOidcProvider toDecrypted(SystemOidcProvider provider) {
        return new DecryptedOidcProvider(
                provider.getProviderId(),
                provider.getVersion(),
                provider.getDisplayName(),
                provider.getUri(),
                provider.getClientId(),
                cryptoPort.decrypt(provider.getClientSecret()));
    }

}
