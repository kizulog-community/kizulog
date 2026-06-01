package io.github.kizulog_community.kizulog.infrastructure.web.footer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * アプリケーションフッタAdvice
 *
 * @author Jun Kobayashi
 */
@ControllerAdvice
public class ApplicationFooterAdvice {

    /** コミュニティ名（固定） */
    static final String COMMUNITY_NAME = "KizuLog Community";

    /** ライセンス名（固定） */
    static final String LICENSE_NAME = "Apache License 2.0";

    /** Model投入時の属性名 */
    static final String MODEL_ATTRIBUTE_NAME = "applicationFooter";

    /** BuildProperties取得用Provider */
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    /** メッセージソース */
    @SuppressWarnings("unused")
    private final MessageSource messageSource;

    /**
     * コンストラクタ
     *
     * @param buildPropertiesProvider BuildProperties Beanプロバイダ
     * @param messageSource メッセージソース
     */
    public ApplicationFooterAdvice(
            ObjectProvider<BuildProperties> buildPropertiesProvider,
            MessageSource messageSource) {
        this.buildPropertiesProvider = buildPropertiesProvider;
        this.messageSource = messageSource;
    }

    /**
     * 全画面共通のフッタ情報をModelに投入する。
     *
     * @return フッタ表示用View
     */
    @ModelAttribute(MODEL_ATTRIBUTE_NAME)
    public ApplicationFooterView applicationFooter() {
        @SuppressWarnings("unused")
        var locale = LocaleContextHolder.getLocale();

        // build-info.propertiesが無い環境では空文字列を返す
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        String version = (buildProperties != null) ? buildProperties.getVersion() : "";

        return new ApplicationFooterView(COMMUNITY_NAME, version, LICENSE_NAME);
    }

}
