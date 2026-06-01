package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountprofile.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.systemaccountprofile.model.SystemAccountProfile;
import io.github.kizulog_community.kizulog.domain.systemaccountprofile.port.SystemAccountProfileRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountprofile.entity.SystemAccountProfileEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountprofile.entity.SystemAccountProfileId;
import lombok.RequiredArgsConstructor;

/**
 * システム管理アカウントプロファイルリポジトリ実装クラス（アダプター）
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class SystemAccountProfileRepositoryImpl
        implements SystemAccountProfileRepository {

    /** JPAリポジトリ */
    private final SystemAccountProfileJpaRepository jpaRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<SystemAccountProfile> findLatestByIdentityId(String identityId) {
        return jpaRepository.findLatestByIdentityId(identityId)
                .map(this::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(SystemAccountProfile profile) {
        jpaRepository.save(toEntity(profile));
    }

    /**
     * Entityをドメインモデルに変換する。
     *
     * @param entity Entity
     * @return ドメインモデル
     */
    private SystemAccountProfile toDomain(SystemAccountProfileEntity entity) {
        return new SystemAccountProfile(
                entity.getId().getIdentityId(),
                entity.getId().getVersion(),
                entity.getClaims(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    /**
     * ドメインモデルをEntityに変換する。
     *
     * @param profile ドメインモデル
     * @return Entity
     */
    private SystemAccountProfileEntity toEntity(SystemAccountProfile profile) {
        return new SystemAccountProfileEntity(
                new SystemAccountProfileId(
                        profile.getIdentityId(),
                        profile.getVersion()),
                profile.getClaims(),
                profile.getCreatedAt(),
                profile.getCreatedBy());
    }

}
