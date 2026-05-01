package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import org.springframework.stereotype.Repository;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccount;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountId;
import lombok.RequiredArgsConstructor;

/**
 * システム管理アカウントリポジトリ実装クラス（アダプター）
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
    public void save(SystemAccount systemAccount) {
        jpaRepository.save(toEntity(systemAccount));
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