package me.hysong.kynesystems.apps.kstradermachine;

import lombok.Getter;
import me.hysong.atlas.interfaces.KSApplication;
import me.hysong.atlas.sdk.graphite.v1.GPSplashWindow;
import me.hysong.atlas.sdk.graphite.v1.GraphiteProgramLauncher;
import me.hysong.atlas.sdk.graphite.v1.KSGraphicalApplication;
import me.hysong.atlas.sharedobj.KSEnvironment;
import me.hysong.kynesystems.apps.kstradermachine.backend.Config;
import me.hysong.kynesystems.apps.kstradermachine.backend.objects.TraderDaemon;
import me.hysong.kynesystems.apps.kstradermachine.subwins.AboutWindow;

import javax.swing.*;
import java.awt.*;
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
        return 0;
    }

    @Override
    public GPSplashWindow getSplashWindow() {
        GPSplashWindow splashWindow = new GPSplashWindow(400, 300, JLabel.RIGHT);
        splashWindow.setSplashBackend(new Thread(() -> {
            // Simulate loading process
            try {
                splashWindow.setCurrentStatus("Loading configurations...");
                Config.load("path/to/config.json"); // Set the path to your config file // TODO: Implement loading path
                Thread.sleep(3000); // Simulate loading time
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
