package me.hysong.kstrader.strategies.generalv1overws;

import lombok.Getter;
import me.hysong.apis.kstrader.v1.strategy.RESTStrategyV1;
import me.hysong.apis.kstrader.v1.strategy.StrategySettingsV1;
import me.hysong.apis.kstrader.v1.strategy.TraderStrategyManifestV1;
import me.hysong.apis.kstrader.v1.strategy.WSStrategyV1;

import java.util.HashMap;

@Getter
public class GeneralV1OverWSManifestV1 implements TraderStrategyManifestV1 {

    private final String strategyName = "GeneralV1OverWS";
    private final String strategyVersion = "1.0.0";
    private final boolean forWS = true;
    private final boolean forREST = false;
    private final boolean supportOrderAsMarket = true;
    private final boolean supportOrderAsLimit = true;
    private final boolean supportFuture = false;
    private final boolean supportOption = false;
    private final boolean supportPerpetual = false;
    private final boolean supportSpot = true;


    @Override
    public StrategySettingsV1 parseSettings(HashMap<String, Object> settings) {
        return new GeneralV1OverWSSettings(settings);
    }

    @Override
    public RESTStrategyV1 getRESTStrategy() {
        return null;
    }

    @Override
    public WSStrategyV1 getWSStrategy() {
        return new GeneralV1OverWSV1();
    }

}
