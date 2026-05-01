package io.github.kizulog_community.kizulog.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.SystemConfigService;
import io.github.kizulog_community.kizulog.infrastructure.web.setup.SetupCheckFilter;
import lombok.RequiredArgsConstructor;

/**
 * フィルター設定クラス
 *
 * @author Jun Kobayashi
 */
@Configuration
@RequiredArgsConstructor
public class FilterConfig {

    /** システム設定サービス */
    private final SystemConfigService systemConfigService;

    /**
     * セットアップチェックフィルターの登録
     *
     * @return フィルター登録Bean
     */
    @Bean
    FilterRegistrationBean<SetupCheckFilter> setupCheckFilterRegistration() {
        SetupCheckFilter filter = new SetupCheckFilter(systemConfigService);
        FilterRegistrationBean<SetupCheckFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }

}