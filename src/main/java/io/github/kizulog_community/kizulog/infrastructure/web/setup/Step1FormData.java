package io.github.kizulog_community.kizulog.infrastructure.web.setup;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Step1フォーム
 *
 * <p>Step1のフォーム送信データを受け取るDTO。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
public class Step1FormData {

    /** OIDC設定リスト */
    private List<OidcSetting> oidcSettings = new ArrayList<>();

}