package io.github.kizulog_community.kizulog.infrastructure.web.footer;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * アプリケーションフッターView
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class ApplicationFooterView {

    /** コミュニティ名 */
    private final String communityName;

    /** アプリケーションバージョン */
    private final String version;

    /** ライセンス名 */
    private final String licenseName;

}
