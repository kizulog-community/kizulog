package io.github.kizulog_community.kizulog.domain.tenantoidc.model;

import java.util.Optional;
import java.util.regex.Pattern;

import lombok.Getter;

/**
 * テナント側 OAuth2 registrationId 値オブジェクト
 *
 * @author Jun Kobayashi
 */
@Getter
public final class TenantOidcRegistrationId {

    /** registrationId の固定プレフィックス */
    public static final String PREFIX = "tenant-";

    /** UUID 区切り・providerId 区切りに用いるハイフン */
    private static final String SEPARATOR = "-";

    /** UUID の文字列長（8-4-4-4-12 = 36文字） */
    private static final int UUID_LENGTH = 36;

    /** tenantId（UUID）形式の検証パターン */
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}"
            + "-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /** providerId 形式の検証パターン */
    private static final Pattern PROVIDER_ID_PATTERN =
            Pattern.compile("^[a-z0-9-]{1,32}$");

    /** テナントID（UUID） */
    private final String tenantId;

    /** プロバイダー識別子 */
    private final String providerId;

    /**
     * 非公開コンストラクタ。
     *
     * @param tenantId テナントID
     * @param providerId プロバイダー識別子
     */
    private TenantOidcRegistrationId(String tenantId, String providerId) {
        this.tenantId = tenantId;
        this.providerId = providerId;
    }

    /**
     * tenantId と providerId から値オブジェクトを構築する。
     *
     * @param tenantId テナントID（UUID 形式）
     * @param providerId プロバイダー識別子（{@code [a-z0-9-]{1,32}}）
     * @return 値オブジェクト
     * @throws IllegalArgumentException tenantId または providerId が形式不正の場合
     */
    public static TenantOidcRegistrationId of(String tenantId, String providerId) {
        if (tenantId == null || !UUID_PATTERN.matcher(tenantId).matches()) {
            throw new IllegalArgumentException("tenantId is not a valid UUID: " + tenantId);
        }
        if (providerId == null || !PROVIDER_ID_PATTERN.matcher(providerId).matches()) {
            throw new IllegalArgumentException("providerId is invalid: " + providerId);
        }
        return new TenantOidcRegistrationId(tenantId, providerId);
    }

    /**
     * registrationId 文字列をパースして値オブジェクトに変換する。
     *
     * @param registrationId registrationId 文字列
     * @return パース結果。形式不正の場合は空
     */
    public static Optional<TenantOidcRegistrationId> parse(String registrationId) {
        if (registrationId == null || !registrationId.startsWith(PREFIX)) {
            return Optional.empty();
        }
        // "tenant-" + UUID(36) + "-" + providerId(>=1)
        int minLength = PREFIX.length() + UUID_LENGTH + 1 + 1;
        if (registrationId.length() < minLength) {
            return Optional.empty();
        }
        int uuidStart = PREFIX.length();
        int uuidEnd = uuidStart + UUID_LENGTH;
        // UUID直後はハイフン区切りであること
        if (registrationId.charAt(uuidEnd) != '-') {
            return Optional.empty();
        }
        String tenantId = registrationId.substring(uuidStart, uuidEnd);
        String providerId = registrationId.substring(uuidEnd + 1);

        if (!UUID_PATTERN.matcher(tenantId).matches()) {
            return Optional.empty();
        }
        if (!PROVIDER_ID_PATTERN.matcher(providerId).matches()) {
            return Optional.empty();
        }
        return Optional.of(new TenantOidcRegistrationId(tenantId, providerId));
    }

    /**
     * registrationId 文字列表現を返す。
     *
     * @return {@code tenant-{tenantId}-{providerId}} 形式の文字列
     */
    public String value() {
        return PREFIX + tenantId + SEPARATOR + providerId;
    }

}
