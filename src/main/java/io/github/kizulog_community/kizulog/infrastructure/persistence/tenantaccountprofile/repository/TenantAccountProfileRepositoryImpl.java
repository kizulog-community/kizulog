package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccountprofile.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.kizulog_community.kizulog.domain.tenantaccountprofile.model.TenantAccountProfile;
import io.github.kizulog_community.kizulog.domain.tenantaccountprofile.port.TenantAccountProfileRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccountprofile.entity.TenantAccountProfileEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccountprofile.entity.TenantAccountProfileId;
import lombok.RequiredArgsConstructor;

/**
 * テナント利用者プロファイルリポジトリ実装クラス（アダプター）
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class TenantAccountProfileRepositoryImpl implements TenantAccountProfileRepository {

    /** JPAリポジトリ */
    private final TenantAccountProfileJpaRepository jpaRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<TenantAccountProfile> findLatestByIdentityId(String identityId) {
        return jpaRepository.findLatestByIdentityId(identityId)
                .map(this::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(TenantAccountProfile profile) {
        jpaRepository.save(toEntity(profile));
    }

    /**
     * Entityをドメインモデルに変換する。
     *
     * @param entity Entity
     * @return ドメインモデル
     */
    private TenantAccountProfile toDomain(TenantAccountProfileEntity entity) {
        return new TenantAccountProfile(
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
    private TenantAccountProfileEntity toEntity(TenantAccountProfile profile) {
        return new TenantAccountProfileEntity(
                new TenantAccountProfileId(
                        profile.getIdentityId(),
                        profile.getVersion()),
                profile.getClaims(),
                profile.getCreatedAt(),
                profile.getCreatedBy());
    }

}
