package io.github.kizulog_community.kizulog.infrastructure.web.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.shared.TenantTimezoneResolver;
import io.github.kizulog_community.kizulog.domain.tenant.model.Tenant;
import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantRole;
import io.github.kizulog_community.kizulog.domain.tenantattendance.model.AttendanceSession;
import io.github.kizulog_community.kizulog.domain.tenantattendance.model.WorkState;
import io.github.kizulog_community.kizulog.domain.tenantattendance.service.TenantAttendanceService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.TenantUserPrincipal;

/**
 * TenantDashboardControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantDashboardControllerTest {

    private static final String TENANT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String ISS = "https://auth.acme.example/realms/acme";
    private static final String AUD = "kizulog-acme";
    private static final String SUB = "user-uuid-123";
    private static final OffsetDateTime NOW =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private TenantDashboardController controller;
    private TenantAttendanceService attendanceService;
    private TenantTimezoneResolver timezoneResolver;
    private Model model;

    @BeforeEach
    void setUp() {
        attendanceService = mock(TenantAttendanceService.class);
        timezoneResolver = mock(TenantTimezoneResolver.class);
        // 既定スタブ：未出勤・空セッション、表示TZはAsia/Tokyo
        when(attendanceService.getCurrentSession(anyString()))
                .thenReturn(new AttendanceSession(WorkState.NOT_WORKING, List.of()));
        when(timezoneResolver.resolve(anyString(), anyString()))
                .thenReturn(SupportedTimezone.of("Asia/Tokyo"));
        controller = new TenantDashboardController(attendanceService, timezoneResolver);
        model = new ConcurrentModel();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Map<String, Object> toMap(Model model) {
        return new HashMap<>(model.asMap());
    }

    private Tenant tenant() {
        return new Tenant(TENANT_ID, NOW, "テナントA", "tenant-a", NOW, "creator");
    }

    private TenantUserPrincipal principal(Set<TenantRole> roles) {
        OidcIdToken idToken = OidcIdToken.withTokenValue("fake-token")
                .issuer(ISS)
                .subject(SUB)
                .audience(List.of(AUD))
                .build();
        return TenantUserPrincipal.ofTenantUser(
                TENANT_ID, "acc-1", "identity-1", ISS, AUD, SUB, idToken, roles);
    }

    @Test
    @DisplayName("dashboard: ビュー名tenant/dashboardを返す")
    void dashboard_returnsCorrectViewName() {
        TenantContext.set(tenant());

        String view = controller.dashboard(
                principal(Set.of(TenantRole.TENANT_ADMIN)), model);

        assertThat(view).isEqualTo("tenant/dashboard");
    }

    @Test
    @DisplayName("dashboard: principalとtenantがmodelに設定される")
    void dashboard_setsPrincipalAndTenant() {
        Tenant t = tenant();
        TenantContext.set(t);
        TenantUserPrincipal p = principal(Set.of(TenantRole.TENANT_ADMIN));

        controller.dashboard(p, model);

        Map<String, Object> attrs = toMap(model);
        assertThat(attrs.get("principal")).isSameAs(p);
        assertThat(attrs.get("tenant")).isSameAs(t);
    }

    @Test
    @DisplayName("dashboard: principalがある場合は打刻パネルがmodelに設定される")
    void dashboard_setsAttendancePanel_whenPrincipalPresent() {
        TenantContext.set(tenant());

        controller.dashboard(principal(Set.of(TenantRole.EMPLOYEE)), model);

        assertThat(toMap(model).get("attendancePanel")).isNotNull();
    }

    @Test
    @DisplayName("dashboard: TenantContext未設定でもtenant=nullで画面を返す（NPEにならない）")
    void dashboard_noTenantContext_setsNullTenant() {
        String view = controller.dashboard(
                principal(Set.of(TenantRole.TENANT_ADMIN)), model);

        assertThat(view).isEqualTo("tenant/dashboard");
        assertThat(toMap(model).get("tenant")).isNull();
    }

    @Test
    @DisplayName("dashboard: principalがnullでも画面を返す（打刻パネルは積まない）")
    void dashboard_nullPrincipal_returnsView() {
        TenantContext.set(tenant());

        String view = controller.dashboard(null, model);

        assertThat(view).isEqualTo("tenant/dashboard");
        assertThat(toMap(model).get("principal")).isNull();
        assertThat(toMap(model).get("attendancePanel")).isNull();
    }

    @Test
    @DisplayName("dashboard: TENANT_ADMINのprincipalがROLE_TENANT_ADMIN権限を持つ")
    void dashboard_tenantAdmin_hasAdminAuthority() {
        TenantContext.set(tenant());
        TenantUserPrincipal p = principal(Set.of(TenantRole.TENANT_ADMIN));

        controller.dashboard(p, model);

        TenantUserPrincipal modelPrincipal =
                (TenantUserPrincipal) toMap(model).get("principal");
        assertThat(modelPrincipal.getAuthorities())
                .extracting("authority")
                .contains(TenantUserPrincipal.ROLE_TENANT_ADMIN);
    }

}
