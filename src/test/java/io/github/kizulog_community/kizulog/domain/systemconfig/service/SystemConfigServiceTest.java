package io.github.kizulog_community.kizulog.domain.systemconfig.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.kizulog_community.kizulog.domain.systemconfig.model.SystemConfig;
import io.github.kizulog_community.kizulog.domain.systemconfig.port.SystemConfigRepository;

/**
 * SystemConfigServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemConfigServiceTest {

    private SystemConfigRepository repository;
    private SystemConfigService service;

    @BeforeEach
    void setUp() {
        repository = mock(SystemConfigRepository.class);
        service = new SystemConfigService(repository);
    }

    /**
     * テスト用のSystemConfigを生成
     */
    private SystemConfig createSystemConfig(String key, OffsetDateTime version) {
        return new SystemConfig(key, version, "{}", version, "test-user");
    }

    @Test
    @DisplayName("findLatestByKey()はリポジトリの結果をそのまま返す")
    void findLatestByKey_returnsRepositoryResult() {
        OffsetDateTime version = OffsetDateTime.now(ZoneOffset.UTC);
        SystemConfig config = createSystemConfig("OIDC", version);
        when(repository.findLatestByKey("OIDC")).thenReturn(Optional.of(config));

        Optional<SystemConfig> result = service.findLatestByKey("OIDC");

        assertThat(result).isPresent();
        assertThat(result.get().getKey()).isEqualTo("OIDC");
        verify(repository).findLatestByKey("OIDC");
    }

    @Test
    @DisplayName("findLatestByKey()は存在しない場合は空のOptionalを返す")
    void findLatestByKey_notFound_returnsEmpty() {
        when(repository.findLatestByKey("UNKNOWN")).thenReturn(Optional.empty());

        Optional<SystemConfig> result = service.findLatestByKey("UNKNOWN");

        assertThat(result).isEmpty();
        verify(repository).findLatestByKey("UNKNOWN");
    }

    @Test
    @DisplayName("findByKeyAndVersion()はリポジトリの結果をそのまま返す")
    void findByKeyAndVersion_returnsRepositoryResult() {
        OffsetDateTime version = OffsetDateTime.now(ZoneOffset.UTC);
        SystemConfig config = createSystemConfig("OIDC", version);
        when(repository.findByKeyAndVersion("OIDC", version))
                .thenReturn(Optional.of(config));

        Optional<SystemConfig> result = service.findByKeyAndVersion("OIDC", version);

        assertThat(result).isPresent();
        verify(repository).findByKeyAndVersion("OIDC", version);
    }

    @Test
    @DisplayName("findByKeyAndVersion()は存在しない場合は空のOptionalを返す")
    void findByKeyAndVersion_notFound_returnsEmpty() {
        OffsetDateTime version = OffsetDateTime.now(ZoneOffset.UTC);
        when(repository.findByKeyAndVersion("UNKNOWN", version))
                .thenReturn(Optional.empty());

        Optional<SystemConfig> result = service.findByKeyAndVersion("UNKNOWN", version);

        assertThat(result).isEmpty();
        verify(repository).findByKeyAndVersion("UNKNOWN", version);
    }

    @Test
    @DisplayName("findAllByKey()はリポジトリの結果リストをそのまま返す")
    void findAllByKey_returnsRepositoryResult() {
        OffsetDateTime v1 = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime v2 = v1.plusHours(1);
        List<SystemConfig> configs = List.of(
                createSystemConfig("OIDC", v1),
                createSystemConfig("OIDC", v2));
        when(repository.findAllByKey("OIDC")).thenReturn(configs);

        List<SystemConfig> result = service.findAllByKey("OIDC");

        assertThat(result).hasSize(2);
        verify(repository).findAllByKey("OIDC");
    }

    @Test
    @DisplayName("findAllByKey()は存在しない場合は空のリストを返す")
    void findAllByKey_notFound_returnsEmptyList() {
        when(repository.findAllByKey("UNKNOWN")).thenReturn(List.of());

        List<SystemConfig> result = service.findAllByKey("UNKNOWN");

        assertThat(result).isEmpty();
        verify(repository).findAllByKey("UNKNOWN");
    }

    @Test
    @DisplayName("save()はリポジトリのsave()を呼び出す")
    void save_callsRepositorySave() {
        OffsetDateTime version = OffsetDateTime.now(ZoneOffset.UTC);
        SystemConfig config = createSystemConfig("OIDC", version);

        service.save(config);

        verify(repository).save(config);
    }

}