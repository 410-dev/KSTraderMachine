package me.hysong.kynesystems.apps.kstradermachine;

import lombok.Getter;
import me.hysong.apis.kstrader.v1.driver.TraderDriverManifestV1;
import me.hysong.apis.kstrader.v1.objects.DriverExitCode;
import me.hysong.apis.kstrader.v1.strategy.TraderStrategyManifestV1;
import me.hysong.atlas.interfaces.KSApplication;
import me.hysong.atlas.sdk.graphite.v1.GPSplashWindow;
import me.hysong.atlas.sdk.graphite.v1.GraphiteProgramLauncher;
import me.hysong.atlas.sdk.graphite.v1.KSGraphicalApplication;
import me.hysong.atlas.sharedobj.KSEnvironment;
import me.hysong.atlas.utils.MFS1;
import me.hysong.kynesystems.apps.kstradermachine.backend.Drivers;
import me.hysong.kynesystems.apps.kstradermachine.backend.objects.TraderDaemon;
import me.hysong.kynesystems.apps.kstradermachine.backend.startup.StorageSetupTool;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

@Getter
public class Application extends KSGraphicalApplication implements KSApplication {

    public static Application currentInstance;

    private final String appDisplayName = "Kyne Systems Trader Machine";
    private final int width = 800;
    private final int height = 600;

    // Components
    // --- Top button bars
    private JPanel topButtonBar;
    private JButton aboutButton;
    private JButton basicSettingsButton;
    private JButton logsButton;
    private JButton tradeProfitLogButton;

    // --- Daemon panel
    private JPanel daemonPanel;
    private JPanel daemon1panel;
    private JPanel daemon2panel;
    private JPanel daemon3panel;
    private JPanel daemon4panel;
    private JPanel daemon5panel;
    private JPanel daemon6panel;
    private JPanel daemon7panel;
    private JPanel daemon8panel;
    private JPanel daemon9panel;
    private JPanel daemon10panel;

    // --- Trading log panel
    private JPanel tradingLogPanel;
    private JTextArea tradingLogTextArea;
    private JScrollPane tradingLogScrollPane;

    // --- All action buttons
    private JPanel allActionButtonsPanel;
    private JButton allStartButton;
    private JButton allStopButton;

    // --- Memory
    private HashMap<Integer, TraderDaemon> daemonMap = new HashMap<>();
    private HashMap<Integer, JPanel> daemonPanelMap = new HashMap<>();

    @Override
    public int appMain(KSEnvironment environment, String execLocation, String[] args) {
        currentInstance = this;
//        GPBannerNotification notification = new GPBannerNotification("Kyne Systems Trader Machine", "Welcome to Kyne Systems Trader Machine!", GPBannerNotification.CORNER_TOP_RIGHT);
//        notification.showNotification();
        return 0;
    }

    @Override
    public GPSplashWindow getSplashWindow() {
        GPSplashWindow splashWindow = new GPSplashWindow(400, 300, JLabel.RIGHT);
        splashWindow.setSplashBackend(new Thread(() -> {
            // Locate storage path
            String storagePath = StorageSetupTool.init(splashWindow);
            StorageSetupTool.copyDefault(splashWindow, storagePath);

            // Check activation
//            boolean activated = LicenseSetupTool.isLicensed(splashWindow, storagePath);
//            if (!activated) {
//                JOptionPane.showMessageDialog(splashWindow, "License not activated. Please reopen the application to reactivate.", "Error", JOptionPane.ERROR_MESSAGE);
//                System.exit(0);
//            }

            // Load configurations
//            boolean success = Config.load(MFS1.realPath(storagePath + "/configs/system.json"));
//            if (!success) {
//                JOptionPane.showMessageDialog(splashWindow, "Failed to load configuration file", "Error", JOptionPane.ERROR_MESSAGE);
//                System.exit(0);
//            }

            // Load libraries
            try {
                splashWindow.setCurrentStatus("Loading libraries...");
                Drivers.loadJarsIn(new File(MFS1.realPath(storagePath + "/libraries")));
            } catch (Exception e) {
                JOptionPane.showMessageDialog(splashWindow, "Failed to load libraries", "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            // Load drivers
            try {
                splashWindow.setCurrentStatus("Loading drivers...");
                Drivers.loadJarsIn(new File(MFS1.realPath(storagePath + "/drivers")));
            } catch (Exception e) {
                JOptionPane.showMessageDialog(splashWindow, "Failed to load drivers", "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            // Load strategies
            try {
                splashWindow.setCurrentStatus("Loading strategies...");
                Drivers.loadJarsIn(new File(MFS1.realPath(storagePath + "/strategies")));
            } catch (Exception e) {
                JOptionPane.showMessageDialog(splashWindow, "Failed to load strategies", "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            // Index drivers
            try {
                splashWindow.setCurrentStatus("Indexing drivers...");
                HashMap<String, Class<?>> drivers = (HashMap<String, Class<?>>) Drivers.DriverIntrospection.findImplementations(TraderDriverManifestV1.class);
                for (String key : drivers.keySet()) {
                    Class<?> driverClass = drivers.get(key);
                    System.out.println("Driver: " + key + " -> " + driverClass.getName());
                    Drivers.drivers.put(key, driverClass);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(splashWindow, "Drivers are not loaded!", "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            // Test driver connection
            System.out.println("Loaded drivers: " + Drivers.drivers.size());
            for (Class<?> driverClass : Drivers.drivers.values()) {
                if (driverClass.isAssignableFrom(TraderDriverManifestV1.class)) {
                    try {
                        TraderDriverManifestV1 manifest = (TraderDriverManifestV1) driverClass.getDeclaredConstructor().newInstance();
                        splashWindow.setCurrentStatus("Testing connection: " + manifest.getDriverExchange());
                        DriverExitCode exitCode = manifest.testConnection();
                        if (exitCode != DriverExitCode.DRIVER_TEST_OK && exitCode != DriverExitCode.OK) {
                            JOptionPane.showMessageDialog(splashWindow, "Driver connection test failed: " + manifest.getDriverExchange() + " from " + manifest.getDriverName(), "Error", JOptionPane.ERROR_MESSAGE);
                            System.exit(0);
                        } else {
                            System.out.println("Driver connection test passed: " + manifest.getDriverExchange());
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(splashWindow, "Failed to test driver connection: " + driverClass.getName(), "Error", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                } else {
                    System.out.println("Driver class " + driverClass.getName() + " is not a valid TraderDriverManifest.");
                }
            }

            // Index strategies
            try {
                splashWindow.setCurrentStatus("Indexing strategies...");
                HashMap<String, Class<?>> strategies = (HashMap<String, Class<?>>) Drivers.DriverIntrospection.findImplementations(TraderStrategyManifestV1.class);
                for (String key : strategies.keySet()) {
                    Class<?> strategyClass = strategies.get(key);
                    System.out.println("Strategy: " + key + " -> " + strategyClass.getName());
                    Drivers.strategies.put(key, strategyClass);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(splashWindow, "Strategies are not loaded!", "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            // Synchronize accounts for each drivers

        }));
        JLabel titleLabel = new JLabel("Kyne Systems Trader Machine");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setVerticalAlignment(SwingConstants.CENTER);
        titleLabel.setSize(splashWindow.getWidth(), splashWindow.getHeight());
        titleLabel.setLocation(0,0);
        splashWindow.add(titleLabel);
        splashWindow.setStatusSuffixSpacing("    ");
        splashWindow.setStatusSuffix("KSTraderMachine 1.0");
        splashWindow.setForegroundColor(Color.WHITE);
        splashWindow.setBackgroundColor(Color.BLACK);
        splashWindow.setImageLocation("path/to/splash/image.png"); // Set the path to your splash image
        return splashWindow;
    }

    public static void main(String[] args) {
        GraphiteProgramLauncher.sleekUIEnabled = true;
        GraphiteProgramLauncher.launch(Application.class, args);
    }
}
