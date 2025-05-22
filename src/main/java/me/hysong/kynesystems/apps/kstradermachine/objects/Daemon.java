package me.hysong.kynesystems.apps.kstradermachine.objects;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import me.hysong.apis.kstrader.v1.driver.TraderDriverManifestV1;
import me.hysong.apis.kstrader.v1.objects.Account;
import me.hysong.apis.kstrader.v1.strategy.RESTStrategyV1;
import me.hysong.apis.kstrader.v1.strategy.TraderStrategyManifestV1;
import me.hysong.apis.kstrader.v1.strategy.WSStrategyV1;
import me.hysong.atlas.utils.MFS1;
import me.hysong.kynesystems.apps.kstradermachine.Application;
import me.hysong.kynesystems.apps.kstradermachine.backend.Drivers;
import me.hysong.kynesystems.apps.kstradermachine.subwins.SystemLogs;

import javax.swing.*;
import java.sql.Driver;

@Getter
public class Daemon {

    private final int slot;
    private DaemonCfg cfg;

    private TraderStrategyManifestV1 strategyManifest;
    private TraderDriverManifestV1 driverManifest;

    private boolean terminateQueued = false;
    private Thread worker = new Thread(() -> {
        while (!terminateQueued) {
            if (driverManifest == null || strategyManifest == null) {
                SystemLogs.log("ERROR", "Daemon " + cfg.getSlot() + " tried to start / run while one of (or both) driverManifest or strategyManifest is null.");
                break;
            }

            String preference = MFS1.readString(Application.storagePath + "/configs/drivers/" + driverManifest.getDriverExchange() + "@" + driverManifest.getDriverAPIEndpoint().replace("/", "_").replace(":", "_"));
            if (preference == null) {
                SystemLogs.log("ERROR", "Driver does not have configuration ready. Please configure driver first.");
                break;
            }
            JsonObject prefObject = JsonParser.parseString(preference).getAsJsonObject();
            if (prefObject.has("settings")) {
                prefObject = prefObject.get("settings").getAsJsonObject();
            }
            Account account = driverManifest.getAccount(cfg.getTraderMode(), prefObject);

            if (strategyManifest.isForREST()) {
                RESTStrategyV1 restStrat = strategyManifest.getRESTStrategy();
                try {
                    restStrat.start(account, cfg.getSymbol().split(","), driverManifest, driverManifest.getDriver());
                    Thread.sleep((long) (restStrat.getPreferredLatency() * 1000));
                } catch (Exception e) {
                    SystemLogs.log("ERROR", "Exception detected in REST Daemon " + cfg.getSlot() + ": " + e.toString());
                }
            } else if (strategyManifest.isForWS()) {
                WSStrategyV1 wsStrat = strategyManifest.getWSStrategy();
                try {
                    wsStrat.loop(account, cfg.getSymbol().split(","), driverManifest, driverManifest.getDriver());
                    Thread.sleep((long) (wsStrat.getPreferredLatency() * 1000));
                } catch (Exception e) {
                    SystemLogs.log("ERROR", "Exception detected in WS Daemon " + cfg.getSlot() + ": " + e.toString());
                }
            } else {
                SystemLogs.log("ERROR", "Daemon " + cfg.getSlot() + " has a strategy that supports neither REST or WS. The daemon is terminating.");
                break;
            }
        }
        terminateQueued = false;
    });

    public Daemon(int slot, DaemonCfg daemonCfg) {
        this.slot = slot;
        daemonCfg.setSlot(slot);
        configure(daemonCfg);
    }

    public void configure(DaemonCfg daemonCfg) {
        this.cfg = daemonCfg;
        Class<?> drv = Drivers.drivers.get(cfg.getExchangeDriverClass());
        if (drv == null) {
            SystemLogs.log("WARNING", "Slot " + slot + " failed to configure due to absent driver: " + cfg.getExchangeDriverClass());
            return;
        }
        try {
            this.driverManifest = (TraderDriverManifestV1) drv.getConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Class<?> stg = Drivers.strategies.get(cfg.getStrategyName());
        if (stg == null) {
            SystemLogs.log("WARNING", "Slot " + slot + " failed to configure due to absent strategy: " + cfg.getStrategyName());
            return;
        }
        try {
            this.strategyManifest = (TraderStrategyManifestV1) stg.getConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reloadPreference() {
        cfg.reload();
        configure(cfg);
    }

    public boolean isRunning() {
        return worker.isAlive();
    }

    public void start() {
        if (isRunning()) {
            return;
        }
        terminateQueued = false;
        worker.start();
    }

    public void terminate() {
        new Thread(() -> {
            terminateQueued = true;
            try {
                Thread.sleep(3000);
            } catch (Exception ignored) {}
            if (worker.isAlive()) {
                worker.interrupt();
            }
            terminateQueued = false;
        }).start();
    }
}
