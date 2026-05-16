package io.github.kizulog_community.kizulog.domain.tenant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantHostError;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantHostException;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantRegistrationError;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantRegistrationException;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantStatusChangeError;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantStatusChangeException;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantUpdateError;
import io.github.kizulog_community.kizulog.domain.tenant.exception.TenantUpdateException;
import io.github.kizulog_community.kizulog.domain.tenant.model.Tenant;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantDetailView;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHost;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostStatus;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostStatusValue;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantListItemView;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantStatus;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantStatusValue;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantHostRepository;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantHostStatusRepository;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantRepository;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantStatusRepository;

/**
 * TenantManagementServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantManagementServiceTest {

    /** テストで使う固定slug */
    private static final String FIXED_SLUG = "abc123def456ghi789jkl";

    /** テストで使う固定操作者ID */
    private static final String OPERATOR_ID = "operator-account-id";

    private TenantRepository tenantRepository;
    private TenantStatusRepository tenantStatusRepository;
    private TenantHostRepository tenantHostRepository;
    private TenantHostStatusRepository tenantHostStatusRepository;
    private SlugGenerator slugGenerator;
    private HostNormalizer hostNormalizer;
    private TenantManagementService service;

    @BeforeEach
    void setUp() {
        tenantRepository = mock(TenantRepository.class);
        tenantStatusRepository = mock(TenantStatusRepository.class);
        tenantHostRepository = mock(TenantHostRepository.class);
        tenantHostStatusRepository = mock(TenantHostStatusRepository.class);
        slugGenerator = mock(SlugGenerator.class);
        hostNormalizer = new HostNormalizer(); // 実物を使用

        service = new TenantManagementService(
                tenantRepository,
                tenantStatusRepository,
                tenantHostRepository,
                tenantHostStatusRepository,
                slugGenerator,
                hostNormalizer);

        // slugGenerator のデフォルト動作: 固定slugを返す
        when(slugGenerator.generate()).thenReturn(FIXED_SLUG);
        // tenantRepository.existsBySlug: デフォルトでは衝突なし
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
    }

    @Test
    @DisplayName("listAllTenants: テナントが存在しない場合は空リストを返す")
    void listAllTenants_returnsEmptyList_whenNoTenants() {
        when(tenantRepository.findAllLatest()).thenReturn(List.of());

        List<TenantListItemView> result = service.listAllTenants();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("listAllTenants: ACTIVE→SUSPENDED→INACTIVEの順、同status内では作成日時降順でソートする")
    void listAllTenants_sortsByStatusThenCreatedAtDesc() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime t2 = OffsetDateTime.of(2026, 5, 2, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime t3 = OffsetDateTime.of(2026, 5, 3, 0, 0, 0, 0, ZoneOffset.UTC);

        Tenant inactiveOld = new Tenant("t1", t1, "Inactive Old", "slug1", t1, "creator");
        Tenant activeNew = new Tenant("t2", t2, "Active New", "slug2", t2, "creator");
        Tenant suspendedNewer = new Tenant("t3", t3, "Suspended Newer", "slug3", t3, "creator");

        when(tenantRepository.findAllLatest())
                .thenReturn(List.of(inactiveOld, activeNew, suspendedNewer));
        when(tenantStatusRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(statusOf("t1", t1, TenantStatusValue.INACTIVE)));
        when(tenantStatusRepository.findLatestByTenantId("t2"))
                .thenReturn(Optional.of(statusOf("t2", t2, TenantStatusValue.ACTIVE)));
        when(tenantStatusRepository.findLatestByTenantId("t3"))
                .thenReturn(Optional.of(statusOf("t3", t3, TenantStatusValue.SUSPENDED)));
        when(tenantHostRepository.findAllLatestByTenantId(anyString()))
                .thenReturn(List.of());

        List<TenantListItemView> result = service.listAllTenants();

        assertThat(result).extracting(TenantListItemView::getTenantId)
                .containsExactly("t2", "t3", "t1");
    }

    @Test
    @DisplayName("listAllTenants: ACTIVE状態のhostのみactiveHostsに含める")
    void listAllTenants_includesActiveHostsOnly() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant tenant = new Tenant("t1", t1, "Tenant", "slug1", t1, "creator");

        TenantHost host1 = new TenantHost("t1", "active.example.com", t1, t1, "creator");
        TenantHost host2 = new TenantHost("t1", "inactive.example.com", t1, t1, "creator");

        when(tenantRepository.findAllLatest()).thenReturn(List.of(tenant));
        when(tenantStatusRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(statusOf("t1", t1, TenantStatusValue.ACTIVE)));
        when(tenantHostRepository.findAllLatestByTenantId("t1"))
                .thenReturn(List.of(host1, host2));
        when(tenantHostStatusRepository.findLatestByTenantIdAndHost("t1", "active.example.com"))
                .thenReturn(Optional.of(hostStatusOf("t1", "active.example.com", t1,
                        TenantHostStatusValue.ACTIVE)));
        when(tenantHostStatusRepository.findLatestByTenantIdAndHost("t1", "inactive.example.com"))
                .thenReturn(Optional.of(hostStatusOf("t1", "inactive.example.com", t1,
                        TenantHostStatusValue.INACTIVE)));

        List<TenantListItemView> result = service.listAllTenants();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActiveHosts())
                .containsExactly("active.example.com");
        assertThat(result.get(0).getTotalHostCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("findTenantDetail: 存在するテナントの詳細を返す")
    void findTenantDetail_returnsView_whenTenantExists() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant tenant = new Tenant("t1", t1, "Tenant", "slug1", t1, "creator");

        when(tenantRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(tenant));
        when(tenantStatusRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(statusOf("t1", t1, TenantStatusValue.ACTIVE)));
        when(tenantStatusRepository.findAllByTenantIdOrderByVersionDesc("t1"))
                .thenReturn(List.of(statusOf("t1", t1, TenantStatusValue.ACTIVE)));
        when(tenantHostRepository.findAllLatestByTenantId("t1"))
                .thenReturn(List.of());

        Optional<TenantDetailView> result = service.findTenantDetail("t1");

        assertThat(result).isPresent();
        assertThat(result.get().getTenantId()).isEqualTo("t1");
        assertThat(result.get().getName()).isEqualTo("Tenant");
        assertThat(result.get().getSlug()).isEqualTo("slug1");
        assertThat(result.get().getCurrentStatus()).isEqualTo(TenantStatusValue.ACTIVE);
    }

    @Test
    @DisplayName("findTenantDetail: テナントが存在しない場合は空のOptionalを返す")
    void findTenantDetail_returnsEmpty_whenTenantNotFound() {
        when(tenantRepository.findLatestByTenantId("missing"))
                .thenReturn(Optional.empty());

        Optional<TenantDetailView> result = service.findTenantDetail("missing");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findTenantDetail: ステータス履歴を新しい順に含む")
    void findTenantDetail_includesStatusHistory() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime t2 = OffsetDateTime.of(2026, 5, 2, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant tenant = new Tenant("t1", t2, "Tenant", "slug1", t1, "creator");

        when(tenantRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(tenant));
        when(tenantStatusRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(statusOf("t1", t2, TenantStatusValue.SUSPENDED)));
        when(tenantStatusRepository.findAllByTenantIdOrderByVersionDesc("t1"))
                .thenReturn(List.of(
                        statusOf("t1", t2, TenantStatusValue.SUSPENDED),
                        statusOf("t1", t1, TenantStatusValue.ACTIVE)));
        when(tenantHostRepository.findAllLatestByTenantId("t1"))
                .thenReturn(List.of());

        Optional<TenantDetailView> result = service.findTenantDetail("t1");

        assertThat(result).isPresent();
        assertThat(result.get().getStatusHistory()).hasSize(2);
        assertThat(result.get().getStatusHistory().get(0).getStatus())
                .isEqualTo(TenantStatusValue.SUSPENDED);
        assertThat(result.get().getStatusHistory().get(1).getStatus())
                .isEqualTo(TenantStatusValue.ACTIVE);
    }

    @Test
    @DisplayName("findTenantBySlug: 存在するslugでテナントを返す")
    void findTenantBySlug_returnsTenant_whenExists() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant tenant = new Tenant("t1", t1, "Tenant", FIXED_SLUG, t1, "creator");
        when(tenantRepository.findLatestBySlug(FIXED_SLUG))
                .thenReturn(Optional.of(tenant));

        Optional<Tenant> result = service.findTenantBySlug(FIXED_SLUG);

        assertThat(result).isPresent();
        assertThat(result.get().getTenantId()).isEqualTo("t1");
    }

    @Test
    @DisplayName("findTenantBySlug: 存在しないslugで空のOptionalを返す")
    void findTenantBySlug_returnsEmpty_whenNotFound() {
        when(tenantRepository.findLatestBySlug("missing-slug"))
                .thenReturn(Optional.empty());

        Optional<Tenant> result = service.findTenantBySlug("missing-slug");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("registerTenant: 正常入力で4種類のエンティティを全て保存する")
    void registerTenant_savesAllEntities_whenValidInput() {
        String tenantId = service.registerTenant(
                "テナント名", List.of("example.com"), "登録理由", OPERATOR_ID);

        assertThat(tenantId).isNotBlank();
        verify(tenantRepository, times(1)).save(any(Tenant.class));
        verify(tenantStatusRepository, times(1)).save(any(TenantStatus.class));
        verify(tenantHostRepository, times(1)).save(any(TenantHost.class));
        verify(tenantHostStatusRepository, times(1)).save(any(TenantHostStatus.class));
    }

    @Test
    @DisplayName("registerTenant: 複数hostを全て保存する")
    void registerTenant_savesMultipleHosts() {
        service.registerTenant(
                "テナント",
                List.of("a.example.com", "b.example.com", "c.example.com"),
                "登録",
                OPERATOR_ID);

        verify(tenantHostRepository, times(3)).save(any(TenantHost.class));
        verify(tenantHostStatusRepository, times(3)).save(any(TenantHostStatus.class));
    }

    @Test
    @DisplayName("registerTenant: name null で NAME_INVALID")
    void registerTenant_throws_whenNameNull() {
        assertThatThrownBy(() -> service.registerTenant(
                null, List.of("example.com"), "reason", OPERATOR_ID))
                .isInstanceOf(TenantRegistrationException.class)
                .matches(e -> ((TenantRegistrationException) e).getError()
                        == TenantRegistrationError.NAME_INVALID);
    }

    @Test
    @DisplayName("registerTenant: name 空文字 で NAME_INVALID")
    void registerTenant_throws_whenNameEmpty() {
        assertThatThrownBy(() -> service.registerTenant(
                "  ", List.of("example.com"), "reason", OPERATOR_ID))
                .isInstanceOf(TenantRegistrationException.class)
                .matches(e -> ((TenantRegistrationException) e).getError()
                        == TenantRegistrationError.NAME_INVALID);
    }

    @Test
    @DisplayName("registerTenant: name 101文字 で NAME_INVALID")
    void registerTenant_throws_whenNameTooLong() {
        String tooLong = "a".repeat(101);

        assertThatThrownBy(() -> service.registerTenant(
                tooLong, List.of("example.com"), "reason", OPERATOR_ID))
                .isInstanceOf(TenantRegistrationException.class)
                .matches(e -> ((TenantRegistrationException) e).getError()
                        == TenantRegistrationError.NAME_INVALID);
    }

    @Test
    @DisplayName("registerTenant: hosts null で HOST_EMPTY")
    void registerTenant_throws_whenHostsNull() {
        assertThatThrownBy(() -> service.registerTenant(
                "name", null, "reason", OPERATOR_ID))
                .isInstanceOf(TenantRegistrationException.class)
                .matches(e -> ((TenantRegistrationException) e).getError()
                        == TenantRegistrationError.HOST_EMPTY);
    }

    @Test
    @DisplayName("registerTenant: hosts 空リスト で HOST_EMPTY")
    void registerTenant_throws_whenHostsEmpty() {
        assertThatThrownBy(() -> service.registerTenant(
                "name", List.of(), "reason", OPERATOR_ID))
                .isInstanceOf(TenantRegistrationException.class)
                .matches(e -> ((TenantRegistrationException) e).getError()
                        == TenantRegistrationError.HOST_EMPTY);
    }

    @Test
    @DisplayName("registerTenant: host形式不正で HOST_INVALID")
    void registerTenant_throws_whenHostInvalid() {
        assertThatThrownBy(() -> service.registerTenant(
                "name",
                List.of("https://example.com"), // schemeが含まれる
                "reason",
                OPERATOR_ID))
                .isInstanceOf(TenantRegistrationException.class)
                .matches(e -> ((TenantRegistrationException) e).getError()
                        == TenantRegistrationError.HOST_INVALID);
    }

    @Test
    @DisplayName("registerTenant: 同一リクエスト内のhost重複で HOST_DUPLICATE_IN_REQUEST")
    void registerTenant_throws_whenDuplicateHosts() {
        assertThatThrownBy(() -> service.registerTenant(
                "name",
                List.of("example.com", "example.com"),
                "reason",
                OPERATOR_ID))
                .isInstanceOf(TenantRegistrationException.class)
                .matches(e -> ((TenantRegistrationException) e).getError()
                        == TenantRegistrationError.HOST_DUPLICATE_IN_REQUEST);
    }

    @Test
    @DisplayName("registerTenant: reason null で REASON_INVALID")
    void registerTenant_throws_whenReasonNull() {
        assertThatThrownBy(() -> service.registerTenant(
                "name", List.of("example.com"), null, OPERATOR_ID))
                .isInstanceOf(TenantRegistrationException.class)
                .matches(e -> ((TenantRegistrationException) e).getError()
                        == TenantRegistrationError.REASON_INVALID);
    }

    @Test
    @DisplayName("registerTenant: reason 1001文字で REASON_INVALID")
    void registerTenant_throws_whenReasonTooLong() {
        String tooLong = "a".repeat(1001);

        assertThatThrownBy(() -> service.registerTenant(
                "name", List.of("example.com"), tooLong, OPERATOR_ID))
                .isInstanceOf(TenantRegistrationException.class)
                .matches(e -> ((TenantRegistrationException) e).getError()
                        == TenantRegistrationError.REASON_INVALID);
    }

    @Test
    @DisplayName("registerTenant: slug衝突時に再試行して別のslugで保存する")
    void registerTenant_retriesOnSlugCollision() {
        when(slugGenerator.generate())
                .thenReturn("conflict-slug-aaaaaaa")
                .thenReturn("conflict-slug-bbbbbbb")
                .thenReturn("success-slug-ccccccc");
        when(tenantRepository.existsBySlug("conflict-slug-aaaaaaa")).thenReturn(true);
        when(tenantRepository.existsBySlug("conflict-slug-bbbbbbb")).thenReturn(true);
        when(tenantRepository.existsBySlug("success-slug-ccccccc")).thenReturn(false);

        service.registerTenant(
                "name", List.of("example.com"), "reason", OPERATOR_ID);

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getSlug()).isEqualTo("success-slug-ccccccc");
        verify(slugGenerator, times(3)).generate();
    }

    @Test
    @DisplayName("registerTenant: slug衝突が最大試行回数で解消されない場合は SLUG_GENERATION_FAILED")
    void registerTenant_throws_whenSlugGenerationFails() {
        when(tenantRepository.existsBySlug(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.registerTenant(
                "name", List.of("example.com"), "reason", OPERATOR_ID))
                .isInstanceOf(TenantRegistrationException.class)
                .matches(e -> ((TenantRegistrationException) e).getError()
                        == TenantRegistrationError.SLUG_GENERATION_FAILED);
    }

    @Test
    @DisplayName("updateTenantName: 正常入力で新しいversionレコードを保存する")
    void updateTenantName_savesNewVersion() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant existing = new Tenant("t1", t1, "Old Name", FIXED_SLUG, t1, "creator");
        when(tenantRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(existing));

        service.updateTenantName("t1", "New Name", "name change reason", OPERATOR_ID);

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("New Name");
        assertThat(captor.getValue().getSlug()).isEqualTo(FIXED_SLUG); // slug保持
        assertThat(captor.getValue().getCreatedBy())
                .startsWith("system:tenant-name-update:")
                .endsWith(OPERATOR_ID);
    }

    @Test
    @DisplayName("updateTenantName: テナント未存在で TENANT_NOT_FOUND")
    void updateTenantName_throws_whenTenantNotFound() {
        when(tenantRepository.findLatestByTenantId("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTenantName(
                "missing", "name", "reason", OPERATOR_ID))
                .isInstanceOf(TenantUpdateException.class)
                .matches(e -> ((TenantUpdateException) e).getError()
                        == TenantUpdateError.TENANT_NOT_FOUND);
    }

    @Test
    @DisplayName("updateTenantName: name不正で NAME_INVALID")
    void updateTenantName_throws_whenNameInvalid() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant existing = new Tenant("t1", t1, "Old", FIXED_SLUG, t1, "creator");
        when(tenantRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateTenantName(
                "t1", "", "reason", OPERATOR_ID))
                .isInstanceOf(TenantUpdateException.class)
                .matches(e -> ((TenantUpdateException) e).getError()
                        == TenantUpdateError.NAME_INVALID);
    }

    @Test
    @DisplayName("updateTenantName: reason不正で REASON_INVALID")
    void updateTenantName_throws_whenReasonInvalid() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant existing = new Tenant("t1", t1, "Old", FIXED_SLUG, t1, "creator");
        when(tenantRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateTenantName(
                "t1", "Name", "  ", OPERATOR_ID))
                .isInstanceOf(TenantUpdateException.class)
                .matches(e -> ((TenantUpdateException) e).getError()
                        == TenantUpdateError.REASON_INVALID);
    }

    @Test
    @DisplayName("changeTenantStatus: 正常入力で新versionレコードを保存する")
    void changeTenantStatus_savesNewStatus() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant tenant = new Tenant("t1", t1, "Tenant", FIXED_SLUG, t1, "creator");
        when(tenantRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(tenant));
        when(tenantStatusRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(statusOf("t1", t1, TenantStatusValue.ACTIVE)));

        service.changeTenantStatus(
                "t1", TenantStatusValue.SUSPENDED, "reason", OPERATOR_ID);

        ArgumentCaptor<TenantStatus> captor = ArgumentCaptor.forClass(TenantStatus.class);
        verify(tenantStatusRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TenantStatusValue.SUSPENDED);
        assertThat(captor.getValue().getReason()).isEqualTo("reason");
        assertThat(captor.getValue().getCreatedBy())
                .startsWith("system:tenant-status-change:")
                .endsWith(OPERATOR_ID);
    }

    @Test
    @DisplayName("changeTenantStatus: テナント未存在で TENANT_NOT_FOUND")
    void changeTenantStatus_throws_whenTenantNotFound() {
        when(tenantRepository.findLatestByTenantId("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeTenantStatus(
                "missing", TenantStatusValue.ACTIVE, "reason", OPERATOR_ID))
                .isInstanceOf(TenantStatusChangeException.class)
                .matches(e -> ((TenantStatusChangeException) e).getError()
                        == TenantStatusChangeError.TENANT_NOT_FOUND);
    }

    @Test
    @DisplayName("changeTenantStatus: 既に同じステータスで ALREADY_IN_TARGET_STATUS")
    void changeTenantStatus_throws_whenAlreadyInTargetStatus() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant tenant = new Tenant("t1", t1, "Tenant", FIXED_SLUG, t1, "creator");
        when(tenantRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(tenant));
        when(tenantStatusRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(statusOf("t1", t1, TenantStatusValue.ACTIVE)));

        assertThatThrownBy(() -> service.changeTenantStatus(
                "t1", TenantStatusValue.ACTIVE, "reason", OPERATOR_ID))
                .isInstanceOf(TenantStatusChangeException.class)
                .matches(e -> ((TenantStatusChangeException) e).getError()
                        == TenantStatusChangeError.ALREADY_IN_TARGET_STATUS);
    }

    @Test
    @DisplayName("changeTenantStatus: reason不正で REASON_INVALID")
    void changeTenantStatus_throws_whenReasonInvalid() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant tenant = new Tenant("t1", t1, "Tenant", FIXED_SLUG, t1, "creator");
        when(tenantRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.changeTenantStatus(
                "t1", TenantStatusValue.SUSPENDED, null, OPERATOR_ID))
                .isInstanceOf(TenantStatusChangeException.class)
                .matches(e -> ((TenantStatusChangeException) e).getError()
                        == TenantStatusChangeError.REASON_INVALID);
    }

    @Test
    @DisplayName("addHost: 正常入力でhostとhostStatusを保存する")
    void addHost_savesHostAndStatus() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant tenant = new Tenant("t1", t1, "Tenant", FIXED_SLUG, t1, "creator");
        when(tenantRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(tenant));
        when(tenantHostRepository.existsByTenantIdAndHost("t1", "new.example.com"))
                .thenReturn(false);

        service.addHost("t1", "new.example.com", "host add reason", OPERATOR_ID);

        ArgumentCaptor<TenantHost> hostCaptor = ArgumentCaptor.forClass(TenantHost.class);
        verify(tenantHostRepository).save(hostCaptor.capture());
        assertThat(hostCaptor.getValue().getHost()).isEqualTo("new.example.com");
        assertThat(hostCaptor.getValue().getTenantId()).isEqualTo("t1");

        ArgumentCaptor<TenantHostStatus> statusCaptor =
                ArgumentCaptor.forClass(TenantHostStatus.class);
        verify(tenantHostStatusRepository).save(statusCaptor.capture());
        assertThat(statusCaptor.getValue().getStatus())
                .isEqualTo(TenantHostStatusValue.ACTIVE);
    }

    @Test
    @DisplayName("addHost: テナント未存在で TENANT_NOT_FOUND")
    void addHost_throws_whenTenantNotFound() {
        when(tenantRepository.findLatestByTenantId("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addHost(
                "missing", "example.com", "reason", OPERATOR_ID))
                .isInstanceOf(TenantHostException.class)
                .matches(e -> ((TenantHostException) e).getError()
                        == TenantHostError.TENANT_NOT_FOUND);
    }

    @Test
    @DisplayName("addHost: host形式不正で HOST_INVALID")
    void addHost_throws_whenHostInvalid() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant tenant = new Tenant("t1", t1, "Tenant", FIXED_SLUG, t1, "creator");
        when(tenantRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.addHost(
                "t1", "https://invalid.example.com", "reason", OPERATOR_ID))
                .isInstanceOf(TenantHostException.class)
                .matches(e -> ((TenantHostException) e).getError()
                        == TenantHostError.HOST_INVALID);
    }

    @Test
    @DisplayName("addHost: 同一テナント・同一hostの重複で HOST_DUPLICATE")
    void addHost_throws_whenHostDuplicate() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant tenant = new Tenant("t1", t1, "Tenant", FIXED_SLUG, t1, "creator");
        when(tenantRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(tenant));
        when(tenantHostRepository.existsByTenantIdAndHost("t1", "duplicate.example.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.addHost(
                "t1", "duplicate.example.com", "reason", OPERATOR_ID))
                .isInstanceOf(TenantHostException.class)
                .matches(e -> ((TenantHostException) e).getError()
                        == TenantHostError.HOST_DUPLICATE);
    }

    @Test
    @DisplayName("addHost: reason不正で REASON_INVALID")
    void addHost_throws_whenReasonInvalid() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant tenant = new Tenant("t1", t1, "Tenant", FIXED_SLUG, t1, "creator");
        when(tenantRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.addHost(
                "t1", "example.com", "", OPERATOR_ID))
                .isInstanceOf(TenantHostException.class)
                .matches(e -> ((TenantHostException) e).getError()
                        == TenantHostError.REASON_INVALID);
    }

    @Test
    @DisplayName("disableHost: ACTIVE→INACTIVEで保存する")
    void disableHost_changesStatusToInactive() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant tenant = new Tenant("t1", t1, "Tenant", FIXED_SLUG, t1, "creator");
        when(tenantRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(tenant));
        when(tenantHostRepository.findLatestByTenantIdAndHost("t1", "host.example.com"))
                .thenReturn(Optional.of(new TenantHost("t1", "host.example.com", t1, t1, "c")));
        when(tenantHostStatusRepository.findLatestByTenantIdAndHost("t1", "host.example.com"))
                .thenReturn(Optional.of(hostStatusOf(
                        "t1", "host.example.com", t1, TenantHostStatusValue.ACTIVE)));

        service.disableHost("t1", "host.example.com", "disable reason", OPERATOR_ID);

        ArgumentCaptor<TenantHostStatus> captor =
                ArgumentCaptor.forClass(TenantHostStatus.class);
        verify(tenantHostStatusRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TenantHostStatusValue.INACTIVE);
        assertThat(captor.getValue().getReason()).isEqualTo("disable reason");
    }

    @Test
    @DisplayName("disableHost: テナント未存在で TENANT_NOT_FOUND")
    void disableHost_throws_whenTenantNotFound() {
        when(tenantRepository.findLatestByTenantId("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.disableHost(
                "missing", "host.example.com", "reason", OPERATOR_ID))
                .isInstanceOf(TenantHostException.class)
                .matches(e -> ((TenantHostException) e).getError()
                        == TenantHostError.TENANT_NOT_FOUND);
    }

    @Test
    @DisplayName("disableHost: host未存在で HOST_NOT_FOUND")
    void disableHost_throws_whenHostNotFound() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant tenant = new Tenant("t1", t1, "Tenant", FIXED_SLUG, t1, "creator");
        when(tenantRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(tenant));
        when(tenantHostRepository.findLatestByTenantIdAndHost("t1", "missing.example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.disableHost(
                "t1", "missing.example.com", "reason", OPERATOR_ID))
                .isInstanceOf(TenantHostException.class)
                .matches(e -> ((TenantHostException) e).getError()
                        == TenantHostError.HOST_NOT_FOUND);
    }

    @Test
    @DisplayName("disableHost: 既にINACTIVEで ALREADY_IN_TARGET_STATUS")
    void disableHost_throws_whenAlreadyInactive() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant tenant = new Tenant("t1", t1, "Tenant", FIXED_SLUG, t1, "creator");
        when(tenantRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(tenant));
        when(tenantHostRepository.findLatestByTenantIdAndHost("t1", "host.example.com"))
                .thenReturn(Optional.of(new TenantHost("t1", "host.example.com", t1, t1, "c")));
        when(tenantHostStatusRepository.findLatestByTenantIdAndHost("t1", "host.example.com"))
                .thenReturn(Optional.of(hostStatusOf(
                        "t1", "host.example.com", t1, TenantHostStatusValue.INACTIVE)));

        assertThatThrownBy(() -> service.disableHost(
                "t1", "host.example.com", "reason", OPERATOR_ID))
                .isInstanceOf(TenantHostException.class)
                .matches(e -> ((TenantHostException) e).getError()
                        == TenantHostError.ALREADY_IN_TARGET_STATUS);
    }

    @Test
    @DisplayName("disableHost: host形式不正で HOST_INVALID")
    void disableHost_throws_whenHostInvalid() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant tenant = new Tenant("t1", t1, "Tenant", FIXED_SLUG, t1, "creator");
        when(tenantRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.disableHost(
                "t1", "https://bad.example.com", "reason", OPERATOR_ID))
                .isInstanceOf(TenantHostException.class)
                .matches(e -> ((TenantHostException) e).getError()
                        == TenantHostError.HOST_INVALID);
    }

    @Test
    @DisplayName("enableHost: INACTIVE→ACTIVEで保存する")
    void enableHost_changesStatusToActive() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant tenant = new Tenant("t1", t1, "Tenant", FIXED_SLUG, t1, "creator");
        when(tenantRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(tenant));
        when(tenantHostRepository.findLatestByTenantIdAndHost("t1", "host.example.com"))
                .thenReturn(Optional.of(new TenantHost("t1", "host.example.com", t1, t1, "c")));
        when(tenantHostStatusRepository.findLatestByTenantIdAndHost("t1", "host.example.com"))
                .thenReturn(Optional.of(hostStatusOf(
                        "t1", "host.example.com", t1, TenantHostStatusValue.INACTIVE)));

        service.enableHost("t1", "host.example.com", "enable reason", OPERATOR_ID);

        ArgumentCaptor<TenantHostStatus> captor =
                ArgumentCaptor.forClass(TenantHostStatus.class);
        verify(tenantHostStatusRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TenantHostStatusValue.ACTIVE);
        assertThat(captor.getValue().getReason()).isEqualTo("enable reason");
    }

    @Test
    @DisplayName("enableHost: 既にACTIVEで ALREADY_IN_TARGET_STATUS")
    void enableHost_throws_whenAlreadyActive() {
        OffsetDateTime t1 = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Tenant tenant = new Tenant("t1", t1, "Tenant", FIXED_SLUG, t1, "creator");
        when(tenantRepository.findLatestByTenantId("t1"))
                .thenReturn(Optional.of(tenant));
        when(tenantHostRepository.findLatestByTenantIdAndHost("t1", "host.example.com"))
                .thenReturn(Optional.of(new TenantHost("t1", "host.example.com", t1, t1, "c")));
        when(tenantHostStatusRepository.findLatestByTenantIdAndHost("t1", "host.example.com"))
                .thenReturn(Optional.of(hostStatusOf(
                        "t1", "host.example.com", t1, TenantHostStatusValue.ACTIVE)));

        assertThatThrownBy(() -> service.enableHost(
                "t1", "host.example.com", "reason", OPERATOR_ID))
                .isInstanceOf(TenantHostException.class)
                .matches(e -> ((TenantHostException) e).getError()
                        == TenantHostError.ALREADY_IN_TARGET_STATUS);
    }

    @Test
    @DisplayName("registerTenant: 入力検証失敗時はリポジトリへの保存を一切行わない")
    void registerTenant_doesNotSaveAnything_whenValidationFails() {
        try {
            service.registerTenant(null, List.of("x.com"), "r", OPERATOR_ID);
        } catch (TenantRegistrationException expected) {
            // expected
        }

        verify(tenantRepository, never()).save(any(Tenant.class));
        verify(tenantStatusRepository, never()).save(any(TenantStatus.class));
        verify(tenantHostRepository, never()).save(any(TenantHost.class));
        verify(tenantHostStatusRepository, never()).save(any(TenantHostStatus.class));
    }

    private TenantStatus statusOf(
            String tenantId, OffsetDateTime version, TenantStatusValue status) {
        return new TenantStatus(
                tenantId, version, status, "reason", version, "creator");
    }

    private TenantHostStatus hostStatusOf(
            String tenantId, String host, OffsetDateTime version,
            TenantHostStatusValue status) {
        return new TenantHostStatus(
                tenantId, host, version, status, "reason", version, "creator");
    }

}
