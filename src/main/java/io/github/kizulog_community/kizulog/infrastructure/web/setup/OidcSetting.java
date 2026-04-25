package io.github.kizulog_community.kizulog.infrastructure.web.setup;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * OIDC設定データ
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
public class OidcSetting implements Serializable {

    private static final long serialVersionUID = 1L;

	/** OIDC識別子 */
    private String id;

    /** URI */
    private String uri;

    /** Client ID. */
    private String clientId;

    /** Client Secret */
    private String clientSecret;
    
}