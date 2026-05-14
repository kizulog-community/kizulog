package io.github.kizulog_community.kizulog.domain.systemconfig.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import lombok.Getter;

/**
 * タイムゾーン設定ドメインモデル
 *
 * <p>システム全体のタイムゾーン設定を表現するオブジェクト。
 * デフォルトタイムゾーンと利用可能タイムゾーンのリストで構成される。</p>
 *
 * <p>不変条件：
 * <ul>
 * <li>デフォルトタイムゾーンは必須（null不可）</li>
 * <li>利用可能タイムゾーンリストは1つ以上必須</li>
 * <li>デフォルトタイムゾーンは利用可能タイムゾーンリストに含まれる必要がある</li>
 * </ul>
 * </p>
 *
 * @author Jun Kobayashi
 */
@Getter
public class TimezoneSetting {

    /** デフォルトタイムゾーン */
    private final SupportedTimezone defaultTimezone;

    /** 利用可能タイムゾーンリスト（不変リスト） */
    private final List<SupportedTimezone> availableTimezones;

    /**
     * コンストラクタ
     *
     * @param defaultTimezone デフォルトタイムゾーン
     * @param availableTimezones 利用可能タイムゾーンリスト
     */
    public TimezoneSetting(
            SupportedTimezone defaultTimezone,
            List<SupportedTimezone> availableTimezones) {
        this.defaultTimezone = defaultTimezone;
        this.availableTimezones = (availableTimezones == null)
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(availableTimezones));
    }

}
