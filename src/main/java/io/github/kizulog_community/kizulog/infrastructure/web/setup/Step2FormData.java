package io.github.kizulog_community.kizulog.infrastructure.web.setup;

import lombok.Getter;
import lombok.Setter;

/**
 * Step2フォームデータ
 *
 * <p>Step2のフォーム送信データを受け取るDTO。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
public class Step2FormData {

    /** ホスト名 */
    private String host;

    /** OIDC設定 */
    private OidcSetting oidcSetting = new OidcSetting();

}