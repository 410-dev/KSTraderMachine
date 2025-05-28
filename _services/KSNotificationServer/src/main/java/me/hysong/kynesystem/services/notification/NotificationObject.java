package me.hysong.kynesystem.services.notification;

import lombok.Getter;
import lombok.Setter;
import me.hysong.atlas.async.ParameteredRunnable;
import me.hysong.atlas.kssocket.v1.objects.KSSocketPayload;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;

@Getter
@Setter
public class NotificationObject implements Serializable {
    private String notificationSrvHost = "127.0.0.1";
    private int notificationSrvPort = NotificationServer.port;
    private transient ParameteredRunnable onClick = args -> {};
    private transient ParameteredRunnable onIgnored = args -> {};
    private transient ParameteredRunnable onError = args -> {
        if (args.length > 0) {
            System.out.println("Failed to dispatch notification to NotificationServer: " + args[0]);
            ((Exception) args[0]).printStackTrace();
        } else {
            System.out.println("Failed to dispatch notification to NotificationServer for unknown reason.");
            }
    };
    private JPanel notificationPanel;
    private String soundFile = "";
    private int dismissInSeconds = 5;

    public NotificationObject(ParameteredRunnable onClick, ParameteredRunnable onIgnored, String title, String text) {
        this.onClick = onClick;
        this.onIgnored = onIgnored;
        JPanel genericDisplayPanel = new JPanel();
        genericDisplayPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        genericDisplayPanel.setLayout(new BorderLayout());
        JLabel titleLabel = new JLabel(title);
        JLabel textLabel = new JLabel(text);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        textLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        genericDisplayPanel.add(titleLabel, BorderLayout.NORTH);
        genericDisplayPanel.add(textLabel, BorderLayout.CENTER);

        this.notificationPanel = genericDisplayPanel;
    }

    public NotificationObject(ParameteredRunnable onClick, ParameteredRunnable onIgnored, JPanel notificationPanel) {
        this.onClick = onClick;
        this.onIgnored = onIgnored;
        this.notificationPanel = notificationPanel;
    }

    public void dispatch() {
        KSSocketPayload payload = new KSSocketPayload(this, notificationSrvHost, notificationSrvPort, NotificationServer.KSNS_GENERIC_AUTH_SEED);
        payload.dispatchAsync((ParameteredRunnable) args -> {
            if (args.length != 1) {
                throw new RuntimeException("Unidentified action format (Length issue)");
            }
            String serverout = args[0].toString();
            if (!serverout.startsWith("Status=")) {
                throw new RuntimeException("Unidentified action format (Format issue). Got: " + serverout);
            }
            serverout = serverout.substring("Status=".length());
            if (serverout.equals("Clicked") && onClick != null) {
                onClick.run();
            } else if (onIgnored != null) {
                onIgnored.run();
            }
        }, onError);
    }

    public static boolean isServerUp() {
        NotificationObject no = new NotificationObject(null, null, null, null);
        KSSocketPayload payload = new KSSocketPayload(new NotificationServerTestingObject("Hello from Client 1"), no.notificationSrvHost, no.notificationSrvPort, NotificationServer.KSNS_GENERIC_AUTH_SEED);
        try {
            Object o = payload.dispatchSync();
            if (o instanceof NotificationServerTestingObject to) {
                return to.getMessage().equals("Hello from Server 1");
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
