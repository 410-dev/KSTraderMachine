package me.hysong.apis.kstrader.v1.strategy;

import me.hysong.apis.kstrader.v1.driver.TraderDriverManifestV1;
import me.hysong.apis.kstrader.v1.driver.TraderDriverV1;
import me.hysong.apis.kstrader.v1.objects.Account;

public interface RESTStrategyV1 {

    void start(Account account, String[] symbols, TraderDriverManifestV1 driverManifest, TraderDriverV1 driver) throws Exception;

}
