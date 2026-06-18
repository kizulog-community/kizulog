package io.github.kizulog_community.kizulog.infrastructure.timezone;

import org.springframework.stereotype.Component;

import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.shared.TenantTimezoneResolver;

/**
 * TenantTimezoneResolverのスタブ実装
 *
 * @author Jun Kobayashi
 */
@Component
public class DefaultTenantTimezoneResolver implements TenantTimezoneResolver {

    /** MVPの既定タイムゾーン */
    private static final SupportedTimezone DEFAULT_TIMEZONE = SupportedTimezone.of("Asia/Tokyo");

    /**
     * {@inheritDoc}
     */
    @Override
    public SupportedTimezone resolve(String tenantId, String accountId) {
        return DEFAULT_TIMEZONE;
    }

}
