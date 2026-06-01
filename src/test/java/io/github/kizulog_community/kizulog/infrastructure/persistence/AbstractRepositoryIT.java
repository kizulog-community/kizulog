package io.github.kizulog_community.kizulog.infrastructure.persistence;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Repository統合テストの抽象クラス
 *
 * @author Jun Kobayashi
 */
public abstract class AbstractRepositoryIT {

    /** 全テストクラスで共有するPostgreSQLコンテナ */
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        // PostgreSQL起動コマンド上書き
        // - fsync=off: Testcontainersデフォルトの起動高速化を維持
        // - max_connections=500: 複数ApplicationContext分のHikariCPプールに耐える数
    	// JVM起動直後に1回だけコンテナを開始
        POSTGRES.setCommand("postgres", "-c", "fsync=off", "-c", "max_connections=500");
        POSTGRES.start();
    }

    /**
     * PostgreSQLコンテナの接続情報と、テスト用HikariCP設定をSpringプロパティに動的注入する。
     *
     * @param registry Spring動的プロパティレジストリ
     */
    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        // DataSource接続情報
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 2);
        registry.add("spring.datasource.hikari.minimum-idle", () -> 0);
    }

}
