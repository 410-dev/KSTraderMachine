package me.hysong.atlas.utils.objects;

import lombok.Getter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

public class Socket2 implements Runnable {
    private final String host;
    private final int port;
    private final AcceptAction action;

    public Socket2(String host, int port, AcceptAction action) {
        this.host = host;
        this.port = port;
        this.action = action;
    }

    @Override
    public void run() {
        try (SocketChannel socketChannel = SocketChannel.open()) {
            System.out.println("Thread " + Thread.currentThread().getName() + " attempting to connect to " + host + ":" + port);
            // socketChannel.configureBlocking(true); // Default is blocking

            socketChannel.connect(new InetSocketAddress(host, port)); // Blocking call

            if (socketChannel.isConnected()) {
                String uuid = UUID.randomUUID().toString();
                System.out.println("Thread " + Thread.currentThread().getName() + " connected successfully. Opening action thread: " + uuid);

                // Set to non-blocking mode
                Thread execute = new Thread(() -> {
                    try {
                        // Get raw data
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int bytesRead = socketChannel.read(buffer);
                        buffer.flip(); // Prepare buffer for reading
                        byte[] data = new byte[bytesRead];
                        buffer.get(data);
                        Request request = new Request(socketChannel, data);

                        String output = action.run(request, request.decoded);

                        // Simulate some work
                        ByteBuffer outputBuffer = ByteBuffer.wrap(output.getBytes());
                        socketChannel.write(outputBuffer);
                        Thread.sleep(1000); // Keep connection for a bit
                        System.out.println("Thread " + Thread.currentThread().getName() + " closing connection.");

                    } catch (ClosedByInterruptException e) {
                        System.out.println("Thread " + Thread.currentThread().getName() + " was interrupted during connect/IO, channel closed. Exception: " + e.getMessage());
                        Thread.currentThread().interrupt(); // Re-set interrupt status as ClosedByInterruptException clears it.
                    } catch (IOException e) {
                        System.err.println("Thread " + Thread.currentThread().getName() + " IOException: " + e.getMessage());
                    } catch (InterruptedException e) { // If sleep or other interruptible op is interrupted
                        System.out.println("Thread " + Thread.currentThread().getName() + " was interrupted (e.g. during sleep). Exception: " + e.getMessage());
                        Thread.currentThread().interrupt(); // Good practice
                    }
                }, uuid);
                execute.start();
            }
        } catch (ClosedByInterruptException e) {
            System.out.println("Thread " + Thread.currentThread().getName() + " was interrupted during connect/IO, channel closed. Exception: " + e.getMessage());
            Thread.currentThread().interrupt(); // Re-set interrupt status as ClosedByInterruptException clears it.
        } catch (IOException e) {
            System.err.println("Thread " + Thread.currentThread().getName() + " IOException: " + e.getMessage());
        }
        System.out.println("Thread " + Thread.currentThread().getName() + " has finished.");
    }

    @FunctionalInterface
    public static interface AcceptAction {
        String run(Request request, Object decoded) throws IOException;
    }

    @Getter
    public static class Request {
        private final SocketChannel socketChannel;
        private final Object decoded;
        private final byte[] rawData;

        private final String clientIp;
        private final int port;

        public Request(SocketChannel socketChannel, byte[] data) throws IOException {
            this.socketChannel = socketChannel;
            this.rawData = data;
            this.decoded = new String(data);

            InetSocketAddress remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
            this.clientIp = remoteAddress.getAddress().getHostAddress();
            this.port = remoteAddress.getPort();
        }
    }

//    public static void main(String[] args) throws InterruptedException {
//        // Start a simple server on port 9090 first (e.g., netcat: nc -lp 9090)
//        // Or use a non-existent server to see connect block
//        Socket2 connector = new Socket2("localhost", 9091); // Assuming 9091 is not listening
//        Thread connectorThread = new Thread(connector, "NioConnectorThread");
//        connectorThread.start();
//
//        Thread.sleep(3000); // Let it try to connect
//
//        System.out.println("Main thread interrupting NioConnectorThread...");
//        connectorThread.interrupt(); // This should cause ClosedByInterruptException
//
//        connectorThread.join(5000);
//        if (connectorThread.isAlive()) {
//            System.out.println("NIO connector thread is still alive.");
//        }
//        System.out.println("Main thread finished.");
//    }
}