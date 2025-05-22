package me.hysong.kynesystems.apps.kstradermachine.front.uiobjects;

import lombok.Getter;
import lombok.Setter;
import me.hysong.atlas.async.ParameteredRunnable;
import me.hysong.kynesystems.apps.kstradermachine.objects.Daemon;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class DaemonPanel extends JPanel {
    private JLabel title_symbolAndExchange;
    private JLabel desc_strategyName;
    private ParameteredRunnable paintingAlgorithm;
    private Color preferredColor;
    private int flashLatency = 80;
    private Thread refreshThread;
    private Daemon holdingDaemon;
    private boolean terminateRefreshThread = false;

    public static final Color COLOR_OPERATING = Color.GREEN;
    public static final Color COLOR_STARTING_UP = Color.ORANGE;
    public static final Color COLOR_ERROR = Color.RED;
    public static final Color COLOR_NOT_RUNNING = Color.GRAY;
    public static final Color COLOR_WAITING = Color.CYAN;
    public static Color COLOR_IDLE = Color.WHITE; // If dark theme, it could be black.
    public static Color COLOR_TEXT = Color.BLACK; // If dark theme, it could be white.

    // arg 0: JPanel
    // arg 1: Current color
    // arg 2: Preferred color
    public static final ParameteredRunnable REPAINT_FLASHING = args -> {
        Color next;
        if (args[1].equals(args[2])) next = COLOR_IDLE;
        else next = (Color) args[1];
        ((JPanel) args[0]).setBackground(next);
    };

    public static final ParameteredRunnable REPAINT_STATIC = args -> {
        ((JPanel) args[0]).setBackground((Color) args[2]);
    };

    public enum DaemonStatusOutlook {
        OPERATING, STARTING_UP, ERROR, EMERGENCY, NOT_RUNNING
    }

    public DaemonPanel(String title_symbolAndExchange, String desc_strategyName, DaemonStatusOutlook outlook, Daemon daemon) {
        this.holdingDaemon = daemon;
        setStatus(outlook);
        refreshThread = new Thread(() -> {
            while (!terminateRefreshThread) {
                setForeground(COLOR_TEXT);
                paintingAlgorithm.run(this, getBackground(), preferredColor);
                repaint();
                if (daemon.getCfg().getSymbol() != null && !daemon.getCfg().getSymbol().isEmpty() && daemon.getDriverManifest() != null && daemon.getStrategyManifest() != null) {
                    this.title_symbolAndExchange.setText(daemon.getCfg().getSymbol() + "@" + daemon.getDriverManifest().getDriverExchangeName());
                    this.desc_strategyName.setText(daemon.getStrategyManifest().getStrategyName());
                }
                try {
                    Thread.sleep(flashLatency);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY)); // Or any other color

        // Add JLabels to display the text
        this.title_symbolAndExchange = new JLabel(title_symbolAndExchange);
        this.desc_strategyName = new JLabel(desc_strategyName);
        // Potentially set alignment, font, etc. on labels

        this.title_symbolAndExchange.setHorizontalAlignment(SwingConstants.CENTER);
        this.title_symbolAndExchange.setFont(this.title_symbolAndExchange.getFont().deriveFont(Font.BOLD));

        this.desc_strategyName.setHorizontalAlignment(SwingConstants.CENTER);

        setLayout(new GridLayout(2, 1)); // Or another layout like BorderLayout
        add(this.title_symbolAndExchange);
        add(this.desc_strategyName);


        refreshThread.start();
    }

    public void setStatus(DaemonStatusOutlook outlook) {
        switch (outlook) {
            case DaemonStatusOutlook.OPERATING:
                this.preferredColor = COLOR_OPERATING;
                this.paintingAlgorithm = REPAINT_FLASHING;
                break;

            case DaemonStatusOutlook.STARTING_UP:
                this.preferredColor = COLOR_STARTING_UP;
                this.paintingAlgorithm = REPAINT_STATIC;
                break;

            case DaemonStatusOutlook.ERROR:
                this.preferredColor = COLOR_ERROR;
                this.paintingAlgorithm = REPAINT_STATIC;
                break;

            case DaemonStatusOutlook.EMERGENCY:
                this.preferredColor = COLOR_ERROR;
                this.paintingAlgorithm = REPAINT_FLASHING;
                break;

            case DaemonStatusOutlook.NOT_RUNNING:
                this.preferredColor = COLOR_NOT_RUNNING;
                this.paintingAlgorithm = REPAINT_STATIC;
                break;

            default:
                this.preferredColor = COLOR_IDLE;
                this.paintingAlgorithm = REPAINT_STATIC;
                break;
        }
    }
}
