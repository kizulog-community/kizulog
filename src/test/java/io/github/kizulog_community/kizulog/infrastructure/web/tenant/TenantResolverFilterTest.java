package io.github.kizulog_community.kizulog.infrastructure.web.tenant;

import static org.assertj.core.api.Assertions.assertThat;
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
import io.github.kizulog_community.kizulog.infrastructure.security.matcher.SystemHostMatcher;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * TenantResolverFilter（ホストベース）の単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantResolverFilterTest {

    private static final String SYSTEM_HOST = "kizulog.dev.internal";
    private static final String TENANT_HOST = "acme.example.com";
    private static final String TENANT_ID = "tenant-uuid-1";
    private static final OffsetDateTime T0 =
            OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private SystemHostMatcher systemHostMatcher;
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
        systemHostMatcher = new SystemHostMatcher(SYSTEM_HOST);
        tenantRepository = mock(TenantRepository.class);
        tenantStatusRepository = mock(TenantStatusRepository.class);
        tenantHostRepository = mock(TenantHostRepository.class);
        tenantHostStatusRepository = mock(TenantHostStatusRepository.class);

        filter = new TenantResolverFilter(
                systemHostMatcher,
                tenantRepository,
                tenantStatusRepository,
                tenantHostRepository,
                tenantHostStatusRepository);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Tenant tenant() {
        return new Tenant(TENANT_ID, T0, "Acme Inc", "slug-acme-000000000000", T0, "system");
    }

    private TenantHost tenantHost() {
        return new TenantHost(TENANT_ID, TENANT_HOST, T0, T0, "system");
    }

    private TenantStatus tenantStatus(TenantStatusValue value) {
        return new TenantStatus(TENANT_ID, T0, value, "reason", T0, "system");
    }

    private TenantHostStatus tenantHostStatus(TenantHostStatusValue value) {
        return new TenantHostStatus(TENANT_ID, TENANT_HOST, T0, value, "reason", T0, "system");
    }

    @Test
    @DisplayName("doFilter: システムホスト宛は素通しする")
    void doFilter_passesThrough_whenSystemHost() throws Exception {
        when(request.getServerName()).thenReturn(SYSTEM_HOST);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    @DisplayName("doFilter: システムホスト素通し時はテナント解決を行わない")
    void doFilter_doesNotResolveTenant_whenSystemHost() throws Exception {
        when(request.getServerName()).thenReturn(SYSTEM_HOST);

        filter.doFilter(request, response, filterChain);

        verify(tenantHostRepository, never())
                .findAllLatestByHost(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("doFilter: テナントホスト宛でACTIVEテナントを解決し後続へ進む")
    void doFilter_resolvesTenant_andProceeds() throws Exception {
        when(request.getServerName()).thenReturn(TENANT_HOST);
        when(tenantHostRepository.findAllLatestByHost(TENANT_HOST))
                .thenReturn(List.of(tenantHost()));
        when(tenantHostStatusRepository.findLatestByTenantIdAndHost(TENANT_ID, TENANT_HOST))
                .thenReturn(Optional.of(tenantHostStatus(TenantHostStatusValue.ACTIVE)));
        when(tenantRepository.findLatestByTenantId(TENANT_ID))
                .thenReturn(Optional.of(tenant()));
        when(tenantStatusRepository.findLatestByTenantId(TENANT_ID))
                .thenReturn(Optional.of(tenantStatus(TenantStatusValue.ACTIVE)));

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    @DisplayName("doFilter: hostに紐づくテナントが無ければ404")
    void doFilter_returns404_whenNoTenantForHost() throws Exception {
        when(request.getServerName()).thenReturn(TENANT_HOST);
        when(tenantHostRepository.findAllLatestByHost(TENANT_HOST))
                .thenReturn(List.of());

        filter.doFilter(request, response, filterChain);

        verify(response, atLeastOnce()).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilter: host_statusがINACTIVEなら404")
    void doFilter_returns404_whenHostInactive() throws Exception {
        when(request.getServerName()).thenReturn(TENANT_HOST);
        when(tenantHostRepository.findAllLatestByHost(TENANT_HOST))
                .thenReturn(List.of(tenantHost()));
        when(tenantHostStatusRepository.findLatestByTenantIdAndHost(TENANT_ID, TENANT_HOST))
                .thenReturn(Optional.of(tenantHostStatus(TenantHostStatusValue.INACTIVE)));

        filter.doFilter(request, response, filterChain);

        verify(response, atLeastOnce()).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilter: host_statusが未登録なら404")
    void doFilter_returns404_whenHostStatusMissing() throws Exception {
        when(request.getServerName()).thenReturn(TENANT_HOST);
        when(tenantHostRepository.findAllLatestByHost(TENANT_HOST))
                .thenReturn(List.of(tenantHost()));
        when(tenantHostStatusRepository.findLatestByTenantIdAndHost(TENANT_ID, TENANT_HOST))
                .thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        verify(response, atLeastOnce()).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilter: テナント本体が無ければ404")
    void doFilter_returns404_whenTenantMissing() throws Exception {
        when(request.getServerName()).thenReturn(TENANT_HOST);
        when(tenantHostRepository.findAllLatestByHost(TENANT_HOST))
                .thenReturn(List.of(tenantHost()));
        when(tenantHostStatusRepository.findLatestByTenantIdAndHost(TENANT_ID, TENANT_HOST))
                .thenReturn(Optional.of(tenantHostStatus(TenantHostStatusValue.ACTIVE)));
        when(tenantRepository.findLatestByTenantId(TENANT_ID))
                .thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        verify(response, atLeastOnce()).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilter: テナントステータスがINACTIVEなら404")
    void doFilter_returns404_whenTenantInactive() throws Exception {
        when(request.getServerName()).thenReturn(TENANT_HOST);
        when(tenantHostRepository.findAllLatestByHost(TENANT_HOST))
                .thenReturn(List.of(tenantHost()));
        when(tenantHostStatusRepository.findLatestByTenantIdAndHost(TENANT_ID, TENANT_HOST))
                .thenReturn(Optional.of(tenantHostStatus(TenantHostStatusValue.ACTIVE)));
        when(tenantRepository.findLatestByTenantId(TENANT_ID))
                .thenReturn(Optional.of(tenant()));
        when(tenantStatusRepository.findLatestByTenantId(TENANT_ID))
                .thenReturn(Optional.of(tenantStatus(TenantStatusValue.INACTIVE)));

        filter.doFilter(request, response, filterChain);

        verify(response, atLeastOnce()).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilter: テナントステータスが未登録なら404")
    void doFilter_returns404_whenTenantStatusMissing() throws Exception {
        when(request.getServerName()).thenReturn(TENANT_HOST);
        when(tenantHostRepository.findAllLatestByHost(TENANT_HOST))
                .thenReturn(List.of(tenantHost()));
        when(tenantHostStatusRepository.findLatestByTenantIdAndHost(TENANT_ID, TENANT_HOST))
                .thenReturn(Optional.of(tenantHostStatus(TenantHostStatusValue.ACTIVE)));
        when(tenantRepository.findLatestByTenantId(TENANT_ID))
                .thenReturn(Optional.of(tenant()));
        when(tenantStatusRepository.findLatestByTenantId(TENANT_ID))
                .thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        verify(response, atLeastOnce()).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilter: serverNameが空文字なら404")
    void doFilter_returns404_whenServerNameEmpty() throws Exception {
        when(request.getServerName()).thenReturn("");

        filter.doFilter(request, response, filterChain);

        verify(response, atLeastOnce()).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilter: 解決成功後はTenantContextがクリアされる")
    void doFilter_clearsContext_afterSuccess() throws Exception {
        when(request.getServerName()).thenReturn(TENANT_HOST);
        when(tenantHostRepository.findAllLatestByHost(TENANT_HOST))
                .thenReturn(List.of(tenantHost()));
        when(tenantHostStatusRepository.findLatestByTenantIdAndHost(TENANT_ID, TENANT_HOST))
                .thenReturn(Optional.of(tenantHostStatus(TenantHostStatusValue.ACTIVE)));
        when(tenantRepository.findLatestByTenantId(TENANT_ID))
                .thenReturn(Optional.of(tenant()));
        when(tenantStatusRepository.findLatestByTenantId(TENANT_ID))
                .thenReturn(Optional.of(tenantStatus(TenantStatusValue.ACTIVE)));

        filter.doFilter(request, response, filterChain);

        assertThat(TenantContext.current()).isNull();
    }

}
