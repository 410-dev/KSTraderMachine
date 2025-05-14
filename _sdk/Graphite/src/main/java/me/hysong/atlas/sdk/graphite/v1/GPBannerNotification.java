package me.hysong.atlas.sdk.graphite.v1;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class GPBannerNotification extends JFrame {



    public static final int CORNER_TOP_LEFT = 0;
    public static final int CORNER_TOP_RIGHT = 1;
    public static final int CORNER_BOTTOM_LEFT = 2;
    public static final int CORNER_BOTTOM_RIGHT = 3;

    private JLabel titleLabel;
    private JLabel messageLabel;

    private int width;
    private int height;
    private int cornerLocation;

    private int openDuration;

    private Runnable onClickAction;

    public GPBannerNotification(String title, String message, int cornerLocation) {
        this.titleLabel = new JLabel(title);
        this.messageLabel = new JLabel(message);

        this.width = 300;
        this.height = 100;

        this.openDuration = 5000; // Default to 5 seconds
        this.cornerLocation = cornerLocation;

        setTitle("Notification");
        setSize(width, height);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new java.awt.BorderLayout());

        add(titleLabel, java.awt.BorderLayout.NORTH);
        add(messageLabel, java.awt.BorderLayout.CENTER);

        // Dispose the notification when clicked
        onClickAction = this::dispose;
    }


    public void showNotification() {
        // Get the screen size
//        setLocationRelativeTo(null);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = 0;
        int y = switch (cornerLocation) {
            case CORNER_TOP_LEFT -> 0;
            case CORNER_TOP_RIGHT -> {
                x = screenSize.width - width;
                yield 0;
            }
            case CORNER_BOTTOM_LEFT -> screenSize.height - height;
            case CORNER_BOTTOM_RIGHT -> {
                x = screenSize.width - width;
                yield screenSize.height - height;
            }
            default -> throw new IllegalArgumentException("Invalid corner location: " + cornerLocation);
        };
        // Calculate the position based on the corner location
        setLocation(x, y);
        setAlwaysOnTop(true);
        setFocusable(false);
        setUndecorated(true);

        // For each containing components, add a mouse listener to dispose the notification
        for (Component component : getComponents()) {
            component.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    onClickAction.run();
                }
            });
        }

        setVisible(true);

        // Automatically close the notification after the specified duration
        new Timer(openDuration, e -> dispose()).start();
    }
}
