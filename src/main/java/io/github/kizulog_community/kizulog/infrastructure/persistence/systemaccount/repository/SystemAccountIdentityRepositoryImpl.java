package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentity;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountIdentityEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountIdentityId;
import lombok.RequiredArgsConstructor;

/**
 * システム管理アカウント認証方法リポジトリ実装
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class SystemAccountIdentityRepositoryImpl
        implements SystemAccountIdentityRepository {

    private final SystemAccountIdentityJpaRepository jpaRepository;

    @Override
    public Optional<SystemAccountIdentity> findLatestByIssAndAudAndSub(
            String iss, String aud, String sub) {
        return jpaRepository.findLatestByIssAndAudAndSub(iss, aud, sub)
                .map(this::toDomain);
    }

    @Override
    public Optional<SystemAccountIdentity> findLatestByIdentityId(String identityId) {
        return jpaRepository.findLatestByIdentityId(identityId)
                .map(this::toDomain);
    }

    @Override
    public List<SystemAccountIdentity> findLatestByAccountId(String accountId) {
        return jpaRepository.findLatestByAccountId(accountId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public int countActiveByIss(String iss) {
        return jpaRepository.countActiveByIss(iss);
    }

    @Override
    public void save(SystemAccountIdentity identity) {
        jpaRepository.save(toEntity(identity));
    }

    private SystemAccountIdentity toDomain(SystemAccountIdentityEntity entity) {
        return new SystemAccountIdentity(
                entity.getId().getIdentityId(),
                entity.getId().getVersion(),
                entity.getAccountId(),
                entity.getIss(),
                entity.getAud(),
                entity.getSub(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    private SystemAccountIdentityEntity toEntity(SystemAccountIdentity domain) {
        return new SystemAccountIdentityEntity(
                new SystemAccountIdentityId(
                        domain.getIdentityId(),
                        domain.getVersion()),
                domain.getAccountId(),
                domain.getIss(),
                domain.getAud(),
                domain.getSub(),
                domain.getCreatedAt(),
                domain.getCreatedBy());
    }

}
