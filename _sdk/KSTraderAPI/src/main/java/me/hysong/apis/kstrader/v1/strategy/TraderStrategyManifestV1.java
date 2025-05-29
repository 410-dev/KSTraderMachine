package me.hysong.apis.kstrader.v1.strategy;

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

    RESTStrategyV1 getRESTStrategy();
    WSStrategyV1 getWSStrategy();


}
