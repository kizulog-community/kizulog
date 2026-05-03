package io.github.kizulog_community.kizulog.domain.setup;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.kizulog_community.kizulog.domain.port.CryptoPort;
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
import io.github.kizulog_community.kizulog.infrastructure.web.setup.OidcSetting;
import io.github.kizulog_community.kizulog.infrastructure.web.setup.SetupSessionData;
import lombok.RequiredArgsConstructor;

/**
 * セットアップサービス
 *
 * <p>OIDC・言語・タイムゾーン設定とシステム管理アカウント登録のユースケースを実装する。</p>
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

    /** 暗号化ポート */
    private final CryptoPort cryptoPort;

    /** JSONマッパー */
    private final ObjectMapper objectMapper;

    /**
     * セットアップ設定を一括保存する。
     *
     * <p>同一トランザクション・同一バージョンで以下を保存する。</p>
     * <ol>
     *   <li>system_config（OIDC・言語・タイムゾーン）</li>
     *   <li>system_accounts（初期管理者本体）</li>
     *   <li>system_account_status（ACTIVE）</li>
     *   <li>system_account_identities（OIDC接続情報）</li>
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

        // 1〜3. システム設定保存
        systemConfigRepository.save(buildOidcConfig(sessionData, version, CREATED_BY));
        systemConfigRepository.save(buildLanguageConfig(sessionData, version, CREATED_BY));
        systemConfigRepository.save(buildTimezoneConfig(sessionData, version, CREATED_BY));

        // ID採番
        String accountId = UUID.randomUUID().toString();
        String identityId = UUID.randomUUID().toString();
        String roleId = UUID.randomUUID().toString();

        // 4. アカウント本体
        systemAccountRepository.save(new SystemAccount(
                accountId, version, version, CREATED_BY));

        // 5. アカウントステータス
        systemAccountStatusRepository.save(new SystemAccountStatus(
                accountId, version, AccountStatus.ACTIVE, null, version, CREATED_BY));

        // 6. 認証手段（identity）
        systemAccountIdentityRepository.save(new SystemAccountIdentity(
                identityId, version, accountId,
                sessionData.getAdminIss(),
                sessionData.getAdminAud(),
                sessionData.getAdminSub(),
                version, CREATED_BY));

        // 7. 認証手段ステータス
        systemAccountIdentityStatusRepository.save(new SystemAccountIdentityStatus(
                identityId, version, AccountStatus.ACTIVE, null, version, CREATED_BY));

        // 8. ロール
        systemAccountRoleRepository.save(new SystemAccountRole(
                roleId, version, accountId, SystemRole.SYSTEM_ADMIN,
                version, CREATED_BY));

        // 9. ロールステータス
        systemAccountRoleStatusRepository.save(new SystemAccountRoleStatus(
                roleId, version, AccountStatus.ACTIVE, null, version, CREATED_BY));
    }

    /**
     * OIDC設定のSystemConfigを生成する。
     *
     * @param sessionData セッションデータ
     * @param version バージョン
     * @param createdBy 作成者
     * @return OIDC設定
     */
    private SystemConfig buildOidcConfig(
            SetupSessionData sessionData, OffsetDateTime version, String createdBy) {
        try {
            List<OidcSetting> settings = sessionData.getOidcSettings().stream()
                    .map(s -> {
                        OidcSetting encrypted = new OidcSetting();
                        encrypted.setId(s.getId());
                        encrypted.setUri(s.getUri());
                        encrypted.setClientId(s.getClientId());
                        encrypted.setClientSecret(
                                cryptoPort.encrypt(s.getClientSecret()));
                        return encrypted;
                    })
                    .toList();

            return new SystemConfig(
                    "OIDC",
                    version,
                    objectMapper.writeValueAsString(settings),
                    version,
                    createdBy);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("OIDC設定のJSON変換に失敗しました", e);
        }
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
