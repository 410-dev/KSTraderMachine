package me.hysong.apis.kstrader.v1.strategy;

public interface TraderStrategyManifest {

    String getStrategyName();
    String getStrategyVersion();
    boolean isForWS();
    boolean isForREST();


}
