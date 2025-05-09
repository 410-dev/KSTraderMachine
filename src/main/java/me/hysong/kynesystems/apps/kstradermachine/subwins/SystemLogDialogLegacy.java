package me.hysong.kynesystems.apps.kstradermachine.subwins;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class SystemLogDialogLegacy extends JDialog {
    private JTextArea logArea;
    private JScrollPane scrollPane;
    private Timer logRefreshTimer;
    private final List<String> systemLogsRef; // Reference to the main system log list

    public SystemLogDialogLegacy(Frame owner, List<String> systemLogs) {
        super(owner, "Detailed System Log", false); // Non-modal dialog
        this.systemLogsRef = systemLogs;

        logArea = new JTextArea(25, 80); // Set initial size rows/cols
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        add(scrollPane, BorderLayout.CENTER);

        // Initial population
        updateLogDisplay();

        // Timer to refresh the display
        logRefreshTimer = new Timer(1000, e -> updateLogDisplay()); // Refresh every second
        logRefreshTimer.start();

        // Add a window listener to stop the timer when the dialog closes
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
//                addSysLogEntry("System Log Dialog closing.");
                stopTimer();
                // Optional: Nullify the reference in CentralApp if needed
                // CentralApp.this.systemLogDialog = null;
            }
            @Override
            public void windowClosed(WindowEvent e) {
                // This is called after dispose()
//                addSysLogEntry("System Log Dialog closed.");
                stopTimer(); // Ensure timer is stopped
            }
        });

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); // Dispose window resources on close
        pack(); // Size the dialog based on components
        setLocationRelativeTo(owner); // Center relative to the main app window
    }

    private void updateLogDisplay() {
        // Ensure update happens on EDT (Timer already does, but good practice)
        SwingUtilities.invokeLater(() -> {
            StringBuilder logContent = new StringBuilder();
            // Iterate directly over the concurrent list
            for (String logEntry : systemLogsRef) {
                logContent.append(logEntry).append("\n");
            }
            String newText = logContent.toString();
            // Only update if text actually changed to avoid unnecessary redraws/scroll jumps
            if (!logArea.getText().equals(newText)) {
                logArea.setText(newText);
                // Scroll to the bottom
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }

    private void stopTimer() {
        if (logRefreshTimer != null && logRefreshTimer.isRunning()) {
            logRefreshTimer.stop();
//            addSysLogEntry("System Log Dialog refresh timer stopped.");
        }
    }
}