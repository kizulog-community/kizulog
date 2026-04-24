package io.github.kizulog_community.kizulog.domain.systemconfig.port;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.SystemConfig;

/**
 * システム設定リポジトリインターフェース（Output Port）.
 *
 * <p>システム設定の永続化操作を定義する。
 * 実装はインフラ層が担い、ドメイン層はこのインターフェースのみ参照する。</p>
 *
 * <p>system_configテーブルはimmutableテーブルのため、
 * 更新・削除操作は提供しない。変更は新バージョンのINSERTで行う。</p>
 */
public interface SystemConfigRepository {

    /**
     * 指定されたキーの最新バージョンを取得する.
     *
     * <p>同一キーの中でversionが最大のレコードを返す。</p>
     *
     * @param key 設定グループ名（例：OIDC、SYSTEM）
     * @return 最新バージョンのシステム設定。存在しない場合は空のOptional
     */
    Optional<SystemConfig> findLatestByKey(String key);

    /**
     * 指定されたキーと指定バージョンのシステム設定を取得する.
     *
     * <p>特定時点の設定値参照・監査用途で使用する。</p>
     *
     * @param key     設定グループ名（例：OIDC、SYSTEM）
     * @param version バージョン（TIMESTAMPTZ）
     * @return 指定バージョンのシステム設定。存在しない場合は空のOptional
     */
    Optional<SystemConfig> findByKeyAndVersion(String key, OffsetDateTime version);

    /**
     * 指定されたキーの全バージョンを取得する.
     *
     * <p>変更履歴の確認・管理画面表示用途で使用する。
     * 並び順はアプリケーション層で制御する。</p>
     *
     * @param key 設定グループ名（例：OIDC、SYSTEM）
     * @return 指定キーの全バージョンのリスト。存在しない場合は空のリスト
     */
    List<SystemConfig> findAllByKey(String key);

    /**
     * システム設定を保存する.
     *
     * <p>system_configテーブルはimmutableのためINSERTのみ実行する。
     * 既存レコードの更新・削除は行わない。</p>
     *
     * @param systemConfig 保存するシステム設定
     */
    void save(SystemConfig systemConfig);
}