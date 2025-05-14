package me.hysong.apis.kstrader.v1.strategy;

public interface TraderStrategyManifestV1 {

    String getStrategyName();
    String getStrategyVersion();
    boolean isForWS();
    boolean isForREST();


}
