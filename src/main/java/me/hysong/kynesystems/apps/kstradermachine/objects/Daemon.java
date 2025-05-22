package me.hysong.kynesystems.apps.kstradermachine.objects;

import lombok.Getter;
import me.hysong.apis.kstrader.v1.driver.TraderDriverManifestV1;
import me.hysong.apis.kstrader.v1.strategy.TraderStrategyManifestV1;

@Getter
public class Daemon {

    private final int slot;
    private DaemonCfg cfg;

    private TraderStrategyManifestV1 strategyManifest;
    private TraderDriverManifestV1 driverManifest;

    public Daemon(int slot, DaemonCfg daemonCfg) {
        configure(daemonCfg);
        this.slot = slot;
        cfg.setSlot(slot);
    }

    public void configure(DaemonCfg daemonCfg) {
        this.cfg = daemonCfg;
    }

    public void reloadPreference() {
        cfg.reload();
    }

    public boolean isRunning() {
        return false;
    }
}
