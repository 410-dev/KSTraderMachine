package me.hysong.kstrader.strategies.generalv1overws;

import lombok.Getter;
import me.hysong.apis.kstrader.v1.strategy.RESTStrategyV1;
import me.hysong.apis.kstrader.v1.strategy.TraderStrategyManifestV1;
import me.hysong.apis.kstrader.v1.strategy.WSStrategyV1;

@Getter
public class GeneralV1OverWSManifestV1 implements TraderStrategyManifestV1 {

    private final String strategyName = "GeneralV1OverWS";
    private final String strategyVersion = "1.0.0";
    private final boolean forWS = true;
    private final boolean forREST = false;


    @Override
    public RESTStrategyV1 getRESTStrategy() {
        return null;
    }

    @Override
    public WSStrategyV1 getWSStrategy() {
        return new GeneralV1OverWSV1();
    }
}
