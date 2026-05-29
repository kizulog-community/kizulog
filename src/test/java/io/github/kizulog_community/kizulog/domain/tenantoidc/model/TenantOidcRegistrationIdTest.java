package io.github.kizulog_community.kizulog.domain.tenantoidc.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TenantOidcRegistrationIdの単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantOidcRegistrationIdTest {

    private static final String VALID_TENANT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String VALID_PROVIDER_ID = "keycloak";
    private static final String VALID_REGISTRATION_ID =
            "tenant-" + VALID_TENANT_ID + "-" + VALID_PROVIDER_ID;

    @Test
    @DisplayName("PREFIX: 公開定数の値は 'tenant-' である")
    void prefix_isTenantHyphen() {
        assertThat(TenantOidcRegistrationId.PREFIX).isEqualTo("tenant-");
    }

    @Test
    @DisplayName("of: 正常な引数で構築できgetter/value()が一致する")
    void of_validArgs_buildsAndExposesGetters() {
        TenantOidcRegistrationId id = TenantOidcRegistrationId.of(
                VALID_TENANT_ID, VALID_PROVIDER_ID);

        assertThat(id.getTenantId()).isEqualTo(VALID_TENANT_ID);
        assertThat(id.getProviderId()).isEqualTo(VALID_PROVIDER_ID);
        assertThat(id.value()).isEqualTo(VALID_REGISTRATION_ID);
    }

    @Test
    @DisplayName("of: providerIdが英数字とハイフンの組合せでも構築できる")
    void of_providerIdWithDigitsAndHyphens_succeeds() {
        TenantOidcRegistrationId id = TenantOidcRegistrationId.of(
                VALID_TENANT_ID, "azure-ad-v2");

        assertThat(id.getProviderId()).isEqualTo("azure-ad-v2");
        assertThat(id.value()).isEqualTo("tenant-" + VALID_TENANT_ID + "-azure-ad-v2");
    }

    @Test
    @DisplayName("of: providerIdが1文字（境界の下限）でも構築できる")
    void of_providerIdSingleChar_succeeds() {
        TenantOidcRegistrationId id = TenantOidcRegistrationId.of(VALID_TENANT_ID, "a");

        assertThat(id.getProviderId()).isEqualTo("a");
    }

    @Test
    @DisplayName("of: providerIdが32文字（境界の上限）でも構築できる")
    void of_providerIdMaxLength_succeeds() {
        String thirtyTwo = "abcdefghijklmnopqrstuvwxyz012345"; // 32文字
        TenantOidcRegistrationId id = TenantOidcRegistrationId.of(VALID_TENANT_ID, thirtyTwo);

        assertThat(id.getProviderId()).isEqualTo(thirtyTwo);
    }

    @Test
    @DisplayName("of: tenantIdが大文字UUID（fA-F）でも構築できる")
    void of_uppercaseUuid_succeeds() {
        String upper = "550E8400-E29B-41D4-A716-446655440000";
        TenantOidcRegistrationId id = TenantOidcRegistrationId.of(upper, VALID_PROVIDER_ID);

        assertThat(id.getTenantId()).isEqualTo(upper);
    }

    @Test
    @DisplayName("of: tenantIdがnullなら IllegalArgumentException")
    void of_nullTenantId_throws() {
        assertThatThrownBy(() -> TenantOidcRegistrationId.of(null, VALID_PROVIDER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    @DisplayName("of: tenantIdがUUID形式でないなら IllegalArgumentException")
    void of_invalidTenantIdFormat_throws() {
        assertThatThrownBy(() -> TenantOidcRegistrationId.of("not-a-uuid", VALID_PROVIDER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    @DisplayName("of: tenantIdが空文字なら IllegalArgumentException")
    void of_emptyTenantId_throws() {
        assertThatThrownBy(() -> TenantOidcRegistrationId.of("", VALID_PROVIDER_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("of: tenantIdが長さ36でも文字種が不正なら IllegalArgumentException")
    void of_tenantIdInvalidChars_throws() {
        String badUuid = "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz";
        assertThatThrownBy(() -> TenantOidcRegistrationId.of(badUuid, VALID_PROVIDER_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("of: providerIdがnullなら IllegalArgumentException")
    void of_nullProviderId_throws() {
        assertThatThrownBy(() -> TenantOidcRegistrationId.of(VALID_TENANT_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("providerId");
    }

    @Test
    @DisplayName("of: providerIdが空文字なら IllegalArgumentException")
    void of_emptyProviderId_throws() {
        assertThatThrownBy(() -> TenantOidcRegistrationId.of(VALID_TENANT_ID, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("providerId");
    }

    @Test
    @DisplayName("of: providerIdに大文字が含まれると IllegalArgumentException")
    void of_uppercaseProviderId_throws() {
        assertThatThrownBy(() -> TenantOidcRegistrationId.of(VALID_TENANT_ID, "Keycloak"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("of: providerIdに記号（アンダースコア等）が含まれると IllegalArgumentException")
    void of_providerIdWithUnderscore_throws() {
        assertThatThrownBy(() -> TenantOidcRegistrationId.of(VALID_TENANT_ID, "key_cloak"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("of: providerIdが33文字以上なら IllegalArgumentException")
    void of_providerIdTooLong_throws() {
        String thirtyThree = "abcdefghijklmnopqrstuvwxyz0123456"; // 33文字
        assertThatThrownBy(() -> TenantOidcRegistrationId.of(VALID_TENANT_ID, thirtyThree))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("parse: 正常なregistrationIdをパースできる")
    void parse_validRegistrationId_returnsValueObject() {
        Optional<TenantOidcRegistrationId> result =
                TenantOidcRegistrationId.parse(VALID_REGISTRATION_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getTenantId()).isEqualTo(VALID_TENANT_ID);
        assertThat(result.get().getProviderId()).isEqualTo(VALID_PROVIDER_ID);
    }

    @Test
    @DisplayName("parse: providerId にハイフンを含んでもパースできる（UUID直後の最初のハイフンで分割）")
    void parse_providerIdWithHyphen_succeeds() {
        String regId = "tenant-" + VALID_TENANT_ID + "-azure-ad-v2";

        Optional<TenantOidcRegistrationId> result = TenantOidcRegistrationId.parse(regId);

        assertThat(result).isPresent();
        assertThat(result.get().getTenantId()).isEqualTo(VALID_TENANT_ID);
        assertThat(result.get().getProviderId()).isEqualTo("azure-ad-v2");
    }

    @Test
    @DisplayName("parse: nullは Optional.empty")
    void parse_null_returnsEmpty() {
        assertThat(TenantOidcRegistrationId.parse(null)).isEmpty();
    }

    @Test
    @DisplayName("parse: 空文字は Optional.empty")
    void parse_emptyString_returnsEmpty() {
        assertThat(TenantOidcRegistrationId.parse("")).isEmpty();
    }

    @Test
    @DisplayName("parse: プレフィックスが違う（system-...）と Optional.empty")
    void parse_wrongPrefix_returnsEmpty() {
        String regId = "system-" + VALID_TENANT_ID + "-" + VALID_PROVIDER_ID;
        assertThat(TenantOidcRegistrationId.parse(regId)).isEmpty();
    }

    @Test
    @DisplayName("parse: 全体が短すぎる（tenant-shortのみ）と Optional.empty")
    void parse_tooShort_returnsEmpty() {
        assertThat(TenantOidcRegistrationId.parse("tenant-short")).isEmpty();
    }

    @Test
    @DisplayName("parse: tenant- + UUID + 区切りなしで終わる（providerId無し）と Optional.empty")
    void parse_noProviderId_returnsEmpty() {
        // "tenant-" + UUID(36) のみ。区切り'-'もproviderIdもない
        assertThat(TenantOidcRegistrationId.parse("tenant-" + VALID_TENANT_ID)).isEmpty();
    }

    @Test
    @DisplayName("parse: UUID直後の区切り文字がハイフンでないと Optional.empty")
    void parse_wrongSeparator_returnsEmpty() {
        // "tenant-" + UUID + 'x' + "keycloak"
        String regId = "tenant-" + VALID_TENANT_ID + "x" + VALID_PROVIDER_ID;
        assertThat(TenantOidcRegistrationId.parse(regId)).isEmpty();
    }

    @Test
    @DisplayName("parse: UUID直後がハイフンでもproviderIdが空（'tenant-{uuid}-'）なら Optional.empty")
    void parse_emptyProviderIdAfterSeparator_returnsEmpty() {
        // "tenant-" + UUID + "-" で終わる
        String regId = "tenant-" + VALID_TENANT_ID + "-";
        assertThat(TenantOidcRegistrationId.parse(regId)).isEmpty();
    }

    @Test
    @DisplayName("parse: UUID部分が長さ36だが文字種が不正なら Optional.empty")
    void parse_invalidUuidChars_returnsEmpty() {
        String badUuid = "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz";
        String regId = "tenant-" + badUuid + "-" + VALID_PROVIDER_ID;
        assertThat(TenantOidcRegistrationId.parse(regId)).isEmpty();
    }

    @Test
    @DisplayName("parse: providerId に大文字が含まれると Optional.empty")
    void parse_uppercaseProviderId_returnsEmpty() {
        String regId = "tenant-" + VALID_TENANT_ID + "-Keycloak";
        assertThat(TenantOidcRegistrationId.parse(regId)).isEmpty();
    }

    @Test
    @DisplayName("parse: providerId が33文字以上なら Optional.empty")
    void parse_providerIdTooLong_returnsEmpty() {
        String thirtyThree = "abcdefghijklmnopqrstuvwxyz0123456";
        String regId = "tenant-" + VALID_TENANT_ID + "-" + thirtyThree;
        assertThat(TenantOidcRegistrationId.parse(regId)).isEmpty();
    }

    @Test
    @DisplayName("parse: プレフィックスがハイフンを含むがまだ短い（'tenant-' のみ）なら Optional.empty")
    void parse_onlyPrefix_returnsEmpty() {
        assertThat(TenantOidcRegistrationId.parse("tenant-")).isEmpty();
    }

    @Test
    @DisplayName("value: tenant-{tenantId}-{providerId} 形式の文字列を返す")
    void value_returnsCorrectFormat() {
        TenantOidcRegistrationId id =
                TenantOidcRegistrationId.of(VALID_TENANT_ID, VALID_PROVIDER_ID);

        assertThat(id.value()).isEqualTo("tenant-" + VALID_TENANT_ID + "-" + VALID_PROVIDER_ID);
    }

    @Test
    @DisplayName("ラウンドトリップ: of().value() を parse すると同じ tenantId/providerId が得られる")
    void roundTrip_ofValueParseEqualsOriginal() {
        TenantOidcRegistrationId original =
                TenantOidcRegistrationId.of(VALID_TENANT_ID, VALID_PROVIDER_ID);

        Optional<TenantOidcRegistrationId> parsed =
                TenantOidcRegistrationId.parse(original.value());

        assertThat(parsed).isPresent();
        assertThat(parsed.get().getTenantId()).isEqualTo(original.getTenantId());
        assertThat(parsed.get().getProviderId()).isEqualTo(original.getProviderId());
        assertThat(parsed.get().value()).isEqualTo(original.value());
    }

    @Test
    @DisplayName("ラウンドトリップ: providerIdにハイフンを含む場合もラウンドトリップで保たれる")
    void roundTrip_providerIdWithHyphen_preserved() {
        TenantOidcRegistrationId original =
                TenantOidcRegistrationId.of(VALID_TENANT_ID, "azure-ad-v2");

        Optional<TenantOidcRegistrationId> parsed =
                TenantOidcRegistrationId.parse(original.value());

        assertThat(parsed).isPresent();
        assertThat(parsed.get().getProviderId()).isEqualTo("azure-ad-v2");
    }

}
