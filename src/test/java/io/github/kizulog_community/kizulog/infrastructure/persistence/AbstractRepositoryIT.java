package io.github.kizulog_community.kizulog.infrastructure.persistence;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Repository統合テストの抽象クラス
 *
 * <p>PostgreSQL Testcontainerをstatic共有することで、
 * テストクラス間でコンテナを再利用し高速化。</p>
 *
 * <p>各テストメソッドは{@code @DataJpaTest}のデフォルト動作により
 * 自動的にトランザクションがロールバックされるため、データ独立性は保たれる。</p>
 *
 * @author Jun Kobayashi
 */
@Testcontainers
public abstract class AbstractRepositoryIT {

    /**
     * 全テストクラスで共有するPostgreSQLコンテナ。
     */
    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

}
