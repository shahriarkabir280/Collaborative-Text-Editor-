package Server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Session {
    private final String sessionID;
    private final StringBuilder buffer = new StringBuilder();
    private final List<ClientHandler> clients = new ArrayList<>();
    private final ConcurrentHashMap<String, ClientHandler> clientMap = new ConcurrentHashMap<>();
    private final LocalDateTime createdAt;
    private LocalDateTime lastActivity;

    public Session(String sessionID) {
        this.sessionID = sessionID;
        this.createdAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }

    public synchronized void insertText(int pos, String text) {
        if (pos < 0 || pos > buffer.length()) {
            pos = buffer.length();
        }
        buffer.insert(pos, text);
        updateLastActivity();
    }

    public synchronized void deleteText(int pos, int length) {
        if (pos >= 0 && pos < buffer.length() && length > 0) {
            // Ensure we don't delete beyond the buffer
            int actualLength = Math.min(length, buffer.length() - pos);
            buffer.delete(pos, pos + actualLength);
            updateLastActivity();
        }
    }

    public synchronized String getBuffer() {
        return buffer.toString();
    }

    public synchronized void addClient(ClientHandler client) {
        clients.add(client);
        if (client.getClientId() != null) {
            clientMap.put(client.getClientId(), client);
        }
        updateLastActivity();

        System.out.println("Client " + client.getClientName() + " added to session " + sessionID +
                ". Total clients: " + clients.size());

        // Broadcast updated user count to all clients
        broadcast("USER_COUNT:" + clients.size(), null);
    }

    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        if (client.getClientId() != null) {
            clientMap.remove(client.getClientId());
        }
        updateLastActivity();

        System.out.println("Client " + client.getClientName() + " removed from session " + sessionID +
                ". Total clients: " + clients.size());
    }

    public synchronized int getClientCount() {
        return clients.size();
    }

    public synchronized List<String> getClientNames() {
        List<String> names = new ArrayList<>();
        for (ClientHandler client : clients) {
            if (client.getClientName() != null) {
                names.add(client.getClientName());
            }
        }
        return names;
    }

    public synchronized void broadcast(String message, ClientHandler exclude) {
        // Create a copy of the list to avoid concurrent modification issues
        List<ClientHandler> clientsCopy = new ArrayList<>(clients);

        for (ClientHandler client : clientsCopy) {
            if (exclude == null || client != exclude) {
                try {
                    client.sendMessage(message);
                } catch (Exception e) {
                    System.err.println(
                            "Error sending message to client " + client.getClientName() + ": " + e.getMessage());
                    // Remove client if sending fails
                    clients.remove(client);
                    if (client.getClientId() != null) {
                        clientMap.remove(client.getClientId());
                    }
                }
            }
        }
    }

    private void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    public String getSessionID() {
        return sessionID;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public synchronized boolean isEmpty() {
        return clients.isEmpty();
    }

    public synchronized String getSessionInfo() {
        return String.format("Session %s: %d clients, created %s, last activity %s",
                sessionID,
                clients.size(),
                createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                lastActivity.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    public synchronized int getBufferLength() {
        return buffer.length();
    }

    public synchronized String getBufferPreview(int maxLength) {
        String content = buffer.toString();
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}