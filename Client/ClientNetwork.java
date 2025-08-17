package Client;

import java.io.*;
import java.net.Socket;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

public class ClientNetwork extends Thread {
    private final String serverAddress;
    private final int port;
    private final String sessionID;
    private final UIManager uiManager;
    private final TextEditorClient clientApp;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private final AtomicLong operationIdCounter = new AtomicLong(0);
    private final String clientId;
    private final String clientName;

    // Updated constructor to accept custom username
    public ClientNetwork(String serverAddress, int port, String sessionID, UIManager uiManager,
            TextEditorClient clientApp, String customUsername) {
        this.serverAddress = serverAddress;
        this.port = port;
        this.sessionID = sessionID;
        this.uiManager = uiManager;
        this.clientApp = clientApp;
        this.clientId = "CLIENT_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
        // Use the custom username instead of generating a random one
        this.clientName = customUsername;
    }

    // Backward compatibility constructor (in case you want to keep the old behavior
    // somewhere)
    public ClientNetwork(String serverAddress, int port, String sessionID, UIManager uiManager,
            TextEditorClient clientApp) {
        this(serverAddress, port, sessionID, uiManager, clientApp, "User" + (int) (Math.random() * 1000));
    }

    @Override
    public void run() {
        try {
            socket = new Socket(serverAddress, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Send session join request with client info
            out.println("SESSION:" + sessionID + ":" + clientId + ":" + clientName);
            System.out.println("Connected to session: " + sessionID + " as " + clientName);

            String line;
            while ((line = in.readLine()) != null && !isInterrupted()) {
                System.out.println("Received: " + line);

                if (line.startsWith("EDIT:")) {
                    handleEditMessage(line);
                } else if (line.startsWith("FULL_BUFFER:")) {
                    handleFullBuffer(line);
                } else if (line.startsWith("CHAT:")) {
                    handleChatMessage(line);
                } else if (line.startsWith("USER_COUNT:")) {
                    handleUserCount(line);
                } else if (line.startsWith("USER_JOINED:")) {
                    handleUserJoined(line);
                } else if (line.startsWith("USER_LEFT:")) {
                    handleUserLeft(line);
                }
            }
        } catch (IOException e) {
            if (!isInterrupted()) {
                System.err.println("ClientNetwork error: " + e.getMessage());
            }
        } finally {
            cleanup();
        }
    }

    private void handleEditMessage(String line) {
        try {
            String[] parts = line.split(":", 6);
            if (parts.length < 6)
                return;

            String opType = parts[1];
            int pos = Integer.parseInt(parts[2]);
            String sourceClientId = parts[4];
            String operationId = parts[5];

            System.out.println("Processing " + opType + " at position " + pos + " from client " + sourceClientId);

            if (opType.equals("INSERT")) {
                String encodedText = parts[3];
                String text = URLDecoder.decode(encodedText, StandardCharsets.UTF_8.toString());
                uiManager.insertText(pos, text);
                System.out.println("Inserted '" + text + "' at position " + pos);
            } else if (opType.equals("DELETE")) {
                int length = Integer.parseInt(parts[3]);
                uiManager.deleteText(pos, length);
                System.out.println("Deleted " + length + " characters at position " + pos);
            }
        } catch (Exception e) {
            System.err.println("Error handling edit message: " + e.getMessage());
        }
    }

    private void handleFullBuffer(String line) {
        try {
            String encodedFullText = line.substring("FULL_BUFFER:".length());
            String fullText = URLDecoder.decode(encodedFullText, StandardCharsets.UTF_8.toString());
            uiManager.setText(fullText);
            System.out.println("Set full buffer: '" + fullText + "'");
        } catch (Exception e) {
            System.err.println("Error handling full buffer: " + e.getMessage());
        }
    }

    private void handleChatMessage(String line) {
        try {
            // Format: CHAT:senderName:encodedMessage
            String[] parts = line.split(":", 3);
            if (parts.length < 3)
                return;

            String senderName = parts[1];
            String encodedMessage = parts[2];
            String message = URLDecoder.decode(encodedMessage, StandardCharsets.UTF_8.toString());

            // Don't show our own messages (they're already shown locally)
            if (!senderName.equals(clientName)) {
                clientApp.addChatMessage(senderName + ": " + message);
            }
        } catch (Exception e) {
            System.err.println("Error handling chat message: " + e.getMessage());
        }
    }

    private void handleUserCount(String line) {
        try {
            String countStr = line.substring("USER_COUNT:".length());
            int count = Integer.parseInt(countStr);
            clientApp.updateUserCount(count);
        } catch (Exception e) {
            System.err.println("Error handling user count: " + e.getMessage());
        }
    }

    private void handleUserJoined(String line) {
        try {
            String userName = line.substring("USER_JOINED:".length());
            if (!userName.equals(clientName)) {
                clientApp.addChatMessage(userName + " joined the session");
            }
        } catch (Exception e) {
            System.err.println("Error handling user joined: " + e.getMessage());
        }
    }

    private void handleUserLeft(String line) {
        try {
            String userName = line.substring("USER_LEFT:".length());
            if (!userName.equals(clientName)) {
                clientApp.addChatMessage(userName + " left the session");
            }
        } catch (Exception e) {
            System.err.println("Error handling user left: " + e.getMessage());
        }
    }

    public void sendInsert(int pos, String text) {
        try {
            String operationId = String.valueOf(operationIdCounter.incrementAndGet());
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
            String message = "EDIT:INSERT:" + pos + ":" + encodedText + ":" + clientId + ":" + operationId;

            out.println(message);
            System.out.println("Sent INSERT: " + message);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void sendDelete(int pos, int length) {
        String operationId = String.valueOf(operationIdCounter.incrementAndGet());
        String message = "EDIT:DELETE:" + pos + ":" + length + ":" + clientId + ":" + operationId;

        out.println(message);
        System.out.println("Sent DELETE: " + message);
    }

    public void sendChatMessage(String message) {
        try {
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString());
            String chatMessage = "CHAT:" + clientName + ":" + encodedMessage;

            out.println(chatMessage);

            // Show our own message locally
            clientApp.addChatMessage(clientName + " (You): " + message);

            System.out.println("Sent CHAT: " + chatMessage);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public String getClientName() {
        return clientName;
    }

    public String getClientId() {
        return clientId;
    }

    private void cleanup() {
        try {
            if (socket != null && !socket.isClosed())
                socket.close();
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
