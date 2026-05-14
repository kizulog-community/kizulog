package io.github.kizulog_community.kizulog.domain.systemconfig.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.LocalizationConfigError;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.LocalizationConfigException;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.LanguageSetting;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.SystemConfig;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.TimezoneSetting;
import io.github.kizulog_community.kizulog.domain.systemconfig.port.SystemConfigRepository;
import lombok.RequiredArgsConstructor;

/**
 * 言語・タイムゾーン設定サービス
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class LocalizationSettingService {

    /** ロガー */
    private static final Logger log = LoggerFactory.getLogger(LocalizationSettingService.class);

    /** system_configテーブルのキー：言語設定 */
    public static final String KEY_LANGUAGE = "LANGUAGE";

    /** system_configテーブルのキー：タイムゾーン設定 */
    public static final String KEY_TIMEZONE = "TIMEZONE";

    /** JSON要素名：デフォルト値 */
    private static final String FIELD_DEFAULT = "DEFAULT";

    /** JSON要素名：利用可能リスト */
    private static final String FIELD_AVAILABLE = "AVAILABLE";

    /** システム設定リポジトリ */
    private final SystemConfigRepository systemConfigRepository;

    /** JSONマッパー */
    private final ObjectMapper objectMapper;

    /**
     * 言語設定の現在値を取得する。
     *
     * @return 言語設定（未保存時は空）
     * @throws LocalizationConfigException JSONデシリアライズに失敗した場合
     */
    @Transactional(readOnly = true)
    public Optional<LanguageSetting> getLanguageSetting() {
        return systemConfigRepository.findLatestByKey(KEY_LANGUAGE)
                .map(this::deserializeLanguage);
    }

    /**
     * タイムゾーン設定の現在値を取得する。
     *
     * @return タイムゾーン設定（未保存時は空）
     * @throws LocalizationConfigException JSONデシリアライズに失敗した場合
     */
    @Transactional(readOnly = true)
    public Optional<TimezoneSetting> getTimezoneSetting() {
        return systemConfigRepository.findLatestByKey(KEY_TIMEZONE)
                .map(this::deserializeTimezone);
    }

    /**
     * 言語設定を保存する。
     *
     * @param setting 保存対象の言語設定
     * @param createdBy 作成者
     * @throws LocalizationConfigException バリデーション失敗・JSON変換失敗時
     */
    @Transactional
    public void saveLanguageSetting(LanguageSetting setting, String createdBy) {
        validateLanguage(setting);
        OffsetDateTime version = OffsetDateTime.now(ZoneOffset.UTC);
        systemConfigRepository.save(buildLanguageConfig(setting, version, createdBy));
        log.info("言語設定を保存しました: default={}, available={}, createdBy={}",
                setting.getDefaultLanguage(),
                setting.getAvailableLanguages(),
                createdBy);
    }

    /**
     * タイムゾーン設定を保存する。
     *
     * @param setting 保存対象のタイムゾーン設定
     * @param createdBy 作成者
     * @throws LocalizationConfigException バリデーション失敗・JSON変換失敗時
     */
    @Transactional
    public void saveTimezoneSetting(TimezoneSetting setting, String createdBy) {
        validateTimezone(setting);
        OffsetDateTime version = OffsetDateTime.now(ZoneOffset.UTC);
        systemConfigRepository.save(buildTimezoneConfig(setting, version, createdBy));
        log.info("タイムゾーン設定を保存しました: default={}, available={}, createdBy={}",
                setting.getDefaultTimezone().getId(),
                setting.getAvailableTimezones().stream()
                        .map(SupportedTimezone::getId)
                        .toList(),
                createdBy);
    }

    /**
     * 言語設定とタイムゾーン設定を同一バージョンで一括保存する。
     *
     * @param language 言語設定
     * @param timezone タイムゾーン設定
     * @param version バージョン（呼び出し元のトランザクションの基準時刻）
     * @param createdBy 作成者識別子
     * @throws LocalizationConfigException バリデーション失敗・JSON変換失敗時
     */
    @Transactional
    public void saveBoth(LanguageSetting language, TimezoneSetting timezone
    		, OffsetDateTime version, String createdBy) {
        validateLanguage(language);
        validateTimezone(timezone);
        systemConfigRepository.save(buildLanguageConfig(language, version, createdBy));
        systemConfigRepository.save(buildTimezoneConfig(timezone, version, createdBy));
    }

    /**
     * 言語設定をバリデーションする。
     *
     * @param setting 検証対象
     * @throws LocalizationConfigException 不正値検出時
     */
    private void validateLanguage(LanguageSetting setting) {
        if (setting.getDefaultLanguage() == null) {
            throw new LocalizationConfigException(
                    LocalizationConfigError.DEFAULT_LANGUAGE_REQUIRED);
        }
        if (setting.getAvailableLanguages() == null
                || setting.getAvailableLanguages().isEmpty()) {
            throw new LocalizationConfigException(
                    LocalizationConfigError.AVAILABLE_LANGUAGES_EMPTY);
        }
        if (!setting.getAvailableLanguages().contains(setting.getDefaultLanguage())) {
            throw new LocalizationConfigException(
                    LocalizationConfigError.DEFAULT_LANGUAGE_NOT_IN_AVAILABLE);
        }
    }

    /**
     * タイムゾーン設定をバリデーションする。
     *
     * @param setting 検証対象
     * @throws LocalizationConfigException 不正値検出時
     */
    private void validateTimezone(TimezoneSetting setting) {
        if (setting.getDefaultTimezone() == null) {
            throw new LocalizationConfigException(
                    LocalizationConfigError.DEFAULT_TIMEZONE_REQUIRED);
        }
        if (setting.getAvailableTimezones() == null
                || setting.getAvailableTimezones().isEmpty()) {
            throw new LocalizationConfigException(
                    LocalizationConfigError.AVAILABLE_TIMEZONES_EMPTY);
        }
        if (!setting.getAvailableTimezones().contains(setting.getDefaultTimezone())) {
            throw new LocalizationConfigException(
                    LocalizationConfigError.DEFAULT_TIMEZONE_NOT_IN_AVAILABLE);
        }
    }

    /**
     * 言語設定をSystemConfigに変換する。
     *
     * @param setting 言語設定
     * @param version バージョン
     * @param createdBy 作成者
     * @return SystemConfig
     */
    private SystemConfig buildLanguageConfig(
            LanguageSetting setting, OffsetDateTime version, String createdBy) {
        try {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put(FIELD_DEFAULT, setting.getDefaultLanguage().getCode());
            value.put(FIELD_AVAILABLE, setting.getAvailableLanguages().stream()
                    .map(SupportedLanguage::getCode)
                    .toList());
            return new SystemConfig(
                    KEY_LANGUAGE,
                    version,
                    objectMapper.writeValueAsString(value),
                    version,
                    createdBy);
        } catch (JsonProcessingException e) {
            log.error("言語設定のJSONシリアライズに失敗しました", e);
            throw new LocalizationConfigException(
                    LocalizationConfigError.SERIALIZATION_FAILED, e);
        }
    }

    /**
     * タイムゾーン設定をSystemConfigに変換する。
     *
     * @param setting タイムゾーン設定
     * @param version バージョン
     * @param createdBy 作成者
     * @return SystemConfig
     */
    private SystemConfig buildTimezoneConfig(
            TimezoneSetting setting, OffsetDateTime version, String createdBy) {
        try {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put(FIELD_DEFAULT, setting.getDefaultTimezone().getId());
            value.put(FIELD_AVAILABLE, setting.getAvailableTimezones().stream()
                    .map(SupportedTimezone::getId)
                    .toList());
            return new SystemConfig(
                    KEY_TIMEZONE,
                    version,
                    objectMapper.writeValueAsString(value),
                    version,
                    createdBy);
        } catch (JsonProcessingException e) {
            log.error("タイムゾーン設定のJSONシリアライズに失敗しました", e);
            throw new LocalizationConfigException(
                    LocalizationConfigError.SERIALIZATION_FAILED, e);
        }
    }

    /**
     * SystemConfigを言語設定にデシリアライズする。
     *
     * @param config SystemConfig
     * @return 言語設定
     * @throws LocalizationConfigException デシリアライズに失敗した場合
     */
    @SuppressWarnings("unchecked")
    private LanguageSetting deserializeLanguage(SystemConfig config) {
        try {
            Map<String, Object> map = objectMapper.readValue(config.getValue(), Map.class);
            String defaultCode = (String) map.get(FIELD_DEFAULT);
            List<String> availableCodes = (List<String>) map.get(FIELD_AVAILABLE);

            SupportedLanguage defaultLang = (defaultCode == null)
                    ? null
                    : resolveLanguageByCode(defaultCode);
            List<SupportedLanguage> availableLangs = (availableCodes == null)
                    ? List.of()
                    : availableCodes.stream()
                            .map(this::resolveLanguageByCode)
                            .filter(java.util.Objects::nonNull)
                            .toList();
            return new LanguageSetting(defaultLang, availableLangs);
        } catch (LocalizationConfigException e) {
            throw e;
        } catch (Exception e) {
            log.error("言語設定のJSONデシリアライズに失敗しました: value={}", config.getValue(), e);
            throw new LocalizationConfigException(
                    LocalizationConfigError.DESERIALIZATION_FAILED, e);
        }
    }

    /**
     * SystemConfigをタイムゾーン設定にデシリアライズする。
     *
     * @param config SystemConfig
     * @return タイムゾーン設定
     * @throws LocalizationConfigException デシリアライズに失敗した場合
     */
    @SuppressWarnings("unchecked")
    private TimezoneSetting deserializeTimezone(SystemConfig config) {
        try {
            Map<String, Object> map = objectMapper.readValue(config.getValue(), Map.class);
            String defaultId = (String) map.get(FIELD_DEFAULT);
            List<String> availableIds = (List<String>) map.get(FIELD_AVAILABLE);

            SupportedTimezone defaultTz = (defaultId == null)
                    ? null
                    : SupportedTimezone.findById(defaultId).orElse(null);
            List<SupportedTimezone> availableTzs = (availableIds == null)
                    ? List.of()
                    : availableIds.stream()
                            .map(id -> SupportedTimezone.findById(id).orElse(null))
                            .filter(java.util.Objects::nonNull)
                            .toList();
            return new TimezoneSetting(defaultTz, availableTzs);
        } catch (LocalizationConfigException e) {
            throw e;
        } catch (Exception e) {
            log.error("タイムゾーン設定のJSONデシリアライズに失敗しました: value={}",
                    config.getValue(), e);
            throw new LocalizationConfigException(
                    LocalizationConfigError.DESERIALIZATION_FAILED, e);
        }
    }

    /**
     * 言語コード（BCP47タグ）からSupportedLanguageを解決する。
     *
     * @param code 言語コード（例：{@code "ja"}）
     * @return SupportedLanguage、または該当なしの場合null
     */
    private SupportedLanguage resolveLanguageByCode(String code) {
        if (code == null) {
            return null;
        }
        for (SupportedLanguage lang : SupportedLanguage.values()) {
            if (lang.getCode().equals(code)) {
                return lang;
            }
        }
        return null;
    }

}
