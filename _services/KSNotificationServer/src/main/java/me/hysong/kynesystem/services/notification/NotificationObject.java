package me.hysong.kynesystem.services.notification;

import lombok.Getter;
import lombok.Setter;
import me.hysong.atlas.async.ParameteredRunnable;
import me.hysong.atlas.kssocket.v1.objects.KSSocketPayload;

import javax.swing.*;
import java.io.Serializable;

@Getter
@Setter
public class NotificationObject implements Serializable {
    private String notificationSrvHost = "127.0.0.1";
    private int notificationSrvPort = NotificationServer.port;
    private ParameteredRunnable onClick = args -> {};
    private ParameteredRunnable onIgnored = args -> {};
    private ParameteredRunnable onError = args -> {
        if (args.length > 0)
            System.out.println("Failed to dispatch notification to NotificationServer: " + ((Exception) args[0]).getMessage());
        else
            System.out.println("Failed to dispatch notification to NotificationServer for unknown reason.");
    };
    private JPanel notificationPanel;
    private String soundFile = "";
    private int dismissInSeconds = 5;

    public NotificationObject() {

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
}
