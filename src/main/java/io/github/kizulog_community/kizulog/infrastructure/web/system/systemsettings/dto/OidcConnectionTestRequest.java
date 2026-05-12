package io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OIDC接続確認APIリクエスト
 *
 * <p>追加・編集フォームで「接続確認」ボタン押下時に
 * フロント→バックエンドへ送信されるリクエストデータ。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
@NoArgsConstructor
public class OidcConnectionTestRequest {

    /** OIDC Issuer URI */
    private String uri;

}
