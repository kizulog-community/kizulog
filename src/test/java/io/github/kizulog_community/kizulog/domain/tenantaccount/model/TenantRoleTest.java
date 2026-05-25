package io.github.kizulog_community.kizulog.domain.tenantaccount.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TenantRole の単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantRoleTest {

    @Test
    @DisplayName("TENANT_ADMIN: enum値が存在する")
    void tenantAdmin_isDefined() {
        assertThat(TenantRole.TENANT_ADMIN).isNotNull();
    }

    @Test
    @DisplayName("EMPLOYEE: enum値が存在する")
    void employee_isDefined() {
        assertThat(TenantRole.EMPLOYEE).isNotNull();
    }

    @Test
    @DisplayName("values: 2つの値（TENANT_ADMIN/EMPLOYEE）が定義されている")
    void values_containsAllRoles() {
        assertThat(TenantRole.values())
                .hasSize(2)
                .containsExactly(TenantRole.TENANT_ADMIN, TenantRole.EMPLOYEE);
    }

    @Test
    @DisplayName("valueOf: 文字列からEnum値を取得できる")
    void valueOf_returnsCorrectEnum() {
        assertThat(TenantRole.valueOf("TENANT_ADMIN"))
                .isEqualTo(TenantRole.TENANT_ADMIN);
        assertThat(TenantRole.valueOf("EMPLOYEE"))
                .isEqualTo(TenantRole.EMPLOYEE);
    }

    @Test
    @DisplayName("name: Enum名がDB保存値と一致する")
    void name_matchesDbStringValue() {
        assertThat(TenantRole.TENANT_ADMIN.name()).isEqualTo("TENANT_ADMIN");
        assertThat(TenantRole.EMPLOYEE.name()).isEqualTo("EMPLOYEE");
    }

}
