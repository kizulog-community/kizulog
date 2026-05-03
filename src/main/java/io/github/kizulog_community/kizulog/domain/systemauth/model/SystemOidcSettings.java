package io.github.kizulog_community.kizulog.domain.systemauth.model;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import lombok.Getter;

/**
 * システム管理OIDC設定群ドメインモデル
 *
 * <p>system_config（key="OIDC"）の最新バージョンに含まれる
 * すべてのOIDC設定とそのバージョン情報を保持する不変オブジェクト。</p>
 *
 * <p>バージョン情報は、認証フロー側でClientRegistrationのキャッシュ
 * 有効性判定に使用される。同一バージョンであれば同一内容と判断できる。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
public final class SystemOidcSettings {

    /** バージョン（system_config.version相当） */
    private final OffsetDateTime version;

    /** OIDC設定リスト（不変） */
    private final List<SystemOidcSetting> settings;

    /**
     * コンストラクタ
     *
     * @param version バージョン
     * @param settings OIDC設定リスト
     */
    public SystemOidcSettings(OffsetDateTime version, List<SystemOidcSetting> settings) {
        this.version = Objects.requireNonNull(version, "version must not be null");
        Objects.requireNonNull(settings, "settings must not be null");
        this.settings = Collections.unmodifiableList(List.copyOf(settings));
    }

}
