package io.github.kizulog_community.kizulog.infrastructure.web.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.kizulog_community.kizulog.domain.tenant.model.Tenant;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHost;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostStatus;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostStatusValue;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantStatus;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantStatusValue;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantHostRepository;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantHostStatusRepository;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantRepository;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantStatusRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * TenantResolverFilterの単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantResolverFilterTest {

    /** テストで使う21文字のslug（パターン [a-z0-9-]{21} に一致） */
    private static final String VALID_SLUG = "abc123def456ghi789jkl";

    /** テストで使うテナントID */
    private static final String TENANT_ID = "tenant-uuid-1";

    /** テストで使うホスト名 */
    private static final String HOST = "example.com";

    /** テストで使う固定タイムスタンプ */
    private static final OffsetDateTime T0 =
            OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private TenantRepository tenantRepository;
    private TenantStatusRepository tenantStatusRepository;
    private TenantHostRepository tenantHostRepository;
    private TenantHostStatusRepository tenantHostStatusRepository;
    private TenantResolverFilter filter;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        tenantRepository = mock(TenantRepository.class);
        tenantStatusRepository = mock(TenantStatusRepository.class);
        tenantHostRepository = mock(TenantHostRepository.class);
        tenantHostStatusRepository = mock(TenantHostStatusRepository.class);

        filter = new TenantResolverFilter(
                tenantRepository,
                tenantStatusRepository,
                tenantHostRepository,
                tenantHostStatusRepository);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        when(request.getContextPath()).thenReturn("");
        when(request.getServerName()).thenReturn(HOST);
    }

    @AfterEach
    void tearDown() {
        // 念のためテスト後にTenantContextをクリア
        TenantContext.clear();
    }

    @Test
    @DisplayName("doFilter: /t/ で始まらないURLは素通しする")
    void doFilter_passesThrough_whenPathDoesNotStartWithTenantPrefix() throws Exception {
        when(request.getRequestURI()).thenReturn("/system/tenants");

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).sendError(eq(HttpServletResponse.SC_NOT_FOUND));
    }

    @Test
    @DisplayName("doFilter: 素通し時はTenantContextに何もセットしない")
    void doFilter_doesNotSetContext_whenPassesThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/system/tenants");

        filter.doFilter(request, response, filterChain);

        assertThat(TenantContext.current()).isNull();
    }

    @Test
    @DisplayName("doFilter: /t/ で始まるがslugが正規表現に一致しない場合は404")
    void doFilter_returns404_whenSlugDoesNotMatchPattern() throws Exception {
        when(request.getRequestURI()).thenReturn("/t/SHORT/login");

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilter: slugが大文字を含む場合は404（[a-z0-9-]{21}に一致しない）")
    void doFilter_returns404_whenSlugContainsUppercase() throws Exception {
        // 大文字混在 21文字
        when(request.getRequestURI())
                .thenReturn("/t/ABC123DEF456ghi789jkl/login");

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    @DisplayName("doFilter: slugが22文字以上の場合は404")
    void doFilter_returns404_whenSlugTooLong() throws Exception {
        String tooLong = "a".repeat(22);
        when(request.getRequestURI()).thenReturn("/t/" + tooLong + "/login");

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    @DisplayName("doFilter: slugでテナントが見つからない場合は404")
    void doFilter_returns404_whenTenantNotFound() throws Exception {
        when(request.getRequestURI()).thenReturn("/t/" + VALID_SLUG + "/login");
        when(tenantRepository.findLatestBySlug(VALID_SLUG))
                .thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilter: テナントがSUSPENDED状態の場合は404")
    void doFilter_returns404_whenTenantSuspended() throws Exception {
        when(request.getRequestURI()).thenReturn("/t/" + VALID_SLUG + "/login");
        when(tenantRepository.findLatestBySlug(VALID_SLUG))
                .thenReturn(Optional.of(makeTenant()));
        when(tenantStatusRepository.findLatestByTenantId(TENANT_ID))
                .thenReturn(Optional.of(makeStatus(TenantStatusValue.SUSPENDED)));

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    @DisplayName("doFilter: テナントがINACTIVE状態の場合は404")
    void doFilter_returns404_whenTenantInactive() throws Exception {
        when(request.getRequestURI()).thenReturn("/t/" + VALID_SLUG + "/login");
        when(tenantRepository.findLatestBySlug(VALID_SLUG))
                .thenReturn(Optional.of(makeTenant()));
        when(tenantStatusRepository.findLatestByTenantId(TENANT_ID))
                .thenReturn(Optional.of(makeStatus(TenantStatusValue.INACTIVE)));

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    @DisplayName("doFilter: リクエストhostがtenant_hostsに紐づかない場合は404")
    void doFilter_returns404_whenHostNotLinkedToTenant() throws Exception {
        when(request.getRequestURI()).thenReturn("/t/" + VALID_SLUG + "/login");
        when(tenantRepository.findLatestBySlug(VALID_SLUG))
                .thenReturn(Optional.of(makeTenant()));
        when(tenantStatusRepository.findLatestByTenantId(TENANT_ID))
                .thenReturn(Optional.of(makeStatus(TenantStatusValue.ACTIVE)));
        // 別テナントの同一host
        when(tenantHostRepository.findAllLatestByHost(HOST))
                .thenReturn(List.of(
                        new TenantHost("other-tenant", HOST, T0, T0, "creator")));

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    @DisplayName("doFilter: hostがINACTIVE状態の場合は404")
    void doFilter_returns404_whenHostInactive() throws Exception {
        when(request.getRequestURI()).thenReturn("/t/" + VALID_SLUG + "/login");
        when(tenantRepository.findLatestBySlug(VALID_SLUG))
                .thenReturn(Optional.of(makeTenant()));
        when(tenantStatusRepository.findLatestByTenantId(TENANT_ID))
                .thenReturn(Optional.of(makeStatus(TenantStatusValue.ACTIVE)));
        when(tenantHostRepository.findAllLatestByHost(HOST))
                .thenReturn(List.of(
                        new TenantHost(TENANT_ID, HOST, T0, T0, "creator")));
        when(tenantHostStatusRepository.findLatestByTenantIdAndHost(TENANT_ID, HOST))
                .thenReturn(Optional.of(makeHostStatus(TenantHostStatusValue.INACTIVE)));

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    @DisplayName("doFilter: request.getServerName()がnullの場合は404")
    void doFilter_returns404_whenServerNameIsNull() throws Exception {
        when(request.getRequestURI()).thenReturn("/t/" + VALID_SLUG + "/login");
        when(request.getServerName()).thenReturn(null);
        when(tenantRepository.findLatestBySlug(VALID_SLUG))
                .thenReturn(Optional.of(makeTenant()));
        when(tenantStatusRepository.findLatestByTenantId(TENANT_ID))
                .thenReturn(Optional.of(makeStatus(TenantStatusValue.ACTIVE)));

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    @DisplayName("doFilter: 全条件を満たす場合は後続フィルタを呼び、TenantContextはクリアされる")
    void doFilter_callsChainAndClearsContext_onSuccess() throws Exception {
        when(request.getRequestURI()).thenReturn("/t/" + VALID_SLUG + "/login");
        when(tenantRepository.findLatestBySlug(VALID_SLUG))
                .thenReturn(Optional.of(makeTenant()));
        when(tenantStatusRepository.findLatestByTenantId(TENANT_ID))
                .thenReturn(Optional.of(makeStatus(TenantStatusValue.ACTIVE)));
        when(tenantHostRepository.findAllLatestByHost(HOST))
                .thenReturn(List.of(
                        new TenantHost(TENANT_ID, HOST, T0, T0, "creator")));
        when(tenantHostStatusRepository.findLatestByTenantIdAndHost(TENANT_ID, HOST))
                .thenReturn(Optional.of(makeHostStatus(TenantHostStatusValue.ACTIVE)));

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).sendError(eq(HttpServletResponse.SC_NOT_FOUND));
        // finallyでクリアされた後
        assertThat(TenantContext.current()).isNull();
    }

    @Test
    @DisplayName("doFilter: hostが大文字を含む場合は小文字化してマッチする")
    void doFilter_lowercasesHostForMatching() throws Exception {
        when(request.getRequestURI()).thenReturn("/t/" + VALID_SLUG + "/login");
        // 大文字混在のホスト名がリクエスト
        when(request.getServerName()).thenReturn("Example.COM");
        when(tenantRepository.findLatestBySlug(VALID_SLUG))
                .thenReturn(Optional.of(makeTenant()));
        when(tenantStatusRepository.findLatestByTenantId(TENANT_ID))
                .thenReturn(Optional.of(makeStatus(TenantStatusValue.ACTIVE)));
        // DBには小文字でhost登録
        when(tenantHostRepository.findAllLatestByHost("example.com"))
                .thenReturn(List.of(
                        new TenantHost(TENANT_ID, "example.com", T0, T0, "creator")));
        when(tenantHostStatusRepository.findLatestByTenantIdAndHost(
                TENANT_ID, "example.com"))
                .thenReturn(Optional.of(makeHostStatus(TenantHostStatusValue.ACTIVE)));

        filter.doFilter(request, response, filterChain);

        verify(filterChain, atLeastOnce()).doFilter(request, response);
        verify(response, never()).sendError(eq(HttpServletResponse.SC_NOT_FOUND));
    }

    private Tenant makeTenant() {
        return new Tenant(TENANT_ID, T0, "Test Tenant", VALID_SLUG, T0, "creator");
    }

    private TenantStatus makeStatus(TenantStatusValue status) {
        return new TenantStatus(TENANT_ID, T0, status, "reason", T0, "creator");
    }

    private TenantHostStatus makeHostStatus(TenantHostStatusValue status) {
        return new TenantHostStatus(
                TENANT_ID, HOST, T0, status, "reason", T0, "creator");
    }

}
