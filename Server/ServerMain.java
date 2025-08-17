package Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerMain {
    private static final int PORT = 12345;
    private static final int MAX_CLIENTS = 100;
    private static final int STATS_INTERVAL_MINUTES = 5;

    private static final AtomicInteger totalConnectionsCount = new AtomicInteger(0);
    private static final AtomicInteger currentConnectionsCount = new AtomicInteger(0);
    private static final LocalDateTime serverStartTime = LocalDateTime.now();

    private static ExecutorService clientPool;
    private static ScheduledExecutorService statsExecutor;
    private static ServerSocket serverSocket;

    public static void main(String[] args) throws IOException {
        System.out.println("===========================================");
        System.out.println("LAN Collaborative Text Editor Server");
        System.out.println("===========================================");
        System.out.println(
                "Server starting at: " + serverStartTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("Port: " + PORT);
        System.out.println("Max Clients: " + MAX_CLIENTS);

        // ADDED: Show server IP addresses
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            System.out.println("Local IP: " + localHost.getHostAddress());
            System.out.println("Server accessible at: " + localHost.getHostAddress() + ":" + PORT);
        } catch (Exception e) {
            System.out.println("Could not determine local IP address");
        }

        System.out.println("===========================================\n");

        // Initialize components
        clientPool = Executors.newFixedThreadPool(MAX_CLIENTS);
        SessionManager.init();

        // Start statistics reporting
        startStatsReporting();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        try {
            // FIXED: Bind to all network interfaces (0.0.0.0) instead of localhost
            serverSocket = new ServerSocket(PORT);
            // Alternative explicit binding:
            // serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName("0.0.0.0"));

            System.out.println(" Server started successfully on port " + PORT);
            System.out.println(" Server is accessible from other devices on the network");
            System.out.println(" Clients can connect using this machine's IP address");
            System.out.println(" Waiting for client connections...\n");

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();

                    // Update connection counters
                    int totalConnections = totalConnectionsCount.incrementAndGet();
                    int currentConnections = currentConnectionsCount.incrementAndGet();

                    System.out.println("New client connected: " + clientSocket.getInetAddress());
                    System.out.println("   Current connections: " + currentConnections);
                    System.out.println("   Total connections since start: " + totalConnections);

                    // Create and execute client handler
                    ClientHandler clientHandler = new ClientHandler(clientSocket) {
                        @Override
                        public void run() {
                            try {
                                super.run();
                            } finally {
                                // Decrement current connections when client disconnects
                                int remaining = currentConnectionsCount.decrementAndGet();
                                System.out.println("Client disconnected: " + clientSocket.getInetAddress());
                                System.out.println("   Remaining connections: " + remaining);
                            }
                        }
                    };

                    clientPool.execute(clientHandler);

                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println(" Server error: " + e.getMessage());
            System.err.println("   Make sure port " + PORT + " is not already in use");
            System.err.println("   Check if firewall is blocking the port");
        } finally {
            shutdown();
        }
    }

    private static void startStatsReporting() {
        statsExecutor = Executors.newScheduledThreadPool(1);
        statsExecutor.scheduleAtFixedRate(
                ServerMain::printServerStats,
                STATS_INTERVAL_MINUTES,
                STATS_INTERVAL_MINUTES,
                TimeUnit.MINUTES);
    }

    private static void printServerStats() {
        System.out.println("\n SERVER STATISTICS");
        System.out.println("===========================================");
        System.out.println("Server uptime: " + getUptime());
        System.out.println("Current connections: " + currentConnectionsCount.get());
        System.out.println("Total connections: " + totalConnectionsCount.get());
        System.out.println("Active sessions: " + SessionManager.getActiveSessionCount());
        System.out.println("Total active clients: " + SessionManager.getTotalActiveClients());
        System.out.println(
                "Thread pool active: " + ((java.util.concurrent.ThreadPoolExecutor) clientPool).getActiveCount());
        System.out.println("Free memory: " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + " MB");
        System.out.println("===========================================");

        // Print detailed session information
        SessionManager.printSessionStats();
    }

    private static String getUptime() {
        LocalDateTime now = LocalDateTime.now();
        long hours = java.time.Duration.between(serverStartTime, now).toHours();
        long minutes = java.time.Duration.between(serverStartTime, now).toMinutes() % 60;
        return hours + "h " + minutes + "m";
    }

    private static void shutdown() throws IOException {
        System.out.println("\n Server shutdown initiated...");

        try {
            // Close server socket
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println(" Server socket closed");
            }

            // Shutdown client pool
            if (clientPool != null) {
                clientPool.shutdown();
                if (!clientPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    clientPool.shutdownNow();
                }
                System.out.println(" Client thread pool shut down");
            }

            // Shutdown stats executor
            if (statsExecutor != null) {
                statsExecutor.shutdown();
                if (!statsExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    statsExecutor.shutdownNow();
                }
                System.out.println(" Statistics executor shut down");
            }

            // Shutdown session manager
            SessionManager.shutdown();

            System.out.println("Server shutdown completed");
            System.out.println("Final Statistics:");
            System.out.println("   Total connections served: " + totalConnectionsCount.get());
            System.out.println("   Server uptime: " + getUptime());
            System.out.println("===========================================");

        } catch (InterruptedException e) {
            System.err.println("Error during shutdown: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
