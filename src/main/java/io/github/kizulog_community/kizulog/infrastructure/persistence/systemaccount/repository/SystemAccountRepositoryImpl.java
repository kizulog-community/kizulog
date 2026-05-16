package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccount;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountId;
import lombok.RequiredArgsConstructor;

/**
 * システム管理アカウントリポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class SystemAccountRepositoryImpl implements SystemAccountRepository {

    private final SystemAccountJpaRepository jpaRepository;

    @Override
    public Optional<SystemAccount> findLatestByAccountId(String accountId) {
        return jpaRepository.findLatestByAccountId(accountId)
                .map(this::toDomain);
    }

    @Override
    public List<SystemAccount> findAllLatest() {
        return jpaRepository.findAllLatest().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void save(SystemAccount systemAccount) {
        jpaRepository.save(toEntity(systemAccount));
    }

    /**
     * EntityからDomainModelに変換する。
     */
    private SystemAccount toDomain(SystemAccountEntity entity) {
        return new SystemAccount(
                entity.getId().getAccountId(),
                entity.getId().getVersion(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    /**
     * DomainModelからEntityに変換する。
     */
    private SystemAccountEntity toEntity(SystemAccount domain) {
        return new SystemAccountEntity(
                new SystemAccountId(domain.getAccountId(), domain.getVersion()),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}