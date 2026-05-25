package io.github.kizulog_community.kizulog.domain.tenantaccount.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TenantAccountStatusValue の単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantAccountStatusValueTest {

    @Test
    @DisplayName("isAuthenticatable: ACTIVEはtrueを返す")
    void isAuthenticatable_active_returnsTrue() {
        assertThat(TenantAccountStatusValue.ACTIVE.isAuthenticatable()).isTrue();
    }

    @Test
    @DisplayName("isAuthenticatable: INACTIVEはfalseを返す")
    void isAuthenticatable_inactive_returnsFalse() {
        assertThat(TenantAccountStatusValue.INACTIVE.isAuthenticatable()).isFalse();
    }

    @Test
    @DisplayName("isAuthenticatable: SUSPENDEDはfalseを返す")
    void isAuthenticatable_suspended_returnsFalse() {
        assertThat(TenantAccountStatusValue.SUSPENDED.isAuthenticatable()).isFalse();
    }

    @Test
    @DisplayName("values: 3つの値（ACTIVE/INACTIVE/SUSPENDED）が定義されている")
    void values_containsAllThreeStatuses() {
        assertThat(TenantAccountStatusValue.values())
                .hasSize(3)
                .containsExactly(
                        TenantAccountStatusValue.ACTIVE,
                        TenantAccountStatusValue.INACTIVE,
                        TenantAccountStatusValue.SUSPENDED);
    }

    @Test
    @DisplayName("valueOf: 文字列からEnum値を取得できる")
    void valueOf_returnsCorrectEnum() {
        assertThat(TenantAccountStatusValue.valueOf("ACTIVE"))
                .isEqualTo(TenantAccountStatusValue.ACTIVE);
        assertThat(TenantAccountStatusValue.valueOf("INACTIVE"))
                .isEqualTo(TenantAccountStatusValue.INACTIVE);
        assertThat(TenantAccountStatusValue.valueOf("SUSPENDED"))
                .isEqualTo(TenantAccountStatusValue.SUSPENDED);
    }

    @Test
    @DisplayName("name: Enum名がDB保存値と一致する")
    void name_matchesDbStringValue() {
        assertThat(TenantAccountStatusValue.ACTIVE.name()).isEqualTo("ACTIVE");
        assertThat(TenantAccountStatusValue.INACTIVE.name()).isEqualTo("INACTIVE");
        assertThat(TenantAccountStatusValue.SUSPENDED.name()).isEqualTo("SUSPENDED");
    }

}
