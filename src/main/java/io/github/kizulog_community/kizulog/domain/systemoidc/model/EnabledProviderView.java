package io.github.kizulog_community.kizulog.domain.systemoidc.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ログイン画面表示用のOIDCプロバイダービュー
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class EnabledProviderView {

    /** プロバイダーID */
    private final String providerId;

    /** 表示名（ボタンのラベル） */
    private final String displayName;

}