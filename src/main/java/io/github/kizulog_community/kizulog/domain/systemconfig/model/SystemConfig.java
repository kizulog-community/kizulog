package io.github.kizulog_community.kizulog.domain.systemconfig.model;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * システム設定ドメインモデル
 *
 * <p>システム全体の設定を表現する。
 * 設定はグループ単位でJSON形式で保持する。</p>
 *
 * <p>初期セットアップ時はOIDC設定のみウィザード形式で登録する。
 * その他の設定はAPI経由で登録・変更等を行う。</p>
 *
 * <p>Immutableテーブルに対応するため、フィールドは全てfinalとする。
 * 変更が必要な場合は新しいインスタンスを生成してINSERTする。</p>
 *
 * <p>バージョン管理：同一keyの中でversionが最大のレコードが有効値となる。
 * versionにはタイムスタンプが入る。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class SystemConfig {

    /** 設定グループ名 */
    private final String key;

    /** バージョン */
    private final OffsetDateTime version;

    /**
     * 設定値（JSON文字列）
     *
     * <p>機密情報はAES-256-GCMで暗号化した文字列を格納する。</p>
     */
    private final String value;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /**
     * 作成者
     *
     * <p>操作者の種別をプレフィックスで識別する。</p>
     * <ul>
     * <li>ユーザー操作：{@code user:uuid}</li>
     * <li>バッチ処理：{@code batch:バッチ名}</li>
     * <li>システム：{@code system:処理名}</li>
     * </ul>
     */
    private final String createdBy;

}