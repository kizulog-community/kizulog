package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountId;

/**
 * @author Jun Kobayashi
 */
public interface SystemAccountJpaRepository extends JpaRepository<SystemAccountEntity, SystemAccountId> { }
