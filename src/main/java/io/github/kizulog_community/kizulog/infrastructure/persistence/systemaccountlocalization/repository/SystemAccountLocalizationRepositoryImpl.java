package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountlocalization.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.model.SystemAccountLocalization;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.port.SystemAccountLocalizationRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountlocalization.entity.SystemAccountLocalizationEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountlocalization.entity.SystemAccountLocalizationId;
import lombok.RequiredArgsConstructor;

/**
 * システム管理アカウント言語・タイムゾーン設定リポジトリ実装クラス（アダプター）
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class SystemAccountLocalizationRepositoryImpl
        implements SystemAccountLocalizationRepository {

    /** JPAリポジトリ */
    private final SystemAccountLocalizationJpaRepository jpaRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<SystemAccountLocalization> findLatestByAccountId(String accountId) {
        return jpaRepository.findLatestByAccountId(accountId)
                .map(this::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> findAccountIdsUsingLanguage(String languageCode) {
        return jpaRepository.findAccountIdsUsingLanguage(languageCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> findAccountIdsUsingTimezone(String timezoneId) {
        return jpaRepository.findAccountIdsUsingTimezone(timezoneId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(SystemAccountLocalization localization) {
        jpaRepository.save(toEntity(localization));
    }

    /**
     * EntityをドメインモデルにMapする。
     *
     * @param entity Entity
     * @return ドメインモデル
     * @throws IllegalStateException 不正な言語コードが保存されていた場合
     * @throws java.time.zone.ZoneRulesException 不正なタイムゾーンIDが保存されていた場合
     */
    private SystemAccountLocalization toDomain(SystemAccountLocalizationEntity entity) {
        return new SystemAccountLocalization(
                entity.getId().getAccountId(),
                entity.getId().getVersion(),
                resolveLanguage(entity.getLanguageCode()),
                SupportedTimezone.of(entity.getTimezoneId()),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    /**
     * ドメインモデルをEntityにMapする。
     *
     * @param localization ドメインモデル
     * @return Entity
     */
    private SystemAccountLocalizationEntity toEntity(SystemAccountLocalization localization) {
        return new SystemAccountLocalizationEntity(
                new SystemAccountLocalizationId(
                        localization.getAccountId(),
                        localization.getVersion()),
                localization.getLanguage().getCode(),
                localization.getTimezone().getId(),
                localization.getCreatedAt(),
                localization.getCreatedBy());
    }

    /**
     * 言語コード（BCP47タグ）からSupportedLanguageを解決する。
     *
     * @param code 言語コード（例："ja"）
     * @return SupportedLanguage
     * @throws IllegalStateException 該当する言語が見つからない場合（保存値破損）
     */
    private SupportedLanguage resolveLanguage(String code) {
        for (SupportedLanguage lang : SupportedLanguage.values()) {
            if (lang.getCode().equals(code)) {
                return lang;
            }
        }
        throw new IllegalStateException(
                "Unknown language code in system_account_localization: " + code);
    }

}
