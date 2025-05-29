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
import me.hysong.kynesystems.apps.kstradermachine.front.uiobjects.DaemonPanel;
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

    private Thread worker; // No longer final, will be re-instantiated

    // Extracted Runnable logic for the worker thread
    private final Runnable workerLogic = () -> {
        // terminateQueued is set to false by the start() method before creating this thread
        SystemLogs.log("INFO", "Worker thread started for slot " + cfg.getSlot());
        try {
            while (!terminateQueued) {
                SystemLogs.log("INFO", "Running slot " + cfg.getSlot() + "...");
                if (driverManifest == null || strategyManifest == null) {
                    SystemLogs.log("ERROR", "Daemon " + cfg.getSlot() + " tried to run while driverManifest or strategyManifest is null. Terminating worker.");
                    break;
                }

                String preferenceFilePath = Application.storagePath + "/configs/drivers/" + driverManifest.getFileSystemIdentifier() + ".json";
                String preference = MFS1.readString(preferenceFilePath);
                if (preference == null) {
                    SystemLogs.log("ERROR", "Driver configuration not found for slot " + cfg.getSlot() + " at " + preferenceFilePath + ". Please configure driver first. Terminating worker.");
                    // Update UI to show an error state for this daemon might be good here if possible
                    // SwingUtilities.invokeLater(() -> Application.currentInstance.getDaemonStatusPanels().get(slot).setStatus(DaemonPanel.DaemonStatusOutlook.ERROR));
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
                    } catch (InterruptedException e) {
                        SystemLogs.log("INFO", "REST Daemon " + cfg.getSlot() + " worker interrupted during sleep. Terminating.");
                        Thread.currentThread().interrupt(); // Restore interrupt status
                        break; // Exit loop if interrupted
                    } catch (Exception e) {
                        SystemLogs.log("ERROR", "Exception in REST Daemon " + cfg.getSlot() + ": " + e.toString());
                        // Depending on severity, you might want to break or continue
                        // For now, let's assume it might be recoverable or part of strategy
                    }
                } else if (strategyManifest.isForWS()) {
                    WSStrategyV1 wsStrat = strategyManifest.getWSStrategy();
                    try {
                        wsStrat.loop(account, cfg.getSymbol().split(","), driverManifest, driverManifest.getDriver());
                        // WSStrategy loop should ideally handle its own latency or blocking.
                        // If wsStrat.loop() is blocking and checks terminateQueued, Thread.sleep might not be needed.
                        // If it's non-blocking and needs a pause, this sleep is okay.
                        Thread.sleep((long) (wsStrat.getPreferredLatency() * 1000));
                    } catch (InterruptedException e) {
                        SystemLogs.log("INFO", "WS Daemon " + cfg.getSlot() + " worker interrupted during sleep. Terminating.");
                        Thread.currentThread().interrupt(); // Restore interrupt status
                        break; // Exit loop if interrupted
                    } catch (Exception e) {
                        SystemLogs.log("ERROR", "Exception in WS Daemon " + cfg.getSlot() + ": " + e.toString());
                    }
                } else {
                    SystemLogs.log("ERROR", "Daemon " + cfg.getSlot() + " has a strategy that supports neither REST nor WS. Terminating worker.");
                    break;
                }
            }
        } catch (Exception e) {
            // Catch any unexpected exceptions from the loop logic itself
            SystemLogs.log("FATAL_ERROR", "Unhandled exception in worker thread for slot " + cfg.getSlot() + ": " + e.toString());
            // Consider updating UI to an error state
            // SwingUtilities.invokeLater(() -> Application.currentInstance.getDaemonStatusPanels().get(slot).setStatus(DaemonPanel.DaemonStatusOutlook.ERROR));
        } finally {
            SystemLogs.log("INFO", "Worker thread for slot " + cfg.getSlot() + " has finished.");
            // UI status to NOT_RUNNING is primarily handled by the terminate() method
            // or if start() fails. If the thread exits due to an internal error (break in loop),
            // isRunning() will become false. The UI might show OPERATING until a new action.
            // If terminateQueued is false here, it means an abnormal exit.
            if (!terminateQueued) {
                SystemLogs.log("WARNING", "Worker for slot " + cfg.getSlot() + " exited without termination being queued (e.g. internal error).");
                // Ensure the UI reflects that it's not running if it stopped unexpectedly
                // Check if this is being called from terminate path or natural error exit
                // To avoid race conditions with terminate() updating the UI, be careful.
                // A robust solution might involve a state machine or ensuring terminate() always finalizes the UI.
                // For now, let terminate() be the main source of NOT_RUNNING status.
                if (Application.currentInstance != null && Application.currentInstance.getDaemonStatusPanels().get(cfg.getSlot()) != null) {
                    // If it stopped on its own due to error, reflect it.
                    // But only if not currently being terminated by terminate() method.
                    // This check can be complex. Let's assume terminate() or a new start() will fix UI.
                }
            }
        }
    };

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
            this.driverManifest = null; // Ensure it's null if config fails
            return;
        }
        try {
            this.driverManifest = (TraderDriverManifestV1) drv.getConstructor().newInstance();
        } catch (Exception e) {
            SystemLogs.log("ERROR", "Slot " + slot + " failed to instantiate driver " + cfg.getExchangeDriverClass() + ": " + e.getMessage());
            e.printStackTrace();
            this.driverManifest = null;
        }

        Class<?> stg = Drivers.strategies.get(cfg.getStrategyName());
        if (stg == null) {
            SystemLogs.log("WARNING", "Slot " + slot + " failed to configure due to absent strategy: " + cfg.getStrategyName());
            this.strategyManifest = null; // Ensure it's null
            return;
        }
        try {
            this.strategyManifest = (TraderStrategyManifestV1) stg.getConstructor().newInstance();
        } catch (Exception e) {
            SystemLogs.log("ERROR", "Slot " + slot + " failed to instantiate strategy " + cfg.getStrategyName() + ": " + e.getMessage());
            e.printStackTrace();
            this.strategyManifest = null;
        }
    }

    public void reloadPreference() {
        if (cfg != null) {
            cfg.reload();
            configure(cfg);
        } else {
            SystemLogs.log("WARNING", "Cannot reload preference for slot " + slot + ": DaemonCfg is null.");
        }
    }

    public boolean isRunning() {
        return worker != null && worker.isAlive();
    }

    public void start() {
        if (isRunning()) {
            SystemLogs.log("WARNING", "Slot " + slot + " is already running.");
            return;
        }

        if (driverManifest == null || strategyManifest == null) {
            SystemLogs.log("ERROR", "Cannot start daemon for slot " + slot + ": Driver or Strategy is not configured/loaded.");
            // Update UI to show an error or not running state
            if (Application.currentInstance != null && Application.currentInstance.getDaemonStatusPanels().get(slot) != null) {
                SwingUtilities.invokeLater(() -> Application.currentInstance.getDaemonStatusPanels().get(slot).setStatus(DaemonPanel.DaemonStatusOutlook.ERROR)); // Or NOT_RUNNING
            }
            return;
        }

        SystemLogs.log("INFO", "Starting worker for slot " + slot + "...");
        // UI updates should generally be on the EDT. Since start() is called from EDT (based on your exception), this is fine.
        Application.currentInstance.getDaemonStatusPanels().get(slot).setStatus(DaemonPanel.DaemonStatusOutlook.STARTING_UP);

        this.terminateQueued = false; // Reset flag for the new run
        this.worker = new Thread(workerLogic, "DaemonWorker-Slot-" + slot); // Create a NEW Thread instance

        try {
            this.worker.start(); // Start the new thread
            Application.currentInstance.getDaemonStatusPanels().get(slot).setStatus(DaemonPanel.DaemonStatusOutlook.OPERATING);
            SystemLogs.log("INFO", "Worker for slot " + slot + " started successfully.");
        } catch (IllegalThreadStateException itse) {
            // This should not happen with the new logic, but as a safeguard:
            SystemLogs.log("CRITICAL_ERROR", "IllegalThreadStateException during start for slot " + slot + ". This indicates a bug in daemon lifecycle management. " + itse.getMessage());
            Application.currentInstance.getDaemonStatusPanels().get(slot).setStatus(DaemonPanel.DaemonStatusOutlook.ERROR);
        } catch (Exception e) {
            SystemLogs.log("ERROR", "Failed to start worker thread for slot " + slot + ": " + e.toString());
            Application.currentInstance.getDaemonStatusPanels().get(slot).setStatus(DaemonPanel.DaemonStatusOutlook.ERROR);
        }
    }

    public void terminate() {
        if (!isRunning() && !terminateQueued) { // If not running and not already in the process of terminating
            SystemLogs.log("INFO", "Worker for slot " + slot + " is not running or termination already handled.");
            // Ensure UI consistency
            if (Application.currentInstance != null && Application.currentInstance.getDaemonStatusPanels().get(slot) != null) {
                SwingUtilities.invokeLater(() -> Application.currentInstance.getDaemonStatusPanels().get(slot).setStatus(DaemonPanel.DaemonStatusOutlook.NOT_RUNNING));
            }
            return;
        }

        if (terminateQueued && !isRunning()) { // Already signaled to terminate and thread is dead
            SystemLogs.log("INFO", "Worker for slot " + slot + " already terminated/terminating.");
            if (Application.currentInstance != null && Application.currentInstance.getDaemonStatusPanels().get(slot) != null) {
                SwingUtilities.invokeLater(() -> Application.currentInstance.getDaemonStatusPanels().get(slot).setStatus(DaemonPanel.DaemonStatusOutlook.NOT_RUNNING));
            }
            return;
        }


        SystemLogs.log("INFO", "Attempting to terminate worker for slot " + slot + ".");
        this.terminateQueued = true; // Signal the worker loop to stop

        // Create a new thread to handle waiting for termination and interrupting if necessary.
        // This prevents blocking the caller of terminate() (e.g., the EDT).
        new Thread(() -> {
            if (worker != null) { // Check if worker was ever initialized
                try {
                    // Wait for the worker thread to die gracefully by checking the 'terminateQueued' flag
                    worker.join(3000); // Wait up to 3 seconds
                } catch (InterruptedException e) {
                    SystemLogs.log("WARNING", "Termination thread for slot " + slot + " interrupted while waiting for worker to join. Worker might need forceful interruption.");
                    Thread.currentThread().interrupt(); // Preserve interrupt status
                }

                if (worker.isAlive()) {
                    SystemLogs.log("WARNING", "Worker thread for slot " + slot + " did not terminate gracefully after 3s. Interrupting...");
                    worker.interrupt(); // Forcefully interrupt if still alive
                    try {
                        worker.join(1000); // Wait a bit longer for the interruption to take effect
                    } catch (InterruptedException e) {
                        SystemLogs.log("WARNING", "Interrupted while waiting for forceful termination of worker in slot " + slot);
                        Thread.currentThread().interrupt();
                    }
                }

                if (worker.isAlive()) {
                    SystemLogs.log("ERROR", "Worker thread for slot " + slot + " could not be terminated even after interrupt.");
                    // UI might show OPERATING or ERROR depending on desired behavior for unkillable threads
                } else {
                    SystemLogs.log("INFO", "Worker thread for slot " + slot + " successfully terminated.");
                }
            } else {
                SystemLogs.log("INFO", "Worker for slot " + slot + " was null during termination, assuming already stopped.");
            }

            // UI updates from a non-EDT thread must use SwingUtilities.invokeLater
            SwingUtilities.invokeLater(() -> {
                if (Application.currentInstance != null && Application.currentInstance.getDaemonStatusPanels().get(slot) != null) {
                    Application.currentInstance.getDaemonStatusPanels().get(slot).setStatus(DaemonPanel.DaemonStatusOutlook.NOT_RUNNING);
                }
            });
            SystemLogs.log("INFO", "Termination process for slot " + slot + " finalized.");
            // Do NOT set terminateQueued = false here. 'start()' method will reset it for a new run.
        }, "DaemonTerminator-Slot-" + slot).start();
    }
}
