package io.github.kizulog_community.kizulog.domain.systemaccount.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * AccountStatusの単体テスト
 *
 * @author Jun Kobayashi
 */
class AccountStatusTest {

    @Test
    @DisplayName("isAuthenticatable: ACTIVEはtrueを返す")
    void isAuthenticatable_active_returnsTrue() {
        assertThat(AccountStatus.ACTIVE.isAuthenticatable()).isTrue();
    }

    @Test
    @DisplayName("isAuthenticatable: INACTIVEはfalseを返す")
    void isAuthenticatable_inactive_returnsFalse() {
        assertThat(AccountStatus.INACTIVE.isAuthenticatable()).isFalse();
    }

    @Test
    @DisplayName("isAuthenticatable: SUSPENDEDはfalseを返す")
    void isAuthenticatable_suspended_returnsFalse() {
        assertThat(AccountStatus.SUSPENDED.isAuthenticatable()).isFalse();
    }

    @Test
    @DisplayName("values: 3つの値（ACTIVE/INACTIVE/SUSPENDED）が定義されている")
    void values_containsAllThreeStatuses() {
        assertThat(AccountStatus.values())
                .hasSize(3)
                .containsExactly(
                        AccountStatus.ACTIVE,
                        AccountStatus.INACTIVE,
                        AccountStatus.SUSPENDED);
    }

    @Test
    @DisplayName("valueOf: 文字列からEnum値を取得できる")
    void valueOf_returnsCorrectEnum() {
        assertThat(AccountStatus.valueOf("ACTIVE")).isEqualTo(AccountStatus.ACTIVE);
        assertThat(AccountStatus.valueOf("INACTIVE")).isEqualTo(AccountStatus.INACTIVE);
        assertThat(AccountStatus.valueOf("SUSPENDED")).isEqualTo(AccountStatus.SUSPENDED);
    }

    @Test
    @DisplayName("name: Enum名がDB保存値と一致する")
    void name_matchesDbStringValue() {
        assertThat(AccountStatus.ACTIVE.name()).isEqualTo("ACTIVE");
        assertThat(AccountStatus.INACTIVE.name()).isEqualTo("INACTIVE");
        assertThat(AccountStatus.SUSPENDED.name()).isEqualTo("SUSPENDED");
    }

}
