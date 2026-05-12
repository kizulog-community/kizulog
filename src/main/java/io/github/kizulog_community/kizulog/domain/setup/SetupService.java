package io.github.kizulog_community.kizulog.domain.setup;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
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
import io.github.kizulog_community.kizulog.domain.systemconfig.model.SystemConfig;
import io.github.kizulog_community.kizulog.domain.systemconfig.port.SystemConfigRepository;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.OidcProviderStatusValue;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.SystemOidcProviderService;
import io.github.kizulog_community.kizulog.infrastructure.web.setup.OidcSetting;
import io.github.kizulog_community.kizulog.infrastructure.web.setup.SetupSessionData;
import lombok.RequiredArgsConstructor;

/**
 * セットアップサービス
 *
 * <p>OIDCプロバイダー・言語・タイムゾーン設定と
 * システム管理アカウント登録のユースケースを実装する。</p>
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class SetupService {

    /** セットアップ作成者識別子 */
    private static final String CREATED_BY = "system:setup-wizard";

    /** システム設定リポジトリ */
    private final SystemConfigRepository systemConfigRepository;

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

    /** システムOIDCプロバイダーサービス */
    private final SystemOidcProviderService systemOidcProviderService;

    /** JSONマッパー */
    private final ObjectMapper objectMapper;

    /**
     * セットアップ設定を一括保存する。
     *
     * <p>同一トランザクション・同一バージョンで以下を保存する。</p>
     * <ol>
     *   <li>system_oidc_providers + system_oidc_provider_status（OIDC接続情報）</li>
     *   <li>system_config（言語・タイムゾーン）</li>
     *   <li>system_accounts（初期管理者本体）</li>
     *   <li>system_account_status（ACTIVE）</li>
     *   <li>system_account_identities（OIDC認証情報）</li>
     *   <li>system_account_identity_status（ACTIVE）</li>
     *   <li>system_account_roles（SYSTEM_ADMIN）</li>
     *   <li>system_account_role_status（ACTIVE）</li>
     * </ol>
     *
     * @param sessionData セッションに保存されたセットアップデータ
     */
    @Transactional
    public void save(SetupSessionData sessionData) {
        OffsetDateTime version = OffsetDateTime.now(ZoneOffset.UTC);

        // 1. OIDCプロバイダー保存（一覧分すべて）
        for (OidcSetting oidc : sessionData.getOidcSettings()) {
            systemOidcProviderService.register(
                    oidc.getId(),
                    oidc.getId(),                  // displayName: セットアップ時はidをそのまま流用
                    oidc.getUri(),
                    oidc.getClientId(),
                    oidc.getClientSecret(),         // 平文（Service内で暗号化）
                    OidcProviderStatusValue.ENABLED,
                    version,
                    CREATED_BY);
        }

        // 2. 言語・タイムゾーン設定保存
        systemConfigRepository.save(buildLanguageConfig(sessionData, version, CREATED_BY));
        systemConfigRepository.save(buildTimezoneConfig(sessionData, version, CREATED_BY));

        // ID採番
        String accountId = UUID.randomUUID().toString();
        String identityId = UUID.randomUUID().toString();
        String roleId = UUID.randomUUID().toString();

        // 3. アカウント本体
        systemAccountRepository.save(new SystemAccount(
                accountId, version, version, CREATED_BY));

        // 4. アカウントステータス
        systemAccountStatusRepository.save(new SystemAccountStatus(
                accountId, version, AccountStatus.ACTIVE, null, version, CREATED_BY));

        // 5. 認証手段（identity）
        systemAccountIdentityRepository.save(new SystemAccountIdentity(
                identityId, version, accountId,
                sessionData.getAdminIss(),
                sessionData.getAdminAud(),
                sessionData.getAdminSub(),
                version, CREATED_BY));

        // 6. 認証手段ステータス
        systemAccountIdentityStatusRepository.save(new SystemAccountIdentityStatus(
                identityId, version, AccountStatus.ACTIVE, null, version, CREATED_BY));

        // 7. ロール
        systemAccountRoleRepository.save(new SystemAccountRole(
                roleId, version, accountId, SystemRole.SYSTEM_ADMIN,
                version, CREATED_BY));

        // 8. ロールステータス
        systemAccountRoleStatusRepository.save(new SystemAccountRoleStatus(
                roleId, version, AccountStatus.ACTIVE, null, version, CREATED_BY));
    }

    /**
     * 言語設定のSystemConfigを生成する。
     *
     * @param sessionData セッションデータ
     * @param version バージョン
     * @param createdBy 作成者
     * @return 言語設定
     */
    private SystemConfig buildLanguageConfig(
            SetupSessionData sessionData, OffsetDateTime version, String createdBy) {
        try {
            var value = new java.util.LinkedHashMap<String, Object>();
            value.put("DEFAULT", sessionData.getDefaultLanguage().getCode());
            value.put("AVAILABLE", sessionData.getAvailableLanguages().stream()
                    .map(SupportedLanguage::getCode)
                    .toList());

            return new SystemConfig(
                    "LANGUAGE",
                    version,
                    objectMapper.writeValueAsString(value),
                    version,
                    createdBy);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("言語設定のJSON変換に失敗しました", e);
        }
    }

    /**
     * タイムゾーン設定のSystemConfigを生成する。
     *
     * @param sessionData セッションデータ
     * @param version バージョン
     * @param createdBy 作成者
     * @return タイムゾーン設定
     */
    private SystemConfig buildTimezoneConfig(
            SetupSessionData sessionData, OffsetDateTime version, String createdBy) {
        try {
            var value = new java.util.LinkedHashMap<String, Object>();
            value.put("DEFAULT", sessionData.getDefaultTimezone().getZoneId().getId());
            value.put("AVAILABLE", sessionData.getAvailableTimezones().stream()
                    .map(tz -> tz.getZoneId().getId())
                    .toList());

            return new SystemConfig(
                    "TIMEZONE",
                    version,
                    objectMapper.writeValueAsString(value),
                    version,
                    createdBy);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("タイムゾーン設定のJSON変換に失敗しました", e);
        }
    }

}
