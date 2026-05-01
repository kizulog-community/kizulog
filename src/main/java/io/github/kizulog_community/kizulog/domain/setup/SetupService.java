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
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccount;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountRole;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountRoleRepository;
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

    /** システム設定リポジトリ */
    private final SystemConfigRepository systemConfigRepository;

    /** システム管理アカウントリポジトリ */
    private final SystemAccountRepository systemAccountRepository;

    /** システム管理アカウントロールリポジトリ */
    private final SystemAccountRoleRepository systemAccountRoleRepository;

    /** システム管理アカウントステータスリポジトリ */
    private final SystemAccountStatusRepository systemAccountStatusRepository;

    /** 暗号化ポート */
    private final CryptoPort cryptoPort;

    /** JSONマッパー */
    private final ObjectMapper objectMapper;

    /**
     * セットアップ設定を一括保存する.
     *
     * <p>同一トランザクション・同一バージョンで以下を保存する。</p>
     * <ul>
     *   <li>system_config（OIDC・言語・タイムゾーン）</li>
     *   <li>system_accounts（初期管理者）</li>
     *   <li>system_account_roles（SYSTEM_ADMIN）</li>
     *   <li>system_account_status（ACTIVE）</li>
     * </ul>
     *
     * @param sessionData セッションに保存されたセットアップデータ
     */
    @Transactional
    public void save(SetupSessionData sessionData) {
        OffsetDateTime version = OffsetDateTime.now(ZoneOffset.UTC);
        String createdBy = "system:setup-wizard";

        // システム設定保存
        systemConfigRepository.save(buildOidcConfig(sessionData, version, createdBy));
        systemConfigRepository.save(buildLanguageConfig(sessionData, version, createdBy));
        systemConfigRepository.save(buildTimezoneConfig(sessionData, version, createdBy));

        // 初期管理者アカウント保存
        String accountId = UUID.randomUUID().toString();

        systemAccountRepository.save(new SystemAccount(
                accountId, version, sessionData.getAdminIss(), sessionData.getAdminAud()
                , sessionData.getAdminSub(), version, createdBy));

        systemAccountRoleRepository.save(new SystemAccountRole(
                accountId, "SYSTEM_ADMIN", version, version, createdBy));

        systemAccountStatusRepository.save(new SystemAccountStatus(
                accountId, version, "ACTIVE", null, version, createdBy));
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