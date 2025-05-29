package me.hysong.kynesystems.apps.kstradermachine.subwins;

import com.google.gson.JsonParser;
import lombok.Getter;
import me.hysong.apis.kstrader.v1.driver.TraderDriverManifestV1;
import me.hysong.apis.kstrader.v1.strategy.TraderStrategyManifestV1;
import me.hysong.atlas.interfaces.KSApplication;
import me.hysong.atlas.sdk.graphite.v1.KSGraphicalApplication;
import me.hysong.atlas.sharedobj.KSEnvironment;
import me.hysong.atlas.utils.MFS1;
import me.hysong.kynesystems.apps.kstradermachine.Application;
import me.hysong.kynesystems.apps.kstradermachine.backend.Drivers;
import me.hysong.kynesystems.apps.kstradermachine.objects.Daemon;
import me.hysong.kynesystems.apps.kstradermachine.objects.DaemonCfg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Getter
public class EditDaemon extends KSGraphicalApplication implements KSApplication {
    private final String appDisplayName = "Edit / Create Daemon";
    private final int closeBehavior = JFrame.DISPOSE_ON_CLOSE;
    private final int windowWidth = 600;
    private final int windowHeight = 500;

    private int slot = -1;
    private String cfgJsonPath = Application.storagePath + "/configs/daemons/undefined.json";
    private DaemonCfg daemonCfg;
    private Daemon daemon;

    // UI Components
    // Add these at the beginning of your EditDaemon class
    // Top Action Bar
    private JButton editWithEditorButton;
    private JButton refreshButtonTop; // Renamed to avoid conflict if another refresh button exists

    // Configuration Form
    private JLabel slotDisplayField; // Using JLabel to display non-editable slot
    private JTextField labelTextField;
    private JComboBox<String> exchangeDriverComboBox;
    private JComboBox<String> strategyNameComboBox;

    private JPanel traderModeButtonPanel;
    private JToggleButton spotToggleButton;
    private JToggleButton perpetToggleButton;
    private JToggleButton futureToggleButton;
    private JToggleButton optionToggleButton;
    private ButtonGroup traderModeButtonGroup;

    private JButton editStrategySettingsButton;
    private JTextField symbolTextField;
    private JTextField maxCashTextField;

    private JPanel orderModeButtonPanel;
    private JToggleButton marketToggleButton;
    private JToggleButton limitToggleButton;
    private ButtonGroup orderModeButtonGroup;

    private JPanel actionButtonPanel;
    private JButton runButton;
    private JButton stopButton;

    // Bottom Apply Button
    private JButton applyButton;

    // Data for ComboBoxes (user will populate these later)
    private final ArrayList<String> exchangeDriverOptions = new ArrayList<>();
    private final ArrayList<String> strategyNameOptions = new ArrayList<>();

    @Override
    public int appMain(KSEnvironment environment, String execLocation, String[] args) {

        if (args.length == 0) {
            JOptionPane.showMessageDialog(null, appDisplayName + " failed to open - Required to have at least one argument (slot=x)", "Error", JOptionPane.ERROR_MESSAGE);
            return 9;
        }

        String slotArg = args[0];
        if (!slotArg.startsWith("slot=")) {
            JOptionPane.showMessageDialog(null, appDisplayName + " failed to open - Slot is not valid", "Error", JOptionPane.ERROR_MESSAGE);
            return 9;
        }

        slot = Integer.parseInt(slotArg.substring("slot=".length()));
        cfgJsonPath = Application.storagePath + "/configs/daemons/" + slot + ".json";
        String content = MFS1.readString(cfgJsonPath);
        if (content == null) {
            JOptionPane.showMessageDialog(null, appDisplayName + " failed to open - Content is not valid", "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
            return 9;
        }
        daemonCfg = new DaemonCfg();
        daemonCfg.fromJson(JsonParser.parseString(content).getAsJsonObject());
        daemonCfg.setSlot(slot);
        daemon = Application.currentInstance.getDaemonMap().get(slot);

        setLayout(new GridBagLayout()); // Main layout for the EditDaemon panel
        GridBagConstraints gbcMain = new GridBagConstraints(); // Constraints for main panel items

        // --- 1. Top Action Bar ---
        JPanel topActionPanel = new JPanel(new BorderLayout(10, 0)); // BorderLayout: Hgap=10, Vgap=0
        editWithEditorButton = new JButton("Edit using editor (Open notepad, Text Editor, or run nano on terminal)");
        refreshButtonTop = new JButton("Refresh");
        topActionPanel.add(editWithEditorButton, BorderLayout.CENTER);
        topActionPanel.add(refreshButtonTop, BorderLayout.EAST);

        gbcMain.gridx = 0;
        gbcMain.gridy = 0;
        gbcMain.weightx = 1.0;
        gbcMain.fill = GridBagConstraints.HORIZONTAL;
        gbcMain.insets = new Insets(5, 5, 5, 5);
        add(topActionPanel, gbcMain);

        // --- 2. Main Configuration Form Panel ---
        JPanel configFormPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcForm = new GridBagConstraints();
        gbcForm.insets = new Insets(3, 5, 3, 5);
        // gbcForm.anchor = GridBagConstraints.WEST; // Set per component type if needed

        // Determine a preferred width for labels
        JLabel protoLabel = new JLabel("Exchange Driver:"); // Use your widest label text
        Dimension labelPreferredSize = protoLabel.getPreferredSize();
        labelPreferredSize.width += 10; // Add a little extra padding if you like

        int formRow = 0;

        // Helper to create and configure labels
        BiConsumer<String, Integer> addLabel = (text, row) -> {
            JLabel label = new JLabel(text);
            label.setPreferredSize(labelPreferredSize);
            gbcForm.gridx = 0;
            gbcForm.gridy = row;
            gbcForm.weightx = 0.0; // Labels do not take extra horizontal space
            gbcForm.fill = GridBagConstraints.NONE;
            gbcForm.anchor = GridBagConstraints.WEST; // Align labels to the left
            configFormPanel.add(label, gbcForm);
        };

        // Helper to configure components
        Consumer<Component> addComponent = (component) -> {
            gbcForm.gridx = 1; // Component in the second column
            // gridy is already set by addLabel or incremented
            gbcForm.weightx = 1.0; // Components take all available extra horizontal space
            gbcForm.fill = GridBagConstraints.HORIZONTAL;
            gbcForm.anchor = GridBagConstraints.WEST;
            configFormPanel.add(component, gbcForm);
        };

        // Slot
        addLabel.accept("Using Slot:", formRow);
        slotDisplayField = new JLabel(String.valueOf(this.slot));
        addComponent.accept(slotDisplayField);
        formRow++;

        // Label
        addLabel.accept("Label:", formRow);
        labelTextField = new JTextField(daemonCfg.getLabel(), 20);
        addComponent.accept(labelTextField);
        formRow++;

        // Exchange Driver (JComboBox)
        addLabel.accept("Exchange Driver:", formRow);
        exchangeDriverOptions.clear(); // Clear if appMain can be called multiple times for the same instance
        exchangeDriverOptions.add("");
        HashMap<String, Class<?>> drvManifests = Drivers.drivers;
        for (String className : drvManifests.keySet()) {
            try {
                Class<?> clz = drvManifests.get(className);
                TraderDriverManifestV1 drvmanifest = (TraderDriverManifestV1) clz.getConstructor().newInstance();
                String line = drvmanifest.getDriverName() + " (" + drvmanifest.getDriverExchange() + "," + drvmanifest.getDriverVersion() + "@" + drvmanifest.getDriverUpdateDate() + "): " + className; // Example: added [spot] for clarity
                exchangeDriverOptions.add(line);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        exchangeDriverComboBox = new JComboBox<>(exchangeDriverOptions.toArray(new String[0]));
        // Attempt to select the stored driver
        String targetDriverClass = daemonCfg.getExchangeDriverClass();
        if (targetDriverClass != null && !targetDriverClass.isEmpty()) {
            boolean found = false;
            for (String option : exchangeDriverOptions) {
                if (option.endsWith(": " + targetDriverClass)) {
                    exchangeDriverComboBox.setSelectedItem(option);
                    found = true;
                    break;
                }
            }
            if (!found && exchangeDriverComboBox.getItemCount() > 0) {
                exchangeDriverComboBox.setSelectedIndex(0); // Default to "" if not found
            }
        } else if (exchangeDriverComboBox.getItemCount() > 0) {
            exchangeDriverComboBox.setSelectedIndex(0); // Default if no class stored
        }
        addComponent.accept(exchangeDriverComboBox);
        formRow++;

        // Strategy Name (JComboBox)
        addLabel.accept("Strategy Name:", formRow);
        strategyNameOptions.clear(); // Clear for fresh population
        strategyNameOptions.add("");
        HashMap<String, Class<?>> strManifest = Drivers.strategies;
        for (String className : strManifest.keySet()) {
            try {
                Class<?> clz = strManifest.get(className);
                TraderStrategyManifestV1 stManifest = (TraderStrategyManifestV1) clz.getConstructor().newInstance();
                String flags = "";
                if (stManifest.isForWS()) flags += "WS";
                if (stManifest.isForREST()) flags += "RE";
                String line = stManifest.getStrategyName() + " (" + stManifest.getStrategyVersion() + " [" + flags + "]): " + className; // Example: added [spot] for clarity
                strategyNameOptions.add(line);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // strategyNameOptions will be populated based on driver selection
        strategyNameComboBox = new JComboBox<>(strategyNameOptions.toArray(new String[0]));
        strategyNameComboBox.setEnabled(exchangeDriverComboBox.getItemCount() > 0 && exchangeDriverComboBox.getSelectedIndex() > 0);
        String targetStrategyClass = daemonCfg.getStrategyName();
        if (targetStrategyClass != null && !targetStrategyClass.isEmpty()) {
            boolean found = false;
            for (String option : strategyNameOptions) {
                if (option.endsWith(": " + targetStrategyClass)) {
                    strategyNameComboBox.setSelectedItem(option);
                    found = true;
                    break;
                }
            }
            if (!found && strategyNameComboBox.getItemCount() > 0) {
                strategyNameComboBox.setSelectedIndex(0);
            }
        } else if (strategyNameComboBox.getItemCount() > 0) {
            strategyNameComboBox.setSelectedIndex(0);
        }
        addComponent.accept(strategyNameComboBox);
        formRow++;

        // Trader Mode (JToggleButtons)
        addLabel.accept("Trader Mode:", formRow);
        traderModeButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        spotToggleButton = new JToggleButton("Spot");
        perpetToggleButton = new JToggleButton("Perpetual"); // Matched storyboard
        futureToggleButton = new JToggleButton("Future");
        optionToggleButton = new JToggleButton("Option");
        traderModeButtonGroup = new ButtonGroup();
        traderModeButtonGroup.add(spotToggleButton); traderModeButtonPanel.add(spotToggleButton);
        traderModeButtonGroup.add(perpetToggleButton); traderModeButtonPanel.add(perpetToggleButton);
        traderModeButtonGroup.add(futureToggleButton); traderModeButtonPanel.add(futureToggleButton);
        traderModeButtonGroup.add(optionToggleButton); traderModeButtonPanel.add(optionToggleButton);
        addComponent.accept(traderModeButtonPanel);
        formRow++;

        // Edit Strategy Settings (JButton) - Placed in its own row, spanning if desired
        // To make it span two columns and be centered:
        gbcForm.gridx = 0;
        gbcForm.gridy = formRow;
        gbcForm.gridwidth = 2; // Span both columns
        gbcForm.weightx = 1.0;
        gbcForm.fill = GridBagConstraints.NONE; // Don't stretch button, just center it
        gbcForm.anchor = GridBagConstraints.CENTER;
        editStrategySettingsButton = new JButton("Edit Strategy Settings");
        editStrategySettingsButton.setEnabled(strategyNameComboBox.getItemCount() > 0 && strategyNameComboBox.getSelectedIndex() > 0);
        configFormPanel.add(editStrategySettingsButton, gbcForm);
        gbcForm.gridwidth = 1; // Reset gridwidth for subsequent rows
        formRow++;


        // Symbol
        addLabel.accept("Symbol:", formRow);
        symbolTextField = new JTextField(daemonCfg.getSymbol(), 20);
        symbolTextField.setEnabled(strategyNameComboBox.getItemCount() > 0 && strategyNameComboBox.getSelectedIndex() > 0);
        addComponent.accept(symbolTextField);
        formRow++;

        // Max Cash
        addLabel.accept("Max Cash:", formRow);
        maxCashTextField = new JTextField(String.valueOf(daemonCfg.getMaxCashAllocated()), 20);
        maxCashTextField.setEnabled(strategyNameComboBox.getItemCount() > 0 && strategyNameComboBox.getSelectedIndex() > 0);
        addComponent.accept(maxCashTextField);
        formRow++;

        // Order Mode (JToggleButtons)
        addLabel.accept("Order Mode:", formRow);
        orderModeButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        marketToggleButton = new JToggleButton("Market");
        limitToggleButton = new JToggleButton("Limit");
        orderModeButtonGroup = new ButtonGroup();
        orderModeButtonGroup.add(marketToggleButton); orderModeButtonPanel.add(marketToggleButton);
        orderModeButtonGroup.add(limitToggleButton); orderModeButtonPanel.add(limitToggleButton);
        addComponent.accept(orderModeButtonPanel);
        formRow++;

        // Action (Run/Stop JButtons)
        addLabel.accept("Action:", formRow);
        actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        runButton = new JButton("Run");
        stopButton = new JButton("Stop");
        actionButtonPanel.add(runButton);
        actionButtonPanel.add(stopButton);
        addComponent.accept(actionButtonPanel);
        // formRow++; // No need to increment if it's the last form item before adding configFormPanel

        runButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                daemon.start();
                populateFormFromDaemonCfg();
            }
        });
        stopButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                daemon.terminate();
                populateFormFromDaemonCfg();
            }
        });

        // Add the configFormPanel to the main panel
        gbcMain.gridx = 0;
        gbcMain.gridy = 1;
        gbcMain.weightx = 1.0;
        gbcMain.weighty = 1.0;
        gbcMain.fill = GridBagConstraints.BOTH;
        gbcMain.insets = new Insets(0, 5, 5, 5);
        add(configFormPanel, gbcMain);


        // --- 3. Bottom Apply Button ---
        applyButton = new JButton("Apply");
        // To make it span, put it in a panel or set gbc constraints directly
        gbcMain.gridx = 0;
        gbcMain.gridy = 2;
        gbcMain.weightx = 1.0;
        gbcMain.weighty = 0.0; // No vertical stretch for apply button's row
        gbcMain.fill = GridBagConstraints.HORIZONTAL;
        gbcMain.insets = new Insets(10, 5, 10, 5); // Add some vertical padding
        add(applyButton, gbcMain);

        // --- Initial UI State & Data Loading ---
        populateFormFromDaemonCfg();
        populateToggles();
        setupActionListenersAndDynamicBehavior(); // Method to be created

        return 0;
    }

    private void populateFormFromDaemonCfg() {
        if (daemonCfg == null) return;

        slotDisplayField.setText(String.valueOf(this.slot)); // Already set if 'slot' instance var is used
        labelTextField.setText(daemonCfg.getLabel());

        // TODO: Map daemonCfg.getExchangeDriverClass() to the display string in exchangeDriverComboBox
        // String displayDriver = mapDriverClassToDisplayString(daemonCfg.getExchangeDriverClass());
        // if (displayDriver != null) exchangeDriverComboBox.setSelectedItem(displayDriver);
        // else if (!exchangeDriverOptions.isEmpty()) exchangeDriverComboBox.setSelectedIndex(0); // Or handle error

        // TODO: Map daemonCfg.getStrategyName() to the display string in strategyNameComboBox
        // String displayStrategy = mapStrategyToDisplayString(daemonCfg.getStrategyName());
        // if (displayStrategy != null) strategyNameComboBox.setSelectedItem(displayStrategy);

        // Trader Mode
        String traderMode = daemonCfg.getTraderMode();
        if ("Spot".equalsIgnoreCase(traderMode)) spotToggleButton.setSelected(true);
        else if ("Perpetual".equalsIgnoreCase(traderMode)) perpetToggleButton.setSelected(true);
        else if ("Future".equalsIgnoreCase(traderMode)) futureToggleButton.setSelected(true);
        else if ("Option".equalsIgnoreCase(traderMode)) optionToggleButton.setSelected(true);

        symbolTextField.setText(daemonCfg.getSymbol());
        maxCashTextField.setText(String.valueOf(daemonCfg.getMaxCashAllocated()));

        // Order Mode
        String orderMode = daemonCfg.getOrderMode();
        if ("Market".equalsIgnoreCase(orderMode)) marketToggleButton.setSelected(true);
        else if ("Limit".equalsIgnoreCase(orderMode)) limitToggleButton.setSelected(true);

        // Run/Stop buttons state based on daemonCfg.isRunning() or daemon.isRunning()
         runButton.setEnabled(!daemon.isRunning());
         stopButton.setEnabled(daemon.isRunning());
    }

    private void setupActionListenersAndDynamicBehavior() {
        // This is where you'll implement the logic from the storyboard annotations.
        // Example for Exchange Driver ComboBox:
        exchangeDriverComboBox.addActionListener(e -> {
            String selectedDriver = (String) exchangeDriverComboBox.getSelectedItem();
            if (selectedDriver != null && !selectedDriver.trim().isEmpty()) {
                // User will populate strategyNameOptions based on selectedDriver
                // For now, just enable the strategy combo box
                strategyNameComboBox.setEnabled(true);
                // TODO: Populate strategyNameComboBox with relevant strategies
                // strategyNameOptions.clear();
                // strategyNameOptions.addAll(getStrategiesForDriver(selectedDriver));
                // strategyNameComboBox.setModel(new DefaultComboBoxModel<>(strategyNameOptions.toArray(new String[0])));

                // Reset and disable further fields until strategy is chosen
                strategyNameComboBox.setSelectedIndex(-1); // Clear selection
                editStrategySettingsButton.setEnabled(false);
                symbolTextField.setEnabled(false);
                maxCashTextField.setEnabled(false);
                // orderModePanel components...
                // traderModePanel components might also update based on driver capabilities
            } else {
                strategyNameComboBox.setEnabled(false);
                // ... disable other dependent fields
            }
        });

        strategyNameComboBox.addActionListener(e -> {
            String selectedStrategy = (String) strategyNameComboBox.getSelectedItem();
            if (selectedStrategy != null && !selectedStrategy.trim().isEmpty()) {
                editStrategySettingsButton.setEnabled(true);
                symbolTextField.setEnabled(true);
                maxCashTextField.setEnabled(true);

                populateToggles();
            } else {
                editStrategySettingsButton.setEnabled(false);
                symbolTextField.setEnabled(false);
                maxCashTextField.setEnabled(false);
                // ... disable other dependent fields
            }
        });

        // Action for "Edit using editor"
        editWithEditorButton.addActionListener(e -> {
            // try { Runtime.getRuntime().exec("notepad " + editorPath); // Example for Windows
            // } catch (IOException ioException) { ioException.printStackTrace(); }
            JOptionPane.showMessageDialog(this, "TODO: Implement opening editor for: " + cfgJsonPath, "Info", JOptionPane.INFORMATION_MESSAGE);
        });

        // Action for "Refresh"
        refreshButtonTop.addActionListener(e -> {
            daemonCfg.reload();
            populateFormFromDaemonCfg();
        });


        // Action for "Apply"
        applyButton.addActionListener(e -> {
            // 1. Update daemonCfg from UI fields
            daemonCfg.setLabel(labelTextField.getText());
            try {
                daemonCfg.setExchangeDriverClass(((String) exchangeDriverComboBox.getSelectedItem()).split(": ")[1]);
                daemonCfg.setStrategyName(((String) strategyNameComboBox.getSelectedItem()).split(": ")[1]);
            } catch (Exception xe) {
                JOptionPane.showMessageDialog(this, "Configuration not saved:\nCannot set strategy name: " + xe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            daemonCfg.setSymbol(symbolTextField.getText());
            try {
                daemonCfg.setMaxCashAllocated(Double.parseDouble(maxCashTextField.getText()));
            } catch (NumberFormatException xe) {
                JOptionPane.showMessageDialog(null, "Configuration not saved:\nMax cash value must be a real number format.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            } catch (Exception xe) {
                JOptionPane.showMessageDialog(this, "Configuration not saved:\nCannot set max cash: " + xe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String traderMode = "";
            if (spotToggleButton.isSelected()) traderMode = "Spot";
            else if (perpetToggleButton.isSelected()) traderMode = "Perpetual";
            else if (futureToggleButton.isSelected()) traderMode = "Future";
            else if (optionToggleButton.isSelected()) traderMode = "Option";
            daemonCfg.setTraderMode(traderMode);

            String orderMode = "";
            if (marketToggleButton.isSelected()) orderMode = "Market";
            else if (limitToggleButton.isSelected()) orderMode = "Limit";
            daemonCfg.setOrderMode(orderMode);


            boolean saved = daemonCfg.save();
            if (!saved) {
                JOptionPane.showMessageDialog(this, "Failed to save configuration.", "Error", JOptionPane.ERROR_MESSAGE);
            }

            Application.currentInstance.getDaemonMap().get(slot).reloadPreference();
            dispose();
        });

        // TODO: Add action listeners for Run, Stop, EditStrategySettings, etc.
        // TODO: Implement logic for dynamic enabling/disabling based on strategy capabilities for Trader Modes.
        // TODO: Implement warnings for Limit order mode.
    }

    private void populateToggles() {
        String selectedStrategy = (String) strategyNameComboBox.getSelectedItem();
        String selectedDriver = (String) exchangeDriverComboBox.getSelectedItem();

        if (selectedStrategy == null || selectedStrategy.trim().isEmpty() || selectedDriver == null || selectedDriver.trim().isEmpty()) {
            spotToggleButton.setEnabled(false);
            perpetToggleButton.setEnabled(false);
            futureToggleButton.setEnabled(false);
            optionToggleButton.setEnabled(false);
            marketToggleButton.setEnabled(false);
            limitToggleButton.setEnabled(false);
            return;
        }

        String driverClassName = "";
        String strategyClassName = "";
        try {
            driverClassName = selectedDriver.split(": ")[1];
            strategyClassName = selectedStrategy.split(": ")[1];
        } catch (Exception ex) {
            SystemLogs.log("ERROR", "Selection invalid: " + selectedDriver + ", " + selectedStrategy);
            return;
        }

        TraderDriverManifestV1 drvManifest = Drivers.driversInstantiated.get(driverClassName);
        TraderStrategyManifestV1 stgManifest = Drivers.strategiesInstantiated.get(strategyClassName);

        if (drvManifest == null || stgManifest == null) {
            SystemLogs.log("ERROR", "Failed loading designated drivers");
            return;
        }

        boolean spot = drvManifest.isSupportSpot() && stgManifest.isSupportSpot();
        boolean perp = drvManifest.isSupportPerpetual() && stgManifest.isSupportPerpetual();
        boolean futu = drvManifest.isSupportFuture() && stgManifest.isSupportFuture();
        boolean optn = drvManifest.isSupportOption() && stgManifest.isSupportOption();

        spotToggleButton.setEnabled(spot);
        perpetToggleButton.setEnabled(perp);
        futureToggleButton.setEnabled(futu);
        optionToggleButton.setEnabled(optn);

        boolean market = drvManifest.isSupportOrderAsMarket() && stgManifest.isSupportOrderAsMarket();
        boolean limit = drvManifest.isSupportOrderAsLimit() && stgManifest.isSupportOrderAsLimit();

        marketToggleButton.setEnabled(market);
        limitToggleButton.setEnabled(limit);
    }
}
