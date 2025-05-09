package me.hysong.apis.kstrader.v1.strategy;

import me.hysong.apis.kstrader.v1.driver.TraderDriver;
import me.hysong.apis.kstrader.v1.driver.TraderDriverManifest;
import me.hysong.apis.kstrader.v1.objects.Account;

public interface RESTStrategy {

    void start(Account account, String[] symbols, TraderDriverManifest driverManifest, TraderDriver driver) throws Exception;

}
