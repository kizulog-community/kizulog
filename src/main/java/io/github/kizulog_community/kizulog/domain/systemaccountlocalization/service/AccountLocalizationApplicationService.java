package io.github.kizulog_community.kizulog.domain.systemaccountlocalization.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountStatusRepository;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.exception.AccountLocalizationError;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.exception.AccountLocalizationException;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.model.SystemAccountLocalization;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.LocalizationConfigException;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.LanguageSetting;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.TimezoneSetting;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.LocalizationSettingService;
import lombok.RequiredArgsConstructor;

/**
 * アカウント言語・タイムゾーン設定アプリケーションサービス
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class AccountLocalizationApplicationService {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(AccountLocalizationApplicationService.class);

    /** アカウントlocalizationサービス */
    private final SystemAccountLocalizationService accountLocalizationService;

    /** システム側localization設定サービス */
    private final LocalizationSettingService localizationSettingService;

    /** アカウントステータスリポジトリ（ACTIVE判定用） */
    private final SystemAccountStatusRepository systemAccountStatusRepository;

    /**
     * アカウントの新規登録時にシステムデフォルトの言語・タイムゾーンを設定する。
     *
     * @param accountId アカウントID
     * @param createdBy 作成者
     * @throws AccountLocalizationException システム側設定未登録時
     */
    @Transactional
    public void createDefaultLocalizationForAccount(String accountId, String createdBy) {
        LanguageSetting langSetting = localizationSettingService.getLanguageSetting()
                .orElseThrow(() -> new AccountLocalizationException(
                        AccountLocalizationError.SYSTEM_LOCALIZATION_NOT_CONFIGURED));
        TimezoneSetting tzSetting = localizationSettingService.getTimezoneSetting()
                .orElseThrow(() -> new AccountLocalizationException(
                        AccountLocalizationError.SYSTEM_LOCALIZATION_NOT_CONFIGURED));

        SupportedLanguage defaultLang = langSetting.getDefaultLanguage();
        SupportedTimezone defaultTz = tzSetting.getDefaultTimezone();
        if (defaultLang == null) {
            throw new AccountLocalizationException(
                    AccountLocalizationError.SYSTEM_LOCALIZATION_NOT_CONFIGURED);
        }
        if (defaultTz == null) {
            throw new AccountLocalizationException(
                    AccountLocalizationError.SYSTEM_LOCALIZATION_NOT_CONFIGURED);
        }

        accountLocalizationService.saveLocalization(
                accountId, defaultLang, defaultTz, createdBy);
        log.info("アカウントにシステムデフォルトの言語・タイムゾーンを適用しました: "
                + "accountId={}, language={}, timezone={}",
                accountId, defaultLang.getCode(), defaultTz.getId());
    }

    /**
     * 本人によるアカウント言語・タイムゾーン設定の保存。
     *
     * @param accountId アカウントID
     * @param language 言語
     * @param timezone タイムゾーン
     * @param createdBy 作成者
     * @throws AccountLocalizationException
     *      引数null、システム側設定未登録、AVAILABLE範囲外
     */
    @Transactional
    public void saveAccountLocalization(
            String accountId,
            SupportedLanguage language,
            SupportedTimezone timezone,
            String createdBy) {
        if (language == null) {
            throw new AccountLocalizationException(
                    AccountLocalizationError.LANGUAGE_REQUIRED);
        }
        if (timezone == null) {
            throw new AccountLocalizationException(
                    AccountLocalizationError.TIMEZONE_REQUIRED);
        }

        LanguageSetting langSetting = localizationSettingService.getLanguageSetting()
                .orElseThrow(() -> new AccountLocalizationException(
                        AccountLocalizationError.SYSTEM_LOCALIZATION_NOT_CONFIGURED));
        TimezoneSetting tzSetting = localizationSettingService.getTimezoneSetting()
                .orElseThrow(() -> new AccountLocalizationException(
                        AccountLocalizationError.SYSTEM_LOCALIZATION_NOT_CONFIGURED));

        if (!langSetting.getAvailableLanguages().contains(language)) {
            throw new AccountLocalizationException(
                    AccountLocalizationError.LANGUAGE_NOT_AVAILABLE);
        }
        if (!tzSetting.getAvailableTimezones().contains(timezone)) {
            throw new AccountLocalizationException(
                    AccountLocalizationError.TIMEZONE_NOT_AVAILABLE);
        }

        accountLocalizationService.saveLocalization(accountId, language, timezone, createdBy);
    }

    /**
     * 指定言語をACTIVEなアカウントで使用中の件数を取得する。
     *
     * @param language 言語
     * @return ACTIVEアカウントでの使用中件数
     */
    @Transactional(readOnly = true)
    public int countActiveAccountsUsingLanguage(SupportedLanguage language) {
        return (int) accountLocalizationService.findAccountIdsUsingLanguage(language)
                .stream()
                .filter(this::isAccountActive)
                .count();
    }

    /**
     * 指定タイムゾーンをACTIVEなアカウントで使用中の件数を取得する。
     *
     * @param timezone タイムゾーン
     * @return ACTIVEアカウントでの使用中件数
     */
    @Transactional(readOnly = true)
    public int countActiveAccountsUsingTimezone(SupportedTimezone timezone) {
        return (int) accountLocalizationService.findAccountIdsUsingTimezone(timezone)
                .stream()
                .filter(this::isAccountActive)
                .count();
    }

    /**
     * アカウントの有効な言語を解決する。
     *
     * <p>解決順序：
     * <ol>
     * <li>アカウントlocalization設定が存在し、システム側AVAILABLE範囲内 → アカウント値</li>
     * <li>アカウントlocalization設定が存在するが範囲外 → システムデフォルト</li>
     * <li>アカウントlocalization設定が未登録 → システムデフォルト</li>
     * <li>システム側設定も未登録 → 空Optional（呼び出し側でフォールバック）</li>
     * </ol>
     * </p>
     *
     * @param accountId アカウントID
     * @return 有効な言語（システム設定も未登録の場合は空）
     */
    @Transactional(readOnly = true)
    public Optional<SupportedLanguage> resolveEffectiveLanguage(String accountId) {
        Optional<LanguageSetting> langSettingOpt = safeGetLanguageSetting();
        Optional<SystemAccountLocalization> accountOpt =
                safeGetAccountLocalization(accountId);

        if (langSettingOpt.isPresent() && accountOpt.isPresent()) {
            SupportedLanguage candidate = accountOpt.get().getLanguage();
            if (langSettingOpt.get().getAvailableLanguages().contains(candidate)) {
                return Optional.of(candidate);
            }
            log.warn("アカウントの言語設定がシステムのAVAILABLE範囲外のためデフォルトへフォールバック: "
                    + "accountId={}, accountLang={}", accountId, candidate.getCode());
        }

        return langSettingOpt.map(LanguageSetting::getDefaultLanguage);
    }

    /**
     * アカウントの有効なタイムゾーンを解決する。
     *
     * <p>解決順序は{@link #resolveEffectiveLanguage(String)}と同様。</p>
     *
     * @param accountId アカウントID
     * @return 有効なタイムゾーン（システム設定も未登録の場合は空）
     */
    @Transactional(readOnly = true)
    public Optional<SupportedTimezone> resolveEffectiveTimezone(String accountId) {
        Optional<TimezoneSetting> tzSettingOpt = safeGetTimezoneSetting();
        Optional<SystemAccountLocalization> accountOpt =
                safeGetAccountLocalization(accountId);

        if (tzSettingOpt.isPresent() && accountOpt.isPresent()) {
            SupportedTimezone candidate = accountOpt.get().getTimezone();
            if (tzSettingOpt.get().getAvailableTimezones().contains(candidate)) {
                return Optional.of(candidate);
            }
            log.warn("アカウントのタイムゾーン設定がシステムのAVAILABLE範囲外のためデフォルトへフォールバック: "
                    + "accountId={}, accountTz={}", accountId, candidate.getId());
        }

        return tzSettingOpt.map(TimezoneSetting::getDefaultTimezone);
    }

    /**
     * アカウントの最新statusがACTIVEか判定する。
     *
     * @param accountId アカウントID
     * @return ACTIVEならtrue、未登録または非ACTIVEならfalse
     */
    private boolean isAccountActive(String accountId) {
        return systemAccountStatusRepository.findLatestByAccountId(accountId)
                .map(SystemAccountStatus::getStatus)
                .filter(s -> s == AccountStatus.ACTIVE)
                .isPresent();
    }

    /**
     * 言語設定を安全に取得する（デシリアライズ失敗時は空Optional）
     */
    private Optional<LanguageSetting> safeGetLanguageSetting() {
        try {
            return localizationSettingService.getLanguageSetting();
        } catch (LocalizationConfigException e) {
            log.warn("システム言語設定の取得に失敗しました: error={}", e.getError(), e);
            return Optional.empty();
        }
    }

    /**
     * タイムゾーン設定を安全に取得する（デシリアライズ失敗時は空Optional）
     */
    private Optional<TimezoneSetting> safeGetTimezoneSetting() {
        try {
            return localizationSettingService.getTimezoneSetting();
        } catch (LocalizationConfigException e) {
            log.warn("システムタイムゾーン設定の取得に失敗しました: error={}", e.getError(), e);
            return Optional.empty();
        }
    }

    /**
     * アカウントlocalization設定を安全に取得する（保存値破損時は空Optional）。
     */
    private Optional<SystemAccountLocalization> safeGetAccountLocalization(String accountId) {
        try {
            return accountLocalizationService.getLocalization(accountId);
        } catch (RuntimeException e) {
            log.warn("アカウント言語・タイムゾーン設定の取得に失敗しました（保存値破損の可能性）: "
                    + "accountId={}, error={}", accountId, e.getMessage(), e);
            return Optional.empty();
        }
    }

}
