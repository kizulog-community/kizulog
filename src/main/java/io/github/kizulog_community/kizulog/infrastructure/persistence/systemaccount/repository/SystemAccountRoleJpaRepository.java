package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleId;

/**
 * @author Jun Kobayashi
 */
public interface SystemAccountRoleJpaRepository extends JpaRepository<SystemAccountRoleEntity, SystemAccountRoleId> {}
