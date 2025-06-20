package me.hysong.kynesystems.apps.kstradermachine.subwins; // Or your desired package

import liblks.files.File2;
import lombok.Getter;
import me.hysong.apis.kstrader.v1.driver.TraderDriverManifestV1;
import me.hysong.atlas.interfaces.KSApplication;
import me.hysong.atlas.sdk.graphite.v1.KSGraphicalApplication;
import me.hysong.atlas.sharedobj.KSEnvironment;
import me.hysong.kynesystems.apps.kstradermachine.KSTraderMachine;
import me.hysong.kynesystems.apps.kstradermachine.backend.Drivers;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
// Potentially: import java.io.File; // For directory opening
// Potentially: import java.io.IOException; // For directory opening

@Getter
public class SettingsWindow extends KSGraphicalApplication implements KSApplication { // Changed class name slightly

    // KSGraphicalApplication fields
    private final String appDisplayName = "Settings";
    private final int closeBehavior = JFrame.DISPOSE_ON_CLOSE;
    private final int windowWidth = 450;  // Adjusted based on storyboard elements
    private final int windowHeight = 550; // Adjusted based on storyboard elements

    // UI Components
    private JButton serviceActivationButton;
    private JButton openDriverDirButton;
    private JButton openStrategyDirButton;
    private JButton refreshListButton;
    private JList<String> exchangesList;
    private DefaultListModel<String> exchangesListModel;

    // Data: Exchanges map (key: display name, value: could be a configuration object or similar)
    private final HashMap<String, TraderDriverManifestV1> exchangesMap = new HashMap<>();

    public SettingsWindow() {
        // Constructor: Initialize data or perform pre-UI setup if needed.
        // For demonstration, let's populate exchangesMap with some data.
        // In a real application, this data would come from your backend/storage.

        HashMap<String, TraderDriverManifestV1> drivers = Drivers.driversInstantiated;
        for (TraderDriverManifestV1 driverManifest : drivers.values()) {
            exchangesMap.put(driverManifest.getDriverName(), driverManifest);
        }
    }

    @Override
    public int appMain(KSEnvironment environment, String execLocation, String[] args) {
        initComponents();
        layoutComponents();
        addActionListeners();
        return 0; // Indicate successful UI setup
    }

