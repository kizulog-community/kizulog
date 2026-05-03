package io.github.kizulog_community.kizulog.domain.systemauth.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.kizulog_community.kizulog.domain.port.CryptoPort;
import io.github.kizulog_community.kizulog.domain.systemauth.model.SystemOidcSetting;
import io.github.kizulog_community.kizulog.domain.systemauth.model.SystemOidcSettings;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.SystemConfig;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.SystemConfigService;
import lombok.RequiredArgsConstructor;

/**
 * システム管理OIDC設定サービス
 *
 * <p>system_config（key="OIDC"）から最新のOIDC設定を取得し、
 * clientSecretの復号まで行ってドメインモデルとして返すサービス。</p>
 *
 * <p>認証フロー（DynamicSystemClientRegistrationRepository）から
 * 呼び出され、Spring SecurityのClientRegistration構築の元となる設定を提供する。</p>
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class SystemOidcSettingService {

    /** system_configのOIDCキー */
    private static final String OIDC_CONFIG_KEY = "OIDC";

    /** JSONフィールド：OIDC識別子 */
    private static final String FIELD_ID = "id";

    /** JSONフィールド：Issuer URI */
    private static final String FIELD_URI = "uri";

    /** JSONフィールド：Client ID */
    private static final String FIELD_CLIENT_ID = "clientId";

    /** JSONフィールド：Client Secret（暗号化済） */
    private static final String FIELD_CLIENT_SECRET = "clientSecret";

    /** システム設定サービス */
    private final SystemConfigService systemConfigService;

    /** 暗号化ポート */
    private final CryptoPort cryptoPort;

    /** JSONマッパー */
    private final ObjectMapper objectMapper;

    /**
     * 最新のシステム管理OIDC設定を取得する。
     *
     * <p>system_configのOIDCキーに対応する最新バージョンを取得し、
     * clientSecretを復号した状態のドメインモデルとして返す。</p>
     *
     * @return OIDC設定（バージョン情報付き）。設定が存在しない場合は空のOptional
     */
    @Transactional(readOnly = true)
    public Optional<SystemOidcSettings> findLatest() {
        return systemConfigService.findLatestByKey(OIDC_CONFIG_KEY)
                .map(this::toSystemOidcSettings);
    }

    /**
     * SystemConfigをSystemOidcSettingsに変換する。
     *
     * @param systemConfig システム設定
     * @return OIDC設定群ドメインモデル
     */
    private SystemOidcSettings toSystemOidcSettings(SystemConfig systemConfig) {
        List<Map<String, Object>> rawList = parseJsonArray(systemConfig.getValue());
        List<SystemOidcSetting> settings = new ArrayList<>(rawList.size());
        for (Map<String, Object> raw : rawList) {
            settings.add(toSystemOidcSetting(raw));
        }
        return new SystemOidcSettings(systemConfig.getVersion(), settings);
    }

    /**
     * JSON文字列をMapリストにパースする。
     *
     * @param json JSON文字列
     * @return Mapのリスト
     * @throws RuntimeException JSONパースに失敗した場合
     */
    private List<Map<String, Object>> parseJsonArray(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new RuntimeException("OIDC設定のJSONパースに失敗しました", e);
        }
    }

    /**
     * 1件のOIDC設定MapをSystemOidcSettingに変換する。
     *
     * <p>clientSecretは復号した上でドメインモデルに格納する。</p>
     *
     * @param raw OIDC設定Map
     * @return OIDC設定ドメインモデル
     */
    private SystemOidcSetting toSystemOidcSetting(Map<String, Object> raw) {
        String id = (String) raw.get(FIELD_ID);
        String uri = (String) raw.get(FIELD_URI);
        String clientId = (String) raw.get(FIELD_CLIENT_ID);
        String encryptedClientSecret = (String) raw.get(FIELD_CLIENT_SECRET);
        String clientSecret = cryptoPort.decrypt(encryptedClientSecret);
        return new SystemOidcSetting(id, uri, clientId, clientSecret);
    }

}
