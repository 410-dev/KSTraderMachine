package me.hysong.kstrader.strategies.generalv1overws;

import lombok.Getter;
import me.hysong.apis.kstrader.v1.strategy.TraderStrategyManifestV1;

@Getter
public class GeneralV1OverWSManifestV1 implements TraderStrategyManifestV1 {

    private final String strategyName = "GeneralV1OverWS";
    private final String strategyVersion = "1.0.0";
    private final boolean forWS = true;
    private final boolean forREST = false;

}
