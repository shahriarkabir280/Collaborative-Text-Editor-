package Server;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionManager {
    private static ConcurrentHashMap<String, Session> sessions;
    private static ScheduledExecutorService cleanupExecutor;
    private static final int SESSION_TIMEOUT_HOURS = 24; // Sessions expire after 24 hours of inactivity
    private static final int CLEANUP_INTERVAL_MINUTES = 30; // Run cleanup every 30 minutes

    public static void init() {
        sessions = new ConcurrentHashMap<>();

        // Start cleanup task
        cleanupExecutor = Executors.newScheduledThreadPool(1);
        cleanupExecutor.scheduleAtFixedRate(
                SessionManager::cleanupExpiredSessions,
                CLEANUP_INTERVAL_MINUTES,
                CLEANUP_INTERVAL_MINUTES,
                TimeUnit.MINUTES);

        System.out.println("SessionManager initialized with automatic cleanup every " +
                CLEANUP_INTERVAL_MINUTES + " minutes");
    }

    public static Session getSession(String sessionID) {
        return sessions.get(sessionID);
    }

    public static Session createSession(String sessionID) {
        Session session = new Session(sessionID);
        sessions.put(sessionID, session);

        System.out.println("Created new session: " + sessionID);
        System.out.println("Total active sessions: " + sessions.size());

        return session;
    }

    public static boolean sessionExists(String sessionID) {
        return sessions.containsKey(sessionID);
    }

    public static void removeSession(String sessionID) {
        Session removed = sessions.remove(sessionID);
        if (removed != null) {
            System.out.println("Removed session: " + sessionID);
            System.out.println("Total active sessions: " + sessions.size());
        }
    }

    public static List<Session> getAllSessions() {
        return new ArrayList<>(sessions.values());
    }

    public static int getActiveSessionCount() {
        return sessions.size();
    }

    public static int getTotalActiveClients() {
        return sessions.values().stream()
                .mapToInt(Session::getClientCount)
                .sum();
    }

    public static void cleanupExpiredSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minus(SESSION_TIMEOUT_HOURS, ChronoUnit.HOURS);
        List<String> expiredSessions = new ArrayList<>();

        for (Session session : sessions.values()) {
            // Remove sessions that are empty or have been inactive for too long
            if (session.isEmpty() || session.getLastActivity().isBefore(cutoff)) {
                expiredSessions.add(session.getSessionID());
            }
        }

        for (String sessionID : expiredSessions) {
            removeSession(sessionID);
            System.out.println("Cleaned up expired/empty session: " + sessionID);
        }

        if (!expiredSessions.isEmpty()) {
            System.out.println("Cleanup completed. Removed " + expiredSessions.size() + " sessions.");
        }
    }

    public static void printSessionStats() {
        System.out.println("\n=== Session Statistics ===");
        System.out.println("Active Sessions: " + getActiveSessionCount());
        System.out.println("Total Active Clients: " + getTotalActiveClients());

        for (Session session : getAllSessions()) {
            System.out.println("  " + session.getSessionInfo());
            System.out.println("    Buffer: " + session.getBufferLength() + " chars - \"" +
                    session.getBufferPreview(50) + "\"");
            System.out.println("    Users: " + String.join(", ", session.getClientNames()));
        }
        System.out.println("========================\n");
    }

    public static void shutdown() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Clear all sessions
        sessions.clear();
        System.out.println("SessionManager shut down.");
    }
}