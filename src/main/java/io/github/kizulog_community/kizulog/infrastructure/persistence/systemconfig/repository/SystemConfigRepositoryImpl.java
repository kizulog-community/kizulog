package io.github.kizulog_community.kizulog.infrastructure.persistence.systemconfig.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.SystemConfig;
import io.github.kizulog_community.kizulog.domain.systemconfig.port.SystemConfigRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemconfig.entity.SystemConfigEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemconfig.entity.SystemConfigId;
import lombok.RequiredArgsConstructor;

/**
 * システム設定リポジトリ実装クラス（アダプター）
 *
 * <p>ドメイン層のOutput Port（{@link SystemConfigRepository}）の実装クラス。
 * Spring Data JPAを使用してPostgreSQLへのアクセスを提供する。</p>
 *
 * @author Jun Kobayashi
 */
@Repository
@RequiredArgsConstructor
public class SystemConfigRepositoryImpl implements SystemConfigRepository {

    /** JPAリポジトリ. */
    private final SystemConfigJpaRepository jpaRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<SystemConfig> findLatestByKey(String key) {
        return jpaRepository.findLatestByKey(key)
                .map(this::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<SystemConfig> findByKeyAndVersion(String key, OffsetDateTime version) {
        return jpaRepository.findByIdKeyAndIdVersion(key, version)
                .map(this::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SystemConfig> findAllByKey(String key) {
        return jpaRepository.findByIdKey(key)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(SystemConfig systemConfig) {
        jpaRepository.save(toEntity(systemConfig));
    }

    /**
     * EntityをドメインモデルにMapする.
     *
     * @param entity システム設定Entity
     * @return システム設定ドメインモデル
     */
    private SystemConfig toDomain(SystemConfigEntity entity) {
        return new SystemConfig(
                entity.getId().getKey(),
                entity.getId().getVersion(),
                entity.getValue(),
                entity.getCreatedAt(),
                entity.getCreatedBy()
        );
    }

    /**
     * ドメインモデルをEntityにMapする.
     *
     * @param systemConfig システム設定ドメインモデル
     * @return システム設定Entity
     */
    private SystemConfigEntity toEntity(SystemConfig systemConfig) {
        return new SystemConfigEntity(
                new SystemConfigId(
                        systemConfig.getKey(),
                        systemConfig.getVersion()
                ),
                systemConfig.getValue(),
                systemConfig.getCreatedAt(),
                systemConfig.getCreatedBy()
        );
    }
}