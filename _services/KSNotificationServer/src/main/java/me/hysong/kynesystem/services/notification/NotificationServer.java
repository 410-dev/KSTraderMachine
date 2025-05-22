package me.hysong.kynesystem.services.notification;

import lombok.Getter;
import lombok.Setter;
import me.hysong.atlas.interfaces.KSService;
import me.hysong.atlas.kssocket.v1.KSSocket;
import me.hysong.atlas.sharedobj.KSEnvironment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

public class NotificationServer implements KSService {

    KSSocket notificationServer;
    Thread notiThread;
    protected static final String KSNS_GENERIC_AUTH_SEED = "SD-250519-912832";

    protected static final int port = 36800;
    private final HashMap<Integer, Boolean> notificationBannerLocationIndex = new HashMap<>(); // TODO - Unclosed notification will show below existing.
    @Getter @Setter private int maximumNotificationsPerCol = 8;
    @Getter @Setter private int maximumNotificationsCols = 7;
    private final int horizontalPadding = 5;
    private final int verticalPadding = 5;

    @Override
    public int serviceMain(KSEnvironment environment, String execLocation, String[] args) {
        System.out.println("NotificationServer is starting...");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize the server
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
                frame.setSize(400, 100);
                frame.setFocusable(false);
                frame.setAlwaysOnTop(true);

                // Get empty slot location
                int x = horizontalPadding;
                int y = verticalPadding;
                int slotIndex = 0;
                boolean found = false;
                for (int i = 0; i < maximumNotificationsCols; i++) {
                    for (int ii = 0; ii < maximumNotificationsPerCol; ii++) {
                        int currentIndex = i * maximumNotificationsPerCol + ii;
                        System.out.println("I: " + i + ", II: " + ii);
                        System.out.println("Current Index: " + currentIndex + " = " + notificationBannerLocationIndex.getOrDefault(currentIndex, false));
                        if (!notificationBannerLocationIndex.getOrDefault(currentIndex, false)) {
                            x = (horizontalPadding + frame.getWidth()) * i;
                            y = (verticalPadding + frame.getHeight()) * ii;
                            notificationBannerLocationIndex.put(currentIndex, true);
                            slotIndex = currentIndex;
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
                frame.setLocation(x, y);
                System.out.println("Location set: " + x + "," + y);
                final int effectiveSlotIndex = slotIndex;

                JButton dismissButton = new JButton("Dismiss");
                dismissButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        notificationBannerLocationIndex.put(effectiveSlotIndex, false);
                        frame.dispose();
                    }
                });
                JButton okButton = new JButton("OK");
                okButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        isClicked[0] = true;
                        notificationBannerLocationIndex.put(effectiveSlotIndex, false);
                        frame.dispose();
                        System.out.println("DETECTED CLICK (BUTTON)");
                    }
                });
                JPanel buttonPanel = new JPanel();
                buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT,0,0));
                buttonPanel.add(dismissButton);
                buttonPanel.add(okButton);
                notificationContentPanel.add(buttonPanel, BorderLayout.SOUTH);



                frame.setVisible(true);
                System.out.println("OK");

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

                notificationBannerLocationIndex.put(effectiveSlotIndex, false);
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

//        NotificationObject no = new NotificationObject();
//
//        no.setNotificationPanel(new JPanel());
//
//        no.setOnClick(args1 -> {
//            System.out.println("Oh notification is clicked");
//        });
//
//        no.setOnIgnored(args1 -> {
//            System.out.println("Oh notification is ignored!");
//        });
//
//        no.dispatch();
    }
}
