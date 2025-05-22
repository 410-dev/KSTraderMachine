package me.hysong.kstrader.strategies.generalv1overws;

import me.hysong.apis.kstrader.v1.driver.TraderDriverManifestV1;
import me.hysong.apis.kstrader.v1.driver.TraderDriverV1;
import me.hysong.apis.kstrader.v1.objects.Account;
import me.hysong.apis.kstrader.v1.strategy.WSStrategyV1;

public class GeneralV1OverWSV1 implements WSStrategyV1 {
    @Override
    public void loop(Account account, String[] symbols, TraderDriverManifestV1 driverManifest, TraderDriverV1 driver) throws Exception {

    }
}
