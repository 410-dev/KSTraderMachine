package me.hysong.kynesystems.apps.kstradermachine.subwins;

import lombok.Getter;
import me.hysong.atlas.interfaces.KSApplication;
import me.hysong.atlas.sdk.graphite.v1.KSGraphicalApplication;
import me.hysong.atlas.sharedobj.KSEnvironment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

@Getter
public class SystemLogs extends KSGraphicalApplication implements KSApplication {
    // Static log storage
    private static final ArrayList<String> logs = new ArrayList<>();
    private static final ArrayList<Integer> infos = new ArrayList<>();
    private static final ArrayList<Integer> warnings = new ArrayList<>();
    private static final ArrayList<Integer> errors = new ArrayList<>();

    // Static reference to the active window for live updates from static log() method
    public static SystemLogs activeInstance = null;

    // Instance variable for the current filter
    private String currentActiveFilter = "ALL";

    // JFrame properties (likely used by KSGraphicalApplication framework)
    private final String appDisplayName = "System Logs";
    private final int closeBehavior = JFrame.DISPOSE_ON_CLOSE;
    private final int windowWidth = 800;
    private final int windowHeight = 600;

    // UI Components (instance variables)
    private JPanel filterButtonPanel;
    private JButton allFilterButton;
    private JButton infoFilterButton;
    private JButton warningFilterButton;
    private JButton errorFilterButton;

    private JTextArea logTextArea;
    private JScrollPane logDisplayScrollPane;

    private JPanel dumpButtonPanel;
    private JButton makeDumpFileButton;

    @Override
    public int appMain(KSEnvironment environment, String execLocation, String[] args) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // 1. Initialize Filter Buttons Panel (Top)
        filterButtonPanel = new JPanel();
        filterButtonPanel.setLayout(new GridLayout(1, 4, 5, 0));

        allFilterButton = new JButton("All");
        infoFilterButton = new JButton("Info");
        warningFilterButton = new JButton("Warning");
        errorFilterButton = new JButton("Error");

        filterButtonPanel.add(allFilterButton);
        filterButtonPanel.add(infoFilterButton);
        filterButtonPanel.add(warningFilterButton);
        filterButtonPanel.add(errorFilterButton);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 0, 5);
        add(filterButtonPanel, gbc);

        // 2. Initialize Log Display Area (Center)
        logTextArea = new JTextArea("Log Text Goes Here...\n");
        logTextArea.setEditable(false);
        logTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        logDisplayScrollPane = new JScrollPane(logTextArea);
        logDisplayScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logDisplayScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        add(logDisplayScrollPane, gbc);

        // 3. Initialize Dump Button Panel (Bottom)
        dumpButtonPanel = new JPanel();
        dumpButtonPanel.setLayout(new GridLayout(1, 1));

        makeDumpFileButton = new JButton("Make a dump file");
        dumpButtonPanel.add(makeDumpFileButton);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 5, 5);
        add(dumpButtonPanel, gbc);

        // --- Add ActionListeners to Buttons ---
        allFilterButton.addActionListener(e -> refreshLogDisplay("ALL"));
        infoFilterButton.addActionListener(e -> refreshLogDisplay("INFO"));
        warningFilterButton.addActionListener(e -> refreshLogDisplay("WARNING"));
        errorFilterButton.addActionListener(e -> refreshLogDisplay("ERROR"));
        makeDumpFileButton.addActionListener(e -> performDumpAction());

        // --- Active Instance Management and Initial Display ---
        SystemLogs.activeInstance = this;

        // Try to attach WindowListener to clean up activeInstance
        // Defer this to ensure the component is part of a window hierarchy
        SwingUtilities.invokeLater(() -> {
            Window topLevelWindow = SwingUtilities.getWindowAncestor(SystemLogs.this);
            if (topLevelWindow != null) {
                topLevelWindow.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        if (SystemLogs.activeInstance == SystemLogs.this) {
                            SystemLogs.activeInstance = null;
                            // SystemLogs.log("INFO", "SystemLogs window closed, activeInstance nulled."); // For debugging
                        }
                    }
                });
            } else {
                // Fallback or logging if window ancestor isn't found immediately.
                // Your KSGraphicalApplication framework might offer a more direct lifecycle hook.
                // System.err.println("SystemLogs: Top-level window not found immediately for listener attachment.");
            }
        });

        refreshLogDisplay(this.currentActiveFilter); // Initial display based on default filter

        return 0;
    }

    // Method to refresh the log display based on filter
    private void refreshLogDisplay(String filterType) {
        this.currentActiveFilter = filterType.toUpperCase();
        if (logTextArea == null) return;

        StringBuilder sb = new StringBuilder();
        switch (this.currentActiveFilter) {
            case "ALL":
                for (String logEntry : logs) sb.append(logEntry).append("\n");
                break;
            case "INFO":
                for (int index : infos) if (index >= 0 && index < logs.size()) sb.append(logs.get(index)).append("\n");
                break;
            case "WARNING":
                for (int index : warnings) if (index >= 0 && index < logs.size()) sb.append(logs.get(index)).append("\n");
                break;
            case "ERROR":
                for (int index : errors) if (index >= 0 && index < logs.size()) sb.append(logs.get(index)).append("\n");
                break;
            default: // Should not happen with button clicks, but good for robustness
                for (String logEntry : logs) sb.append(logEntry).append("\n");
        }
        logTextArea.setText(sb.toString());
        logTextArea.setCaretPosition(0);
    }

    // Method to handle dumping logs to a file
    private void performDumpAction() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Log Dump");
        fileChooser.setApproveButtonText("Save");
        fileChooser.setSelectedFile(new File("system_logs_dump_" + System.currentTimeMillis() + ".txt"));

        int userSelection = fileChooser.showSaveDialog(SwingUtilities.getWindowAncestor(this));

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
                for (String logEntry : logs) { // Dump all logs
                    writer.write(logEntry);
                    writer.newLine();
                }
                JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                        "Logs successfully dumped to:\n" + fileToSave.getAbsolutePath(),
                        "Dump Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                        "Error saving log dump: " + ex.getMessage(),
                        "Dump Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    // Static method to add logs (with basic live update)
    public static void log(String type, String logMessage) {
        String currentTimeStamp = java.time.LocalDateTime.now().toString();
        logMessage = "[" + type + "] [" + currentTimeStamp + "] " + logMessage;
        logs.add(logMessage);
        System.out.println(logMessage);
        int idx = logs.size() - 1;
        String upperType = type.toUpperCase();
        switch (upperType) {
            case "WARNING" -> warnings.add(idx);
            case "ERROR" -> errors.add(idx);
            default -> infos.add(idx); // Include INFO
        }

        if (SystemLogs.activeInstance != null && SystemLogs.activeInstance.logTextArea != null) {
            final String currentFilterInInstance = SystemLogs.activeInstance.currentActiveFilter;
            boolean shouldAppend = "ALL".equalsIgnoreCase(currentFilterInInstance) || upperType.equalsIgnoreCase(currentFilterInInstance);

            if (shouldAppend) {
                String finalLogMessage = logMessage;
                SwingUtilities.invokeLater(() -> {
                    if (SystemLogs.activeInstance != null && SystemLogs.activeInstance.logTextArea != null) {
                        SystemLogs.activeInstance.logTextArea.append(finalLogMessage + "\n");
                    }
                });
            }
        }
    }
}