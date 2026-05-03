package io.github.kizulog_community.kizulog.domain.systemaccount.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SystemRoleの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemRoleTest {

    @Test
    @DisplayName("SYSTEM_ADMIN: enum値が存在する")
    void systemAdmin_isDefined() {
        assertThat(SystemRole.SYSTEM_ADMIN).isNotNull();
    }

    @Test
    @DisplayName("values: 1つの値（SYSTEM_ADMIN）が定義されている")
    void values_containsSystemAdmin() {
        assertThat(SystemRole.values())
                .hasSize(1)
                .containsExactly(SystemRole.SYSTEM_ADMIN);
    }

    @Test
    @DisplayName("valueOf: 文字列からEnum値を取得できる")
    void valueOf_returnsCorrectEnum() {
        assertThat(SystemRole.valueOf("SYSTEM_ADMIN"))
                .isEqualTo(SystemRole.SYSTEM_ADMIN);
    }

    @Test
    @DisplayName("name: Enum名がDB保存値と一致する")
    void name_matchesDbStringValue() {
        assertThat(SystemRole.SYSTEM_ADMIN.name()).isEqualTo("SYSTEM_ADMIN");
    }

}
