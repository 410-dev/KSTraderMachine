package me.hysong.kynesystem.services.notification;

import me.hysong.atlas.interfaces.KSService;
import me.hysong.atlas.sharedobj.KSEnvironment;
import me.hysong.atlas.utils.objects.Socket2;

public class NotificationServer implements KSService {

    Socket2 notificationServer;
    Thread serverThread;
    private static final int port = 36800;
    private int code = 0;

    @Override
    public int serviceMain(KSEnvironment environment, String execLocation, String[] args) {
        // Initialize the server
        System.out.println("NotificationServer is starting...");
        notificationServer = new Socket2("localhost", port, (request, decoded) -> {
            // Handle request
            System.out.println("Received request: " + decoded);

            // Open

        });
        serverThread = new Thread(notificationServer);
        serverThread.setName("NotificationServer");
        serverThread.start();
    }

    @Override
    public int stop(int code) {
        serverThread.interrupt();
        return 0;
    }
}
