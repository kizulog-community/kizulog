package io.github.kizulog_community.kizulog.domain.systemaccountlocalization.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.exception.AccountLocalizationError;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.exception.AccountLocalizationException;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.model.SystemAccountLocalization;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.port.SystemAccountLocalizationRepository;
import lombok.RequiredArgsConstructor;

/**
 * システム管理アカウント言語・タイムゾーン設定サービス
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class SystemAccountLocalizationService {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(SystemAccountLocalizationService.class);

    /** アカウント言語・TZ設定リポジトリ */
    private final SystemAccountLocalizationRepository repository;

    /**
     * accountIdで最新版のアカウント言語・タイムゾーン設定を取得する。
     *
     * @param accountId アカウントID
     * @return 最新版の設定（未登録時は空）
     */
    @Transactional(readOnly = true)
    public Optional<SystemAccountLocalization> getLocalization(String accountId) {
        return repository.findLatestByAccountId(accountId);
    }

    /**
     * アカウントの言語・タイムゾーン設定を新バージョンとして保存する。
     *
     * @param accountId アカウントID
     * @param language 言語
     * @param timezone タイムゾーン
     * @param createdBy 作成者
     * @throws AccountLocalizationException 入力値が不正な場合
     */
    @Transactional
    public void saveLocalization(
            String accountId,
            SupportedLanguage language,
            SupportedTimezone timezone,
            String createdBy) {
        validate(language, timezone);
        OffsetDateTime version = OffsetDateTime.now(ZoneOffset.UTC);
        SystemAccountLocalization localization = new SystemAccountLocalization(
                accountId,
                version,
                language,
                timezone,
                version,
                createdBy);
        repository.save(localization);
        log.info("アカウント言語・タイムゾーン設定を保存しました: "
                + "accountId={}, language={}, timezone={}, createdBy={}",
                accountId, language.getCode(), timezone.getId(), createdBy);
    }

    /**
     * 指定言語を最新版で使用中のaccountIdの一覧を取得する。
     *
     * @param language 言語
     * @return account_idのリスト（一致なしの場合は空リスト）
     */
    @Transactional(readOnly = true)
    public List<String> findAccountIdsUsingLanguage(SupportedLanguage language) {
        if (language == null) {
            throw new AccountLocalizationException(
                    AccountLocalizationError.LANGUAGE_REQUIRED);
        }
        return repository.findAccountIdsUsingLanguage(language.getCode());
    }

    /**
     * 指定タイムゾーンを最新版で使用中のaccountIdの一覧を取得する。
     *
     * @param timezone タイムゾーン
     * @return account_idのリスト（一致なしの場合は空リスト）
     */
    @Transactional(readOnly = true)
    public List<String> findAccountIdsUsingTimezone(SupportedTimezone timezone) {
        if (timezone == null) {
            throw new AccountLocalizationException(
                    AccountLocalizationError.TIMEZONE_REQUIRED);
        }
        return repository.findAccountIdsUsingTimezone(timezone.getId());
    }

    /**
     * 言語・タイムゾーンのnullチェック。
     *
     * @param language 言語
     * @param timezone タイムゾーン
     * @throws AccountLocalizationException null検出時
     */
    private void validate(SupportedLanguage language, SupportedTimezone timezone) {
        if (language == null) {
            throw new AccountLocalizationException(
                    AccountLocalizationError.LANGUAGE_REQUIRED);
        }
        if (timezone == null) {
            throw new AccountLocalizationException(
                    AccountLocalizationError.TIMEZONE_REQUIRED);
        }
    }

}
