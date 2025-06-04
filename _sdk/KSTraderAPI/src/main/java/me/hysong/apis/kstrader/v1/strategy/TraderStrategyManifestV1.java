package me.hysong.apis.kstrader.v1.strategy;

import java.util.HashMap;
import java.util.Objects;

public interface TraderStrategyManifestV1 {

    String getStrategyName();
    String getStrategyVersion();
    boolean isForWS();
    boolean isForREST();
    boolean isSupportSpot();
    boolean isSupportFuture();
    boolean isSupportOption();
    boolean isSupportPerpetual();
    boolean isSupportOrderAsLimit();
    boolean isSupportOrderAsMarket();
    StrategySettingsV1 parseSettings(HashMap<String, Object> settings);

    RESTStrategyV1 getRESTStrategy();
    WSStrategyV1 getWSStrategy();


}
