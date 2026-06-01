package io.github.kizulog_community.kizulog.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * OIDCプロファイルクレーム設定プロパティ
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "kizulog.oidc.profile")
public class OidcProfileClaimsProperties {

    /** キャッシュに保存するクレームのホワイトリスト */
    private List<String> cachedClaims = new ArrayList<>(List.of(
            "name",
            "given_name",
            "family_name",
            "middle_name",
            "nickname",
            "preferred_username",
            "email",
            "email_verified",
            "locale",
            "zoneinfo",
            "picture",
            "updated_at",
            "organization"));

}
