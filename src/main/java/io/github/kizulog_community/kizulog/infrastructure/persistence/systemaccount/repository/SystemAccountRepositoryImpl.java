package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccount;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountId;
import lombok.RequiredArgsConstructor;

/**
 * システム管理アカウントリポジトリ実装クラス（アダプター）
 *
 * <p>ドメイン層のOutput Port（{@link SystemAccountRepository}）の実装クラス。
 * Spring Data JPAを使用してPostgreSQLへのアクセスを提供する。</p>
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class SystemAccountRepositoryImpl implements SystemAccountRepository {

    /** JPAリポジトリ */
    private final SystemAccountJpaRepository jpaRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<SystemAccount> findLatestByIssAndAudAndSub(
            String iss, String aud, String sub) {
        return jpaRepository.findLatestByIssAndAudAndSub(iss, aud, sub)
                .map(this::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(SystemAccount systemAccount) {
        jpaRepository.save(toEntity(systemAccount));
    }

    /**
     * Entityをドメインモデルにマップする。
     *
     * @param entity Entity
     * @return ドメインモデル
     */
    private SystemAccount toDomain(SystemAccountEntity entity) {
        return new SystemAccount(
                entity.getId().getAccountId(),
                entity.getId().getVersion(),
                entity.getIss(),
                entity.getAud(),
                entity.getSub(),
                entity.getCreatedAt(),
                entity.getCreatedBy()
        );
    }

    /**
     * ドメインモデルをEntityにマップする。
     *
     * @param systemAccount ドメインモデル
     * @return Entity
     */
    private SystemAccountEntity toEntity(SystemAccount systemAccount) {
        return new SystemAccountEntity(
                new SystemAccountId(
                        systemAccount.getAccountId(),
                        systemAccount.getVersion()
                ),
                systemAccount.getIss(),
                systemAccount.getAud(),
                systemAccount.getSub(),
                systemAccount.getCreatedAt(),
                systemAccount.getCreatedBy()
        );
    }

}
