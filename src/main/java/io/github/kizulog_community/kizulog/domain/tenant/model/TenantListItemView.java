package io.github.kizulog_community.kizulog.domain.tenant.model;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * テナント一覧画面の1行表示用ビュー
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantListItemView {

    /** テナントID */
    private final String tenantId;

    /** テナント名 */
    private final String name;

    /** URL用slug */
    private final String slug;

    /** 現在のステータス */
    private final TenantStatusValue currentStatus;

    /** 作成日時 */
    private final OffsetDateTime createdAt;

    /** 現在ACTIVEなhost一覧（表示用、非ACTIVEは含まない） */
    private final List<String> activeHosts;

    /** 過去含む全host数 */
    private final int totalHostCount;

}
