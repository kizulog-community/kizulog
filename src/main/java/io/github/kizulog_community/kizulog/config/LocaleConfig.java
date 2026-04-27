package io.github.kizulog_community.kizulog.config;

import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

/**
 * ロケール設定クラス
 *
 * <p>セッション単位で言語を切り替えるための設定を行う。
 * デフォルトロケールは日本語とする。</p>
 *
 * @author Jun Kobayashi
 */
@Configuration
public class LocaleConfig {

    /**
     * セッションベースのLocaleResolverを登録
     *
     * <p>セッションに保存された言語設定を使用する。
     * セッションに言語設定がない場合は日本語をデフォルトとする。</p>
     *
     * @return LocaleResolver
     */
	@Bean
	public SessionLocaleResolver localeResolver() {
	    SessionLocaleResolver resolver = new SessionLocaleResolver();
	    resolver.setDefaultLocale(Locale.JAPANESE);
	    return resolver;
	}

}