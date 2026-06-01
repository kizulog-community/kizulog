package io.github.kizulog_community.kizulog.infrastructure.web.setup;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.kizulog_community.kizulog.domain.systemoidc.model.ClaimsMappingTarget;
import lombok.Getter;
import lombok.Setter;

/**
 * Step3.5（クレームマッピング設定）フォームデータ
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
public class Step3_5FormData implements Serializable {

    private static final long serialVersionUID = 1L;

    /** クレームマッピング設定 */
    private Map<String, String> claimsMapping =
            new LinkedHashMap<>(ClaimsMappingTarget.defaultMapping());

}
