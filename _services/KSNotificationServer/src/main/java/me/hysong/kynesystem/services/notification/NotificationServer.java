package me.hysong.kynesystem.services.notification;

import me.hysong.atlas.async.ParameteredRunnable;
import me.hysong.atlas.async.SimplePromise;
import me.hysong.atlas.interfaces.KSService;
import me.hysong.atlas.kssocket.v1.KSSocket;
import me.hysong.atlas.sharedobj.KSEnvironment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class NotificationServer implements KSService {

    KSSocket notificationServer;
    Thread notiThread;
    protected static final String KSNS_GENERIC_AUTH_SEED = "SD-250519-912832";

    protected static final int port = 36800;
    private int code = 0;

    @Override
    public int serviceMain(KSEnvironment environment, String execLocation, String[] args) {
        // Initialize the server
        System.out.println("NotificationServer is starting...");
        try {
            notificationServer = new KSSocket("127.0.0.1", port, (request, decodedPayload) -> {
                if (!NotificationObject.class.isAssignableFrom(decodedPayload.getClass())) {
                    System.out.println("[NotificationServer] Error: Received object is not NotificationObject: " + decodedPayload.getClass().getName());
                    return "error:type_error";
                }

                NotificationObject notificationObject = (NotificationObject) decodedPayload;
                JPanel notificationContentPanel = notificationObject.getNotificationPanel();
                final boolean[] isClicked = {false};
                notificationContentPanel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        isClicked[0] = true;
                        System.out.println("DETECTED CLICK (OUTER)");
                    }
                });
                for (Component c : notificationContentPanel.getComponents()) {
                    c.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            isClicked[0] = true;
                            System.out.println("DETECTED CLICK (INNER)");
                        }
                    });
                }

                // Make notification window
                JFrame frame = new JFrame();
                frame.setContentPane(notificationContentPanel);
                frame.setUndecorated(true);
                frame.setSize(100, 100);
                frame.setLocation(0, 0);
                frame.setVisible(true);

                // Play sound
                // TODO - String soundfilePath = notificationObject.getSoundFile();

                double waitedSeconds = 0;
                while (!isClicked[0] && (waitedSeconds <= notificationObject.getDismissInSeconds())) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    waitedSeconds += 0.1;
                }

                frame.dispose();

                if (isClicked[0]) {
                    return "Status=Clicked";
                } else {
                    return "Status=Timeout";
                }
            });
            notiThread = notificationServer.startThread();
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
        return 0;
    }

    @Override
    public int stop(int code) {
        if (notiThread != null) {
            notiThread.interrupt();
        }
        return 0;
    }

    public static void main(String[] args) {
        NotificationServer ns = new NotificationServer();
        ns.serviceMain(null, null, null);

        NotificationObject no = new NotificationObject();

        no.setNotificationPanel(new JPanel());

        no.setOnClick(args1 -> {
            System.out.println("Oh notification is clicked");
        });

        no.setOnIgnored(args1 -> {
            System.out.println("Oh notification is ignored!");
        });

        no.dispatch();
    }
}