    private void initComponents() {
        serviceActivationButton = new JButton("Service Activation");
        openDriverDirButton = new JButton("Open Driver Directory");
        openStrategyDirButton = new JButton("Open Strategy Directory");
        refreshListButton = new JButton("Refresh List");

        exchangesListModel = new DefaultListModel<>();
        exchangesList = new JList<>(exchangesListModel);
        exchangesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//        exchangesList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        exchangesList.setFixedCellHeight(28); // Give list items a bit more height
        exchangesList.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5)); // Padding for items

        populateExchangesList(); // Load initial data into the list model
    }

    private void populateExchangesList() {
        exchangesListModel.clear();
        if (exchangesMap == null) return;

        // Sort keys for consistent display, with "(Default Template)" first
        ArrayList<String> sortedExchangeNames = new ArrayList<>(exchangesMap.keySet());
        Collections.sort(sortedExchangeNames);

        for (String name : sortedExchangeNames) {
            exchangesListModel.addElement(name);
        }
    }

    private void layoutComponents() {
        setLayout(new GridBagLayout()); // Main layout for this panel
        GridBagConstraints gbc = new GridBagConstraints();

        // Define default insets for components
        gbc.insets = new Insets(7, 15, 7, 15); // Top, Left, Bottom, Right padding
        gbc.weightx = 1.0; // Most components will take full width
        gbc.fill = GridBagConstraints.HORIZONTAL; // Default fill

        // 1. Service Activation Button
        gbc.gridx = 0;
        gbc.gridy = 0;
//        serviceActivationButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        add(serviceActivationButton, gbc);

        // 2. Directory Buttons Panel
        JPanel directoryButtonsPanel = new JPanel(new GridLayout(1, 2, 10, 0)); // 1 row, 2 cols, 10px hgap
        directoryButtonsPanel.add(openDriverDirButton);
        directoryButtonsPanel.add(openStrategyDirButton);
        gbc.gridy++; // Move to next row
        add(directoryButtonsPanel, gbc);

        // 3. Refresh List Button
        gbc.gridy++; // Move to next row
        add(refreshListButton, gbc);

        // 4. "Exchanges" Label
        JLabel exchangesLabel = new JLabel("Exchanges");
//        exchangesLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        gbc.gridy++; // Move to next row
        gbc.weighty = 0.0; // Label doesn't expand vertically
        gbc.anchor = GridBagConstraints.WEST; // Align label to the left
        gbc.fill = GridBagConstraints.NONE;   // Label takes its preferred size
        gbc.insets = new Insets(15, 15, 3, 15); // Adjust padding for section header
        add(exchangesLabel, gbc);

        // 5. Exchanges List (JList in JScrollPane)
        JScrollPane exchangesScrollPane = new JScrollPane(exchangesList);
        // Give the scroll pane a reasonable preferred size, especially height
        exchangesScrollPane.setPreferredSize(new Dimension(100, 200)); // Width will be managed by layout, height gives it a base
        gbc.gridy++; // Move to next row
        gbc.weighty = 1.0; // List area takes up remaining vertical space
        gbc.fill = GridBagConstraints.BOTH; // List fills both horizontally and vertically
        gbc.insets = new Insets(3, 15, 10, 15); // Reset insets for the list area
        add(exchangesScrollPane, gbc);
    }

    private void addActionListeners() {
        // Service Activation Button - Not implemented as per request
        serviceActivationButton.setEnabled(false); // Disable it for now
        // If you want to add a placeholder:
        // serviceActivationButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "Service Activation clicked (Not Implemented)", "Info", JOptionPane.INFORMATION_MESSAGE));


        // Open Driver Directory Button
        openDriverDirButton.addActionListener(e -> {
            KSTraderMachine.openFileExplorer(new File2(KSTraderMachine.storagePath + "/drivers").getAbsolutePath());
        });

        // Open Strategy Directory Button
        openStrategyDirButton.addActionListener(e -> {
            KSTraderMachine.openFileExplorer(new File2(KSTraderMachine.storagePath + "/strategies").getAbsolutePath());
        });

        // Refresh List Button
        refreshListButton.addActionListener(e -> {
            KSTraderMachine.currentInstance.loadDrivers();
            KSTraderMachine.currentInstance.loadStrategies();
            HashMap<String, TraderDriverManifestV1> drivers = Drivers.driversInstantiated;
            exchangesMap.clear();
            for (TraderDriverManifestV1 driverManifest : drivers.values()) {
                exchangesMap.put(driverManifest.getDriverName(), driverManifest);
            }
            populateExchangesList();
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                    "List refreshed.", "Action", JOptionPane.INFORMATION_MESSAGE);
        });

        // Exchanges List Item Click Action
        exchangesList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                // We use mouseClicked for simplicity here; ListSelectionListener is more robust for selection changes.
                if (evt.getClickCount() >= 1) { // Handle single or double click
                    int index = exchangesList.locationToIndex(evt.getPoint()); // Get index of clicked item
                    if (index >= 0) { // Check if click was on an actual item
                        String selectedExchangeName = exchangesListModel.getElementAt(index);
                        openExchangeDetailWindow(selectedExchangeName);
                    }
                }
            }
        });
    }

    private void openExchangeDetailWindow(String exchangeName) {
        // Get driver manifest
        TraderDriverManifestV1 manifest = exchangesMap.get(exchangeName);

        // Create content for the new frame
        try {
            EditDriverSettings settings = new EditDriverSettings(manifest);
            settings.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Unable to open preference: " +  e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}