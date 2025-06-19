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
import me.hysong.atlas.utils.LanguageKit;
import me.hysong.atlas.utils.MFS1;
import me.hysong.kynesystem.services.notification.NotificationObject;
import me.hysong.kynesystems.apps.kstradermachine.backend.Drivers;
import me.hysong.kynesystems.common.foundation.startup.StorageSetupTool;
import me.hysong.kynesystems.apps.kstradermachine.front.uiobjects.DaemonPanel;
import me.hysong.kynesystems.apps.kstradermachine.objects.Daemon;
import me.hysong.kynesystems.apps.kstradermachine.objects.DaemonCfg;
import me.hysong.kynesystems.apps.kstradermachine.subwins.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

@Getter
public class KSTraderMachine extends KSGraphicalApplication implements KSApplication {

    public static KSTraderMachine currentInstance;
    public static String storagePath;

    private final String appDisplayName = "Kyne Systems Trader Machine";
    private final int windowWidth = 800;
    private final int windowHeight = 600;

    // Components

    // --- Top button bars
    private JPanel topButtonBar;
    private JButton aboutButton;
    private JButton basicSettingsButton;
    private JButton logsButton;
    private JButton tradeProfitLogButton;

    // --- Daemon panel
    private JPanel middleHStackPanel;
    private JPanel daemonGridPanel;
    private HashMap<Integer, DaemonPanel> daemonStatusPanels;

    // --- Trading log panel
    private JTextArea tradingLogTextArea;
    private JScrollPane tradingLogScrollPane;

    // --- All action buttons
    private JPanel allActionButtonsPanel;
    private JButton allStartButton;
    private JButton allStopButton;

    // --- Memory
    private final HashMap<Integer, Daemon> daemonMap = new HashMap<>();

    @Override
    public int appMain(KSEnvironment environment, String execLocation, String[] args) {
        currentInstance = this;
//        for (int i = 0; i < 10; i++) {
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            NotificationObject notification = new NotificationObject((o) -> {}, (o) -> {}, "KSTraderMachine " + i, "Welcome to KSTrader Machine!");
//            notification.dispatch();
//        }
        if (!NotificationObject.isServerUp()) {
            System.out.println("NOTIFICATION SERVER DOWN");
            JOptionPane.showMessageDialog(null, "Warning: It seems KSNotificationServer is down. Important notification may be missed.", "Warning", JOptionPane.WARNING_MESSAGE);
        } else {
            System.out.println("Notification server is up and running.");
        }
        return 0;
    }

    public void loadDrivers() {

        // Load drivers
        try {
            Drivers.loadJarsIn(new File(MFS1.realPath(storagePath + "/drivers")));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Failed to load drivers", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        // Index drivers
        try {
            HashMap<String, Class<?>> drivers = (HashMap<String, Class<?>>) Drivers.DriverIntrospection.findImplementations(TraderDriverManifestV1.class);
            for (String key : drivers.keySet()) {
                Class<?> driverClass = drivers.get(key);
                SystemLogs.log("INFO", "Driver: " + key + " -> " + driverClass.getName());
                try {
                    TraderDriverManifestV1 manifest = (TraderDriverManifestV1) driverClass.getDeclaredConstructor().newInstance();
                    Drivers.driversInstantiated.put(key, manifest);
                    Drivers.drivers.put(key, driverClass);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Drivers not instantiated!", "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                    throw new RuntimeException("Driver instantiation failed", ex);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Drivers are not loaded!", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            throw new RuntimeException(e);
        }


    }

    public void loadStrategies() {
        // Load strategies
        try {
            Drivers.loadJarsIn(new File(MFS1.realPath(storagePath + "/strategies")));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Failed to load strategies", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        // Index strategies
        try {
            HashMap<String, Class<?>> strategies = (HashMap<String, Class<?>>) Drivers.DriverIntrospection.findImplementations(TraderStrategyManifestV1.class);
            for (String key : strategies.keySet()) {
                Class<?> strategyClass = strategies.get(key);
                SystemLogs.log("INFO", "Strategy: " + key + " -> " + strategyClass.getName());
                try {
                    TraderStrategyManifestV1 manifest = (TraderStrategyManifestV1) strategyClass.getDeclaredConstructor().newInstance();
                    Drivers.strategiesInstantiated.put(key, manifest);
                    Drivers.strategies.put(key, strategyClass);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Drivers not instantiated!", "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                    throw new RuntimeException("Driver instantiation failed", ex);
                }
//                    Drivers.strategies.put(key, strategyClass);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Strategies are not loaded!", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public GPSplashWindow getSplashWindow(String[] args) {
        GPSplashWindow splashWindow = new GPSplashWindow(400, 300, JLabel.RIGHT);
        splashWindow.setSplashBackend(new Thread(() -> {
            // Locate storage path
            storagePath = StorageSetupTool.init("KSTraderMachine", args);

            // TODO Check activation
//            boolean activated = LicenseSetupTool.isLicensed(splashWindow, storagePath);
//            if (!activated) {
//                JOptionPane.showMessageDialog(splashWindow, "License not activated. Please reopen the application to reactivate.", "Error", JOptionPane.ERROR_MESSAGE);
//                System.exit(0);
//            }

            // TODO Load configurations
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

            // Load languages
            String[] nonDefaultLanguages = MFS1.listFiles(storagePath + "/languages", false);
            String[] defaultLanguages = MFS1.listFiles(storagePath + "/defaults/languages", false);
            for (String file : defaultLanguages) {
                // Filter .lang.txt files only
                if (!file.endsWith(".lang.txt")) continue;

                // Load
                try {
                    LanguageKit.loadLanguageFromFile(storagePath + "/defaults/languages/" + file);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for (String file : nonDefaultLanguages) {
                // Filter .lang.txt files only
                if (!file.endsWith(".lang.txt")) continue;

                // Load
                try {
                    LanguageKit.loadLanguageFromFile(storagePath + "/languages/" + file);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            loadDrivers();
            loadStrategies();

            // Test driver connection
            SystemLogs.log("INFO", "Loaded drivers: " + Drivers.drivers.size());
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
                            SystemLogs.log("INFO", "Driver connection test passed: " + manifest.getDriverExchange());
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(splashWindow, "Failed to test driver connection: " + driverClass.getName(), "Error", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                } else {
                    SystemLogs.log("INFO", "Driver class " + driverClass.getName() + " is not a valid TraderDriverManifest.");
                }
            }

            // Load daemons
            try {
                splashWindow.setCurrentStatus("Loading configurations...");
                String[] flist = MFS1.listFiles(storagePath + "/configs/daemons", false);
                for (String fileName : flist) {
                    if (!fileName.endsWith(".json")) {
                        continue;
                    }
                    try {
                        int slot = Integer.parseInt(fileName.replace(".json", ""));
                        Daemon dm = new Daemon(slot, new DaemonCfg());
                        dm.reloadPreference();
                        daemonMap.put(slot, dm);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // TODO Synchronize accounts for each drivers
//            try {
//                splashWindow.setCurrentStatus("Synchronizing accounts...");
//                Drivers.DriverLoader
//            }


            // ==================
            // Prepare UI
            // ==================
            splashWindow.setCurrentStatus(LanguageKit.getValue("STATUS_PREPARE_UI"));
            setLayout(new GridBagLayout()); // Main panel uses GridBagLayout
            GridBagConstraints gbcMain = new GridBagConstraints();

            // -- top buttons
            aboutButton = new JButton(LanguageKit.getValue("ABOUT_BUTTON_TEXT"));
            basicSettingsButton = new JButton(LanguageKit.getValue("BASIC_SETTINGS_BUTTON_TEXT"));
            logsButton = new JButton(LanguageKit.getValue("LOGS_BUTTON_TEXT"));
            tradeProfitLogButton = new JButton(LanguageKit.getValue("TRADE_PROFIT_LOG_BUTTON_TEXT"));
            topButtonBar = new JPanel();
            topButtonBar.setLayout(new GridLayout(1, 0, 5, 0));
            topButtonBar.add(aboutButton);
            topButtonBar.add(basicSettingsButton);
            topButtonBar.add(logsButton);
            topButtonBar.add(tradeProfitLogButton);

            gbcMain.gridx = 0;
            gbcMain.gridy = 0;
            gbcMain.weightx = 1.0; // Takes full width
            gbcMain.weighty = 0.1; // 10% of window height
            gbcMain.fill = GridBagConstraints.BOTH;
            add(topButtonBar, gbcMain);

            // -- Middle panel
            middleHStackPanel = new JPanel();
            middleHStackPanel.setLayout(new GridBagLayout()); // Middle panel also uses GridBagLayout
            GridBagConstraints gbcMiddle = new GridBagConstraints();

            // -- Daemon panel
            int rows = 5;
            int cols = 2;
            daemonGridPanel = new JPanel();
            daemonGridPanel.setLayout(new GridLayout(rows, cols, 5, 5)); // Added gaps for better visuals
            daemonStatusPanels = new HashMap<>();
            for (int i = 0; i < rows; i++) {
                for (int ii = 0; ii < cols; ii++) {
                    int idx = i * cols + ii;
                    SystemLogs.log("INFO", "Displaying daemon " + idx);
                    DaemonPanel dp = new DaemonPanel("-", "Not Configured", DaemonPanel.DaemonStatusOutlook.NOT_RUNNING, daemonMap.get(idx));
                    daemonStatusPanels.put(idx, dp);
                    daemonGridPanel.add(dp);
                    dp.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            GraphiteProgramLauncher.launch(EditDaemon.class, new String[]{"slot=" + idx});
                        }
                    });
                }
            }

            // Define the fixed width for the daemonGridPanel.
            // This should be calculated based on your DaemonPanel's content and 'cols'.
            // For example, if each DaemonPanel aims for 150px width, and cols=2, with 5px hgap:
            // (150 * 2) + (5 * (2-1)) = 300 + 5 = 305. Add insets if any.
            final int FIXED_DAEMON_GRID_WIDTH = 305; // ADJUST THIS VALUE AS NEEDED

            daemonGridPanel.setPreferredSize(new Dimension(FIXED_DAEMON_GRID_WIDTH, 10)); // Initial height can be small, layout will manage
            daemonGridPanel.setMinimumSize(new Dimension(FIXED_DAEMON_GRID_WIDTH, 10));
            daemonGridPanel.setMaximumSize(new Dimension(FIXED_DAEMON_GRID_WIDTH, Short.MAX_VALUE)); // Fixed width, flexible height

            gbcMiddle.gridx = 0;
            gbcMiddle.gridy = 0;
            gbcMiddle.weightx = 0.0; // Daemon panel does not take extra horizontal space
            gbcMiddle.weighty = 1.0; // Takes proportional vertical space
            gbcMiddle.fill = GridBagConstraints.VERTICAL; // Fills vertically, respects preferred width
            gbcMiddle.anchor = GridBagConstraints.NORTHWEST; // Anchor to top-left
            middleHStackPanel.add(daemonGridPanel, gbcMiddle);

            // -- Trading log panel
            tradingLogTextArea = new JTextArea(10, 30); // Give some initial preferred size
            tradingLogTextArea.setEditable(false);
            tradingLogTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            tradingLogScrollPane = new JScrollPane(tradingLogTextArea);
            tradingLogScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            tradingLogScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); // Usually better than ALWAYS

            gbcMiddle.gridx = 1;
            gbcMiddle.gridy = 0;
            gbcMiddle.weightx = 1.0; // Trading log takes all remaining horizontal space
            gbcMiddle.weighty = 1.0; // Takes proportional vertical space
            gbcMiddle.fill = GridBagConstraints.BOTH;
            middleHStackPanel.add(tradingLogScrollPane, gbcMiddle);

            // Add middleHStackPanel to the main frame
            gbcMain.gridx = 0;
            gbcMain.gridy = 1;
            gbcMain.weightx = 1.0;
            gbcMain.weighty = 0.9; // 70% of window height
            gbcMain.fill = GridBagConstraints.BOTH;
            add(middleHStackPanel, gbcMain);

            // -- All action buttons
            allActionButtonsPanel = new JPanel();
            allStartButton = new JButton(LanguageKit.getValue("ALLSTART_BUTTON_TEXT"));
            allStopButton = new JButton(LanguageKit.getValue("ALLSTOP_BUTTON_TEXT"));
            allActionButtonsPanel.setLayout(new GridLayout(1, 0, 5, 0)); // NEW: 1 row, any columns, 5px hgap, 0px vgap
            allActionButtonsPanel.add(allStartButton);
            allActionButtonsPanel.add(allStopButton);

            gbcMain.gridx = 0;
            gbcMain.gridy = 2;
            gbcMain.weightx = 1.0;
            gbcMain.weighty = 0.1; // 10% of window height
            gbcMain.fill = GridBagConstraints.BOTH; // Panel fills its allocated space
            add(allActionButtonsPanel, gbcMain);

            // Add button actions
            aboutButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    GraphiteProgramLauncher.launch(AboutWindow.class, new String[]{});
                }
            });
            basicSettingsButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    GraphiteProgramLauncher.launch(SettingsWindow.class, new String[]{});
                }
            });
            logsButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    GraphiteProgramLauncher.launch(SystemLogs.class, new String[]{});
                }
            });
            tradeProfitLogButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    GraphiteProgramLauncher.launch(ProfitLogs.class, new String[]{});
                }
            });

            allStartButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    for (Daemon d : daemonMap.values()) {
                        d.start();
                    }
                    JOptionPane.showMessageDialog(null, "All daemons started.");
                }
            });
            allStopButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    for (Daemon d : daemonMap.values()) {
                        d.terminate();
                    }
                    JOptionPane.showMessageDialog(null, "All daemons terminate queued.");
                }
            });

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

    public static void openFileExplorer(String path) {
        File file = new File(path);

        if (!file.exists()) {
            System.err.println("Path does not exist: " + path);
            return;
        }

        try {
            Desktop desktop = Desktop.getDesktop();
            if (file.isDirectory()) {
                desktop.open(file);  // opens the folder in the default file explorer
            } else {
                desktop.open(file.getParentFile());  // opens the folder containing the file
            }
        } catch (IOException | UnsupportedOperationException e) {
            System.err.println("Failed to open file explorer: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        GraphiteProgramLauncher.sleekUIEnabled = true;
        GraphiteProgramLauncher.launch(KSTraderMachine.class, args);
    }
}
