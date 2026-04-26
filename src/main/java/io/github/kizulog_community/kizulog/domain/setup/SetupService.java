package io.github.kizulog_community.kizulog.domain.setup;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.kizulog_community.kizulog.domain.port.CryptoPort;
import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.SystemConfig;
import io.github.kizulog_community.kizulog.domain.systemconfig.port.SystemConfigRepository;
import io.github.kizulog_community.kizulog.infrastructure.web.setup.OidcSetting;
import io.github.kizulog_community.kizulog.infrastructure.web.setup.SetupSessionData;
import lombok.RequiredArgsConstructor;

/**
 * システム設定サービス
 *
 * <p>OIDC・言語・タイムゾーン等のシステム設定の保存・更新ユースケースを実装する。
 * セットアップウィザードおよびAPI経由での設定変更に対応する。</p>
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class SetupService {

    /** システム設定リポジトリ */
    private final SystemConfigRepository systemConfigRepository;

    /** 暗号化ポート */
    private final CryptoPort cryptoPort;

    /** JSONマッパー */
    private final ObjectMapper objectMapper;

    /**
     * セットアップ設定を一括保存する。
     *
     * <p>OIDC・LANGUAGE・TIMEZONEを同一トランザクション・同一バージョンで保存する。</p>
     *
     * @param sessionData セッションに保存されたセットアップデータ
     * @throws RuntimeException JSON変換に失敗した場合
     */
    @Transactional
    public void save(SetupSessionData sessionData) {
        OffsetDateTime version = OffsetDateTime.now(ZoneOffset.UTC);
        String createdBy = "system:setup-wizard";

        systemConfigRepository.save(buildOidcConfig(sessionData, version, createdBy));
        systemConfigRepository.save(buildLanguageConfig(sessionData, version, createdBy));
        systemConfigRepository.save(buildTimezoneConfig(sessionData, version, createdBy));
    }

    /**
     * OIDC設定のSystemConfigを生成する。
     *
     * @param sessionData セットアップセッションデータ
     * @param version バージョン（保存日時）
     * @param createdBy 作成者
     * @return OIDC設定のSystemConfig
     */
    private SystemConfig buildOidcConfig(SetupSessionData sessionData,
            OffsetDateTime version, String createdBy) {
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
     * @param sessionData セットアップセッションデータ
     * @param version バージョン（保存日時）
     * @param createdBy 作成者
     * @return 言語設定のSystemConfig
     */
    private SystemConfig buildLanguageConfig(SetupSessionData sessionData,
            OffsetDateTime version, String createdBy) {
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
     * @param sessionData セットアップセッションデータ
     * @param version バージョン（保存日時）
     * @param createdBy 作成者
     * @return タイムゾーン設定のSystemConfig
     */
    private SystemConfig buildTimezoneConfig(SetupSessionData sessionData,
            OffsetDateTime version, String createdBy) {
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