package io.github.kizulog_community.kizulog.domain.setup;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

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
import io.github.kizulog_community.kizulog.domain.systemconfig.model.LanguageSetting;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.TimezoneSetting;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.LocalizationSettingService;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.ClaimsMappingTarget;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.OidcProviderStatusValue;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.SystemOidcProviderService;
import io.github.kizulog_community.kizulog.infrastructure.web.setup.OidcSetting;
import io.github.kizulog_community.kizulog.infrastructure.web.setup.SetupSessionData;
import lombok.RequiredArgsConstructor;

/**
 * セットアップサービス
 *
 * <p>OIDCプロバイダー・言語・タイムゾーン設定とシステム管理アカウント登録する。</p>
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class SetupService {

    /** セットアップ作成者識別子 */
    private static final String CREATED_BY = "system:setup-wizard";

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

    /** 言語・タイムゾーン設定サービス */
    private final LocalizationSettingService localizationSettingService;

    /**
     * セットアップ設定を一括保存する。
     *
     * <p>同一トランザクション・同一バージョンで以下を保存する。</p>
     * <ol>
     * <li>system_oidc_providers + system_oidc_provider_status（OIDC接続情報、claimsMapping は OidcSetting から取得）</li>
     * <li>system_config（言語・タイムゾーン）</li>
     * <li>system_accounts（初期管理者本体）</li>
     * <li>system_account_status（ACTIVE）</li>
     * <li>system_account_identities（OIDC認証情報）</li>
     * <li>system_account_identity_status（ACTIVE）</li>
     * <li>system_account_roles（SYSTEM_ADMIN）</li>
     * <li>system_account_role_status（ACTIVE）</li>
     * </ol>
     *
     * @param sessionData セッションに保存されたセットアップデータ
     */
    @Transactional
    public void save(SetupSessionData sessionData) {
        OffsetDateTime version = OffsetDateTime.now(ZoneOffset.UTC);

        // 1. OIDCプロバイダー保存（一覧分すべて）
        for (OidcSetting oidc : sessionData.getOidcSettings()) {
            Map<String, String> claimsMapping = oidc.getClaimsMapping();
            // null/空の場合はデフォルトマッピングを採用（防御的）
            if (claimsMapping == null || claimsMapping.isEmpty()) {
                claimsMapping = ClaimsMappingTarget.defaultMapping();
            }

            systemOidcProviderService.register(
                    oidc.getId(),
                    oidc.getId(),
                    oidc.getUri(),
                    oidc.getClientId(),
                    oidc.getClientSecret(),
                    claimsMapping,
                    OidcProviderStatusValue.ENABLED,
                    version,
                    CREATED_BY);
        }

        // 2. 言語・タイムゾーン設定保存（LocalizationSettingServiceへ委譲）
        LanguageSetting languageSetting = new LanguageSetting(
                sessionData.getDefaultLanguage(),
                sessionData.getAvailableLanguages());
        TimezoneSetting timezoneSetting = new TimezoneSetting(
                sessionData.getDefaultTimezone(),
                sessionData.getAvailableTimezones());
        localizationSettingService.saveBoth(
                languageSetting, timezoneSetting, version, CREATED_BY);

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

}
