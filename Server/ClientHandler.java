package Server;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Session session;
    private String clientId;
    private String clientName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String inputLine = in.readLine();
            if (inputLine != null && inputLine.startsWith("SESSION:")) {
                // Parse session join request: SESSION:sessionID:clientId:clientName
                String[] parts = inputLine.split(":", 4);
                if (parts.length < 4) {
                    System.err.println("Invalid session join format: " + inputLine);
                    return;
                }

                String sessionID = parts[1].trim();
                this.clientId = parts[2].trim();
                this.clientName = parts[3].trim();

                session = SessionManager.sessionExists(sessionID)
                        ? SessionManager.getSession(sessionID)
                        : SessionManager.createSession(sessionID);

                session.addClient(this);

                // Send initial buffer
                String encodedFullBuffer = URLEncoder.encode(session.getBuffer(), StandardCharsets.UTF_8.toString());
                sendMessage("FULL_BUFFER:" + encodedFullBuffer);

                // Send current user count
                sendMessage("USER_COUNT:" + session.getClientCount());

                // Notify other clients about new user
                session.broadcast("USER_JOINED:" + clientName, this);

                System.out.println("Client " + clientName + " (" + clientId + ") joined session " + sessionID);

                // Process incoming operations
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("EDIT:")) {
                        processEditOperation(inputLine);
                    } else if (inputLine.startsWith("CHAT:")) {
                        processChatMessage(inputLine);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("ClientHandler error for " + socket.getInetAddress() + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void processEditOperation(String inputLine) {
        try {
            String[] parts = inputLine.split(":", 6);
            if (parts.length < 6)
                return;

            String opType = parts[1];
            int requestedPos = Integer.parseInt(parts[2]);
            String clientId = parts[4];
            String operationId = parts[5];

            if (opType.equals("INSERT")) {
                handleInsertOperation(parts, requestedPos, clientId, operationId);
            } else if (opType.equals("DELETE")) {
                handleDeleteOperation(parts, requestedPos, clientId, operationId);
            }
        } catch (Exception e) {
            System.err.println("Error processing edit operation: " + e.getMessage());
        }
    }

    private void processChatMessage(String inputLine) {
        try {
            // Format: CHAT:senderName:encodedMessage
            String[] parts = inputLine.split(":", 3);
            if (parts.length < 3)
                return;

            String senderName = parts[1];
            String encodedMessage = parts[2];

            // Broadcast chat message to all other clients (not the sender)
            session.broadcast("CHAT:" + senderName + ":" + encodedMessage, this);

            // Decode for logging
            String message = URLDecoder.decode(encodedMessage, StandardCharsets.UTF_8.toString());
            System.out.println("Chat from " + senderName + ": " + message);

        } catch (Exception e) {
            System.err.println("Error processing chat message: " + e.getMessage());
        }
    }

    private void handleInsertOperation(String[] parts, int requestedPos, String clientId, String operationId) {
        try {
            // Decode received text
            String encodedText = parts[3];
            String text = URLDecoder.decode(encodedText, StandardCharsets.UTF_8.toString());

            // Validate and adjust position
            int actualPos = Math.max(0, Math.min(requestedPos, session.getBuffer().length()));

            // Perform the insertion
            session.insertText(actualPos, text);

            // Broadcast to ALL clients (including sender) with the actual position used
            String broadcastEncodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
            String broadcastMessage = "EDIT:INSERT:" + actualPos + ":" + broadcastEncodedText + ":" + clientId + ":"
                    + operationId;

            session.broadcast(broadcastMessage, null);

            System.out.println("Processed INSERT from " + clientName + " (" + clientId + ") at position " + actualPos
                    + " (requested: " + requestedPos + ") text: '" + text + "'");
            System.out.println("Buffer is now: '" + session.getBuffer() + "'");

        } catch (Exception e) {
            System.err.println("Error handling insert operation: " + e.getMessage());
        }
    }

    private void handleDeleteOperation(String[] parts, int requestedPos, String clientId, String operationId) {
        try {
            int requestedLength = Integer.parseInt(parts[3]);

            // Validate position and length
            int bufferLength = session.getBuffer().length();
            if (requestedPos < 0 || requestedPos >= bufferLength) {
                System.out.println("Invalid delete position " + requestedPos + " for buffer length " + bufferLength);
                return;
            }

            // Calculate actual deletion length
            int actualLength = Math.min(requestedLength, bufferLength - requestedPos);
            if (actualLength <= 0) {
                System.out.println("No text to delete at position " + requestedPos);
                return;
            }

            // Perform the deletion
            session.deleteText(requestedPos, actualLength);

            // Broadcast to ALL clients with actual parameters
            String broadcastMessage = "EDIT:DELETE:" + requestedPos + ":" + actualLength + ":" + clientId + ":"
                    + operationId;
            session.broadcast(broadcastMessage, null);

            System.out.println("Processed DELETE from " + clientName + " (" + clientId + ") at position " + requestedPos
                    + " length " + actualLength);
            System.out.println("Buffer is now: '" + session.getBuffer() + "'");

        } catch (Exception e) {
            System.err.println("Error handling delete operation: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        if (out != null && !out.checkError()) {
            out.println(message);
        }
    }

    public String getClientName() {
        return clientName;
    }

    public String getClientId() {
        return clientId;
    }

    private void cleanup() {
        if (session != null) {
            session.removeClient(this);
            // Notify other clients about user leaving
            if (clientName != null) {
                session.broadcast("USER_LEFT:" + clientName, this);
                session.broadcast("USER_COUNT:" + session.getClientCount(), null);
            }
        }
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