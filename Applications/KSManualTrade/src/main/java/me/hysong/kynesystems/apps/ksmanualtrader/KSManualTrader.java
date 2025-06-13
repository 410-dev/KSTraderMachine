package me.hysong.kynesystems.apps.ksmanualtrader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import me.hysong.apis.kstrader.v1.driver.TraderDriverManifestV1;
import me.hysong.atlas.application.ApplicationStorage;
import me.hysong.atlas.async.SimplePromise;
import me.hysong.atlas.interfaces.KSApplication;
import me.hysong.atlas.sdk.graphite.v1.GPSplashWindow;
import me.hysong.atlas.sdk.graphite.v1.GraphiteProgramLauncher;
import me.hysong.atlas.sdk.graphite.v1.KSGraphicalApplication;
import me.hysong.atlas.sharedobj.KSEnvironment;
import me.hysong.atlas.utils.LanguageKit;
import me.hysong.atlas.utils.MFS1;
import me.hysong.kynesystems.apps.ksmanualtrader.windows.EditDriverSettings;
import me.hysong.kynesystems.common.foundation.SystemLogs;
import me.hysong.kynesystems.common.foundation.startup.StorageSetupTool;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

@Getter
public class KSManualTrader extends KSGraphicalApplication implements KSApplication {

    private final String appDisplayName = "Kyne Systems Manual Trader";
    private final int windowWidth = 800;
    private final int windowHeight = 600;

    public static String storagePath = "Storage";



    // --- Global Component Declarations ---

    // Top Panel Components
    private JPanel topPanel;
    private JButton refreshButton;
    private JComboBox<String> exchangeComboBox;

    // Main content panel that holds all sections
    private JPanel mainContentPanel;

    // General Section Components
    private JPanel generalPanel;
    private JButton generalSettingsButton;
    private JButton logsButton;

    // Orders Section Components
    private JPanel ordersPanel;
    private JButton makeOrderButton;
    private JButton currentOrdersButton1;
    private JButton currentOrdersButton2; // As per the image, there are two buttons with similar text

    // History Section Components
    private JPanel historyPanel;
    private JButton historySettingsButton;
    private JButton historyButton;

    // Bottom Panel Components
    private JLabel versionLabel;
    private JLabel connectionLabel;

    // Static data
    public static final String appDataPath = "/apps/KSManualTrade/"; // Append after storage path
    public static final String logsPath = appDataPath + "logs";
    public static final String cfgPath = appDataPath + "configs";

    /**
     * Initializes all the Swing components.
     */
    private void initComponents() {
        // Top Panel
        refreshButton = new JButton("Refresh");
//        String[] exchanges = {"UpBit", "Binance", "Coinbase"};

        // Make combo box from drivers
        String[] exchanges = new String[Drivers.driversInstantiated.size()];
        int index = 0;
        for (String driverId : Drivers.driversInstantiated.keySet()) {
            String exchangeName = Drivers.driversInstantiated.get(driverId).getDriverExchangeName() + ": " + Drivers.driversInstantiated.get(driverId).getDriverExchange() + " (" + driverId + ")";
            exchanges[index] = exchangeName;
            index += 1;
        }
        exchangeComboBox = new JComboBox<>(exchanges);


        // General Section
        generalSettingsButton = new JButton("Settings");
        logsButton = new JButton("Logs");

        // Orders Section
        makeOrderButton = new JButton("Make Order");
        currentOrdersButton1 = new JButton("Current Orders");
        currentOrdersButton2 = new JButton("Current Orders");

        // History Section
        historySettingsButton = new JButton("Settings");
        historyButton = new JButton("History");

        // Bottom Panel
        versionLabel = new JLabel("Version 1.0");
        connectionLabel = new JLabel("Not connected");

        // Button actions
        generalSettingsButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String id = null;
                if (exchangeComboBox.getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(null, "Unable to open setting: ID is not selected.", "ID not selected", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                id = exchangeComboBox.getSelectedItem().toString();
                String[] comp = id.split(" \\(");
                if (comp.length < 2) {
                    JOptionPane.showMessageDialog(null, "Unable to retrieve driver ID from selection.", "Internal Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                id = comp[comp.length - 1];
                id = id.substring(0, id.length() - 1); // This is key of drivers instantiated

                new EditDriverSettings(Drivers.driversInstantiated.get(id)).setVisible(true);
            }
        });
    }

    /**
     * Lays out all the initialized components within various panels.
     */
    private void layoutComponents() {
        // --- Top Panel Construction ---
        topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.add(refreshButton, BorderLayout.WEST);
        topPanel.add(exchangeComboBox, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH); // Add top panel to the frame

        // --- Main Content Panel Construction (for sections) ---
        mainContentPanel = new JPanel();
        mainContentPanel.setLayout(new BoxLayout(mainContentPanel, BoxLayout.Y_AXIS));

        // --- General Panel ---
        generalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        generalPanel.setBorder(new TitledBorder("General"));
        generalPanel.add(generalSettingsButton);
        generalPanel.add(logsButton);
        mainContentPanel.add(generalPanel);

        // --- Orders Panel ---
        ordersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        ordersPanel.setBorder(new TitledBorder("Orders"));
        ordersPanel.add(makeOrderButton);
        ordersPanel.add(currentOrdersButton1);
        ordersPanel.add(currentOrdersButton2);
        mainContentPanel.add(ordersPanel);

        // --- History Panel ---
        historyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        historyPanel.setBorder(new TitledBorder("History"));
        historyPanel.add(historySettingsButton);
        historyPanel.add(historyButton);
        mainContentPanel.add(historyPanel);

        // Add the central container panel to the frame's center
        add(mainContentPanel, BorderLayout.CENTER);

        // --- Bottom Panel (Version Label) ---
        // We wrap the label in a panel to provide some padding
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(versionLabel);
        add(bottomPanel, BorderLayout.SOUTH);
    }


    @Override
    public int appMain(KSEnvironment environment, String execLocation, String[] args) {
        return 0;
    }

    @Override
    public GPSplashWindow getSplashWindow(String[] args) {
        GPSplashWindow splashWindow = new GPSplashWindow(400, 300, JLabel.RIGHT);
        splashWindow.setSplashBackend(new Thread(() -> {
            // Setup storage path
            storagePath = StorageSetupTool.init(args);

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

            // Load drivers
            loadDrivers();

            // TODO Load configurations
            // ldcfg

            // Prepare UI
            // --- Frame Setup ---
//        super("KSManualTrader"); // Set window title
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout(10, 10)); // Main layout with gaps

            // --- Initialize Components ---
            initComponents();

            // --- Layout Panels ---
            layoutComponents();

            // --- Finalize Frame ---
//        pack(); // Adjust window size to fit all components
//        setLocationRelativeTo(null); // Center the window on the screen
            setMinimumSize(getSize()); // Prevent resizing smaller than the packed size

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

    public static void main(String[] args) {
        GraphiteProgramLauncher.sleekUIEnabled = true;
        GraphiteProgramLauncher.launch(KSManualTrader.class, args);
    }
}