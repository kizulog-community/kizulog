package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountStatusId;

/**
 * @author Jun Kobayashi
 */
public interface SystemAccountStatusJpaRepository extends JpaRepository<SystemAccountStatusEntity, SystemAccountStatusId> {}
