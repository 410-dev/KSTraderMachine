package me.hysong.atlas.kssocket.v1;

import lombok.Getter;
import lombok.Setter;
import me.hysong.atlas.async.ParameteredRunnable;
import me.hysong.atlas.kssocket.v1.objects.KSSocketPayload;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Callable;


public class KSSocket implements Runnable {
    private final String[] whitelistHosts;    // allowed client IPs
    private final int port;
    private final AcceptAction action;
    private final Authorization authorization;
    @Setter @Getter private boolean allowNonKSSocketPayload = false;

    public KSSocket(String[] whitelistHosts, int port, AcceptAction action, Authorization authorization) {
        this.whitelistHosts = whitelistHosts;
        this.port = port;
        this.action = action;
        this.authorization = authorization;
    }

    public KSSocket(String[] whitelistHosts, int port, AcceptAction action) {
        this(whitelistHosts, port, action, authorizationString -> true);
    }

    public KSSocket(String whitelistHost, int port, AcceptAction action) {
        this(new String[]{whitelistHost}, port, action);
    }

    @Override
    public void run() {
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.bind(new InetSocketAddress(port));
            System.out.println("Listening on port " + port);

            while (!Thread.currentThread().isInterrupted()) {
                try (SocketChannel clientChannel = serverChannel.accept()) {
                    InetSocketAddress remote = (InetSocketAddress) clientChannel.getRemoteAddress();
                    String clientIp = remote.getAddress().getHostAddress();

                    // Whitelist check: if non-empty and missing, reject
                    if (whitelistHosts.length > 0 && !Arrays.asList(whitelistHosts).contains(clientIp)) {
                        System.out.println("Rejected connection from " + clientIp + " (not in whitelist)");
                        clientChannel.close();
                        continue;
                    }

                    System.out.println("Accepted connection from " + clientIp);
                    handle(clientChannel.socket());
                }
            }
        } catch (ClosedByInterruptException e) {
            System.out.println("Server interrupted, shutting down.");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Thread startThread() {
        Thread t = new Thread(this, "SOCKET-" + UUID.randomUUID());
        t.start();
        return t;
    }

    private void handle(Socket client) throws IOException, ClassNotFoundException {
        try (
                ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                ObjectInputStream in  = new ObjectInputStream(client.getInputStream())
        ) {
            Object obj = in.readObject();
            // Cast to KSSocketPayload if possible
            Request req;
            if (KSSocketPayload.class.isAssignableFrom(obj.getClass())) {
                KSSocketPayload payload = (KSSocketPayload) obj;

                // Check authorization
                if (authorization == null || !authorization.check(payload.getAuthorization())) {
                    out.writeObject("error:unauthorized");
                    out.flush();
                    return;
                }

                req = new Request(client, payload.getPayload());

            } else {
                if (!allowNonKSSocketPayload) {
                    out.writeObject("error:invalid_input_type");
                    out.flush();
                    return;
                }
                req = new Request(client, obj);
            }
            Serializable response = action.run(req, req.getDecodedPayload());
            out.writeObject(response);
            out.flush();
        }
    }

    @FunctionalInterface
    public interface AcceptAction {
        Serializable run(Request request, Object decodedPayload) throws IOException;
    }

    // inner Request class unchanged…
    @lombok.Getter
    public static class Request {
        private final Socket client;
        private final Object decodedPayload;
        private final String clientIp;
        private final int port;

        public Request(Socket socket, Object decodedPayload) {
            this.client = socket;
            this.decodedPayload = decodedPayload;
            InetSocketAddress rem = (InetSocketAddress) socket.getRemoteSocketAddress();
            this.clientIp = rem.getAddress().getHostAddress();
            this.port = rem.getPort();
        }

        public Request(Socket socket, KSSocketPayload payload) {
            this.client = socket;
            this.decodedPayload = payload.getPayload();
            InetSocketAddress rem = (InetSocketAddress) socket.getRemoteSocketAddress();
            this.clientIp = rem.getAddress().getHostAddress();
            this.port = rem.getPort();
        }
    }

    @FunctionalInterface
    public static interface Authorization {
        public boolean check(String authorizationString);
    }


    public static void main(String[] args) throws InterruptedException, IOException {
        KSSocket ksc2 = new KSSocket(new String[]{"127.0.0.1"}, 39000, (request, decodedPayload) -> {
            return "Hello, " + request.getClientIp() + " with " + decodedPayload;
        });
        Thread connectorThread = ksc2.startThread();

        // Server is running now.

        // Try sending payload
        KSSocketPayload payload = new KSSocketPayload();
        payload.setPayload("Hello, World!");
        payload.setHost("localhost");
        payload.setPort(39000);
        payload.dispatchAsync(args1 -> System.out.println(Arrays.toString(args1)), args1 -> System.out.println(Arrays.toString(args1)));
//        Object o = payload.dispatchSync();

//        System.out.println(o);


//        connectorThread.interrupt(); // This should cause ClosedByInterruptException

//        connectorThread.join(5000);
//        if (connectorThread.isAlive()) {
//            System.out.println("NIO connector thread is still alive.");
//        }
//        System.out.println("Main thread finished.");
    }
}