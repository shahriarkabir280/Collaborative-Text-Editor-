package Client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Chat themes enum moved outside the class
enum ChatTheme {
    DEFAULT("Default", "#f8f9fa", "#2c3e50", "#ffffff"),
    DARK("Dark", "#2c3e50", "#ecf0f1", "#34495e"),
    BLUE("Ocean Blue", "#e3f2fd", "#1565c0", "#bbdefb"),
    GREEN("Forest Green", "#e8f5e8", "#2e7d32", "#c8e6c9"),
    PURPLE("Royal Purple", "#f3e5f5", "#7b1fa2", "#e1bee7"),
    ORANGE("Sunset Orange", "#fff3e0", "#f57c00", "#ffcc02"),
    PINK("Cherry Blossom", "#fce4ec", "#c2185b", "#f8bbd9");

    private final String name;
    private final String backgroundColor;
    private final String textColor;
    private final String inputColor;

    ChatTheme(String name, String backgroundColor, String textColor, String inputColor) {
        this.name = name;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.inputColor = inputColor;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public String getInputColor() {
        return inputColor;
    }

    @Override
    public String toString() {
        return name;
    }
}

public class TextEditorClient extends Application {

    private TextArea textArea;
    private TextField serverIPInput; // ADDED: Server IP input field
    private TextField sessionInput;
    private TextField usernameInput;
    private Button connectButton;
    private Button disconnectButton;
    private Label statusLabel;
    private Label sessionStatusLabel;
    private Button saveButton;
    private Button fontSizeIncreaseButton;
    private Button fontSizeDecreaseButton;
    private Label fontSizeLabel;

    // Chat components
    private TextArea chatArea;
    private TextField chatInput;
    private Button sendChatButton;
    private VBox chatPanel;
    private boolean chatVisible = false;
    private Button toggleChatButton;
    private ComboBox<ChatTheme> chatThemeCombo;

    // Connected users
    private Label connectedUsersLabel;
    private int connectedUsersCount = 0;

    private UIManager uiManager;
    private ClientNetwork network;
    private double currentFontSize = 14.0;

    private ChatTheme currentChatTheme = ChatTheme.DEFAULT;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("LAN Collaborative Text Editor");

        // Create main layout
        BorderPane mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #f5f5f5;");

        // Create top panel
        VBox topPanel = createTopPanel();

        // Create text editor area
        VBox editorPanel = createEditorPanel();

        // Create chat panel
        chatPanel = createChatPanel();

        // Create bottom status bar
        HBox statusBar = createStatusBar();

        // Layout assembly
        mainLayout.setTop(topPanel);
        mainLayout.setCenter(editorPanel);
        mainLayout.setBottom(statusBar);

        Scene scene = new Scene(mainLayout, 1200, 800);
        scene.getStylesheets().add("data:text/css," + getCustomCSS());

        primaryStage.setScene(scene);
        primaryStage.show();

        // Set up event handlers
        setupEventHandlers();

        //CHANGED: Initial focus on server IP
        serverIPInput.requestFocus();
    }

    private VBox createTopPanel() {
        VBox topPanel = new VBox(10);
        topPanel.setPadding(new Insets(15));
        topPanel.setStyle(
                "-fx-background-color: #2c3e50; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");

        // Title
        Label titleLabel = new Label("📝 Collaborative Text Editor");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        titleLabel.setAlignment(Pos.CENTER);

        //ADDED: Server connection area
        HBox serverBox = new HBox(15);
        serverBox.setAlignment(Pos.CENTER);

        Label serverLabel = new Label("Server IP:");
        serverLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        serverIPInput = new TextField("Enter Ip");
        serverIPInput.setPromptText("Enter server IP (e.g., 192.168.1.100)");
        serverIPInput.setPrefWidth(200);
        serverIPInput.setStyle("-fx-font-size: 14px; -fx-padding: 8px;");

        serverBox.getChildren().addAll(serverLabel, serverIPInput);

        // User info area
        HBox userBox = new HBox(15);
        userBox.setAlignment(Pos.CENTER);

        Label usernameLabel = new Label("Your Name:");
        usernameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        usernameInput = new TextField();
        usernameInput.setPromptText("Enter your display name");
        usernameInput.setPrefWidth(200);
        usernameInput.setStyle("-fx-font-size: 14px; -fx-padding: 8px;");

        Label sessionLabel = new Label("Session ID:");
        sessionLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        sessionInput = new TextField();
        sessionInput.setPromptText("Enter Session ID (e.g., project-alpha)");
        sessionInput.setPrefWidth(300);
        sessionInput.setStyle("-fx-font-size: 14px; -fx-padding: 8px;");

        connectButton = new Button("🔗 Connect");
        connectButton.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8px 15px; -fx-background-radius: 5px;");
        connectButton.setOnAction(e -> connectToServer());

        disconnectButton = new Button("🔌 Disconnect");
        disconnectButton.setStyle(
                "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8px 15px; -fx-background-radius: 5px;");
        disconnectButton.setDisable(true);
        disconnectButton.setOnAction(e -> disconnectFromServer());

        userBox.getChildren().addAll(usernameLabel, usernameInput, sessionLabel, sessionInput, connectButton,
                disconnectButton);

        // Toolbar
        HBox toolbar = createToolbar();

        //CHANGED: Added serverBox to the top panel
        topPanel.getChildren().addAll(titleLabel, serverBox, userBox, toolbar);
        return topPanel;
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER);

        saveButton = new Button("Save");
        saveButton.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6px 12px; -fx-background-radius: 3px;");
        saveButton.setDisable(true);
        saveButton.setOnAction(e -> saveFile());

        Separator sep1 = new Separator();
        sep1.setOrientation(javafx.geometry.Orientation.VERTICAL);

        fontSizeDecreaseButton = new Button("🔍➖");
        fontSizeDecreaseButton.setStyle(
                "-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6px 10px; -fx-background-radius: 3px;");
        fontSizeDecreaseButton.setDisable(true);
        fontSizeDecreaseButton.setOnAction(e -> changeFontSize(-2));

        fontSizeLabel = new Label("14px");
        fontSizeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");

        fontSizeIncreaseButton = new Button("🔍➕");
        fontSizeIncreaseButton.setStyle(
                "-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6px 10px; -fx-background-radius: 3px;");
        fontSizeIncreaseButton.setDisable(true);
        fontSizeIncreaseButton.setOnAction(e -> changeFontSize(2));

        Separator sep2 = new Separator();
        sep2.setOrientation(javafx.geometry.Orientation.VERTICAL);

        toggleChatButton = new Button("Chat");
        toggleChatButton.setStyle(
                "-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6px 12px; -fx-background-radius: 3px;");
        toggleChatButton.setDisable(true);
        toggleChatButton.setOnAction(e -> toggleChat());

        // Chat theme selector
        Label themeLabel = new Label("Theme:");
        themeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");

        chatThemeCombo = new ComboBox<>();
        chatThemeCombo.getItems().addAll(ChatTheme.values());
        chatThemeCombo.setValue(ChatTheme.DEFAULT);
        chatThemeCombo.setStyle("-fx-font-size: 11px;");
        chatThemeCombo.setDisable(true);
        chatThemeCombo.setOnAction(e -> changeChatTheme());

        connectedUsersLabel = new Label("👥 Users: 0");
        connectedUsersLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");

        toolbar.getChildren().addAll(
                saveButton, sep1,
                fontSizeDecreaseButton, fontSizeLabel, fontSizeIncreaseButton, sep2,
                toggleChatButton, themeLabel, chatThemeCombo, new Region(), connectedUsersLabel);

        HBox.setHgrow(toolbar.getChildren().get(toolbar.getChildren().size() - 2), Priority.ALWAYS);

        return toolbar;
    }

    private VBox createEditorPanel() {
        VBox editorPanel = new VBox();

        // Editor container
        HBox editorContainer = new HBox();

        // Text area
        textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setStyle(
                "-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 14px; -fx-padding: 15px; -fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-width: 1px;");
        textArea.setEditable(true);
        textArea.setPromptText("Enter server info and connect to start collaborative editing...");

        HBox.setHgrow(textArea, Priority.ALWAYS);

        editorContainer.getChildren().add(textArea);
        editorPanel.getChildren().add(editorContainer);
        VBox.setVgrow(editorContainer, Priority.ALWAYS);
        VBox.setVgrow(editorPanel, Priority.ALWAYS);
        return editorPanel;
    }

    private VBox createChatPanel() {
        VBox chatPanel = new VBox(10);
        chatPanel.setPadding(new Insets(15));
        chatPanel.setPrefWidth(300);
        chatPanel.setMaxWidth(300);
        chatPanel.setVisible(false);
        chatPanel.setManaged(false);

        Label chatTitle = new Label("Session Chat");
        chatTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPromptText("Chat messages will appear here...");
        VBox.setVgrow(chatArea, Priority.ALWAYS);

        HBox chatInputBox = new HBox(5);
        chatInput = new TextField();
        chatInput.setPromptText("Type a message...");
        chatInput.setStyle("-fx-font-size: 12px; -fx-padding: 8px;");
        HBox.setHgrow(chatInput, Priority.ALWAYS);

        sendChatButton = new Button("Send");
        sendChatButton.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 8px 12px; -fx-background-radius: 3px;");
        sendChatButton.setOnAction(e -> sendChatMessage());

        chatInputBox.getChildren().addAll(chatInput, sendChatButton);
        chatPanel.getChildren().addAll(chatTitle, chatArea, chatInputBox);

        // Apply initial theme
        applyChatTheme();

        return chatPanel;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(20);
        statusBar.setPadding(new Insets(8, 15, 8, 15));
        statusBar.setStyle("-fx-background-color: #34495e; -fx-border-color: #2c3e50; -fx-border-width: 1px 0 0 0;");
        statusBar.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 12px;");

        sessionStatusLabel = new Label("Not Connected");
        sessionStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLabel = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        timeLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 12px;");

        statusBar.getChildren().addAll(statusLabel, new Separator(), sessionStatusLabel, spacer, timeLabel);
        return statusBar;
    }

    private void setupEventHandlers() {
        // ADDED: Enter key for server IP input
        serverIPInput.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                usernameInput.requestFocus();
            }
        });

        // Enter key for username input
        usernameInput.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                sessionInput.requestFocus();
            }
        });

        // Enter key for session input
        sessionInput.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                connectToServer();
            }
        });

        // Enter key for chat input
        chatInput.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                sendChatMessage();
            }
        });

        // Text area key events
        textArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        textArea.addEventFilter(KeyEvent.KEY_TYPED, this::handleKeyTyped);
    }

    private void handleKeyPressed(KeyEvent e) {
        if (network == null)
            return;

        int caretPos = textArea.getCaretPosition();

        switch (e.getCode()) {
            case BACK_SPACE:
                e.consume();
                if (caretPos > 0) {
                    network.sendDelete(caretPos - 1, 1);
                }
                break;

            case DELETE:
                e.consume();
                if (caretPos < textArea.getLength()) {
                    network.sendDelete(caretPos, 1);
                }
                break;

            case ENTER:
                e.consume();
                network.sendInsert(caretPos, "\n");
                break;

            case TAB:
                e.consume();
                network.sendInsert(caretPos, "\t");
                break;

            case LEFT:
            case RIGHT:
            case UP:
            case DOWN:
            case HOME:
            case END:
            case PAGE_UP:
            case PAGE_DOWN:
                break;

            default:
                break;
        }
    }

    private void handleKeyTyped(KeyEvent e) {
        if (network == null)
            return;

        String typed = e.getCharacter();
        if (typed.isEmpty() || (typed.charAt(0) < 32 && !typed.equals("\t"))) {
            return;
        }

        if (typed.equals("\t")) {
            return;
        }

        e.consume();
        int caretPos = textArea.getCaretPosition();
        network.sendInsert(caretPos, typed);
    }

    // FIXED: Updated connectToServer method to use server IP
    private void connectToServer() {
        String serverIP = serverIPInput.getText().trim(); // ADDED: Get server IP
        String sessionID = sessionInput.getText().trim();
        String username = usernameInput.getText().trim();

        // ADDED: Server IP validation
        if (serverIP.isEmpty()) {
            showAlert("Server IP cannot be empty.", Alert.AlertType.ERROR);
            serverIPInput.requestFocus();
            return;
        }

        if (sessionID.isEmpty()) {
            showAlert("Session ID cannot be empty.", Alert.AlertType.ERROR);
            sessionInput.requestFocus();
            return;
        }

        if (username.isEmpty()) {
            showAlert("Please enter your display name.", Alert.AlertType.ERROR);
            usernameInput.requestFocus();
            return;
        }

        // Validate username length and characters
        if (username.length() > 20) {
            showAlert("Display name must be 20 characters or less.", Alert.AlertType.ERROR);
            usernameInput.requestFocus();
            return;
        }

        try {
            uiManager = new UIManager(textArea);
            // FIXED: Use serverIP instead of hardcoded "localhost"
            network = new ClientNetwork(serverIP, 12345, sessionID, uiManager, this, username);
            network.start();

            // Update UI
            serverIPInput.setDisable(true); // ADDED: Disable server IP input
            usernameInput.setDisable(true);
            sessionInput.setDisable(true);
            connectButton.setDisable(true);
            disconnectButton.setDisable(false);
            saveButton.setDisable(false);
            fontSizeIncreaseButton.setDisable(false);
            fontSizeDecreaseButton.setDisable(false);
            toggleChatButton.setDisable(false);
            chatThemeCombo.setDisable(false);

            // UPDATED: Status message to show server IP
            statusLabel.setText("Connected to " + serverIP + " session: " + sessionID + " as " + username);
            sessionStatusLabel.setText("Connected");
            sessionStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px; -fx-font-weight: bold;");

            textArea.requestFocus();
            textArea.setPromptText("Start typing to collaborate...");

        } catch (Exception e) {
            showAlert("Failed to connect to server: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void disconnectFromServer() {
        if (network != null) {
            network.interrupt();
            network = null;
        }

        // Reset UI
        serverIPInput.setDisable(false); // ADDED: Enable server IP input
        usernameInput.setDisable(false);
        sessionInput.setDisable(false);
        connectButton.setDisable(false);
        disconnectButton.setDisable(true);
        saveButton.setDisable(true);
        fontSizeIncreaseButton.setDisable(true);
        fontSizeDecreaseButton.setDisable(true);
        toggleChatButton.setDisable(true);
        chatThemeCombo.setDisable(true);

        statusLabel.setText("Disconnected");
        sessionStatusLabel.setText("Not Connected");
        sessionStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px; -fx-font-weight: bold;");

        textArea.clear();
        textArea.setPromptText("Enter server info and connect to start collaborative editing...");
        chatArea.clear();

        // Hide chat if visible
        if (chatVisible) {
            toggleChat();
        }
    }

    private void saveFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Document");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        File file = fileChooser.showSaveDialog(textArea.getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(textArea.getText());
                statusLabel.setText("File saved: " + file.getName());
            } catch (IOException e) {
                showAlert("Error saving file: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void changeFontSize(double delta) {
        currentFontSize += delta;
        if (currentFontSize < 8)
            currentFontSize = 8;
        if (currentFontSize > 32)
            currentFontSize = 32;

        textArea.setStyle(textArea.getStyle().replaceAll("-fx-font-size: \\d+px",
                "-fx-font-size: " + (int) currentFontSize + "px"));
        fontSizeLabel.setText((int) currentFontSize + "px");
    }

    private void changeChatTheme() {
        currentChatTheme = chatThemeCombo.getValue();
        applyChatTheme();
    }

    private void applyChatTheme() {
        if (chatPanel == null || currentChatTheme == null)
            return;

        String panelStyle = String.format(
                "-fx-background-color: %s; -fx-border-color: #bdc3c7; -fx-border-width: 0 0 0 1px;",
                currentChatTheme.getBackgroundColor());
        chatPanel.setStyle(panelStyle);

        String titleStyle = String.format(
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: %s;",
                currentChatTheme.getTextColor());
        ((Label) chatPanel.getChildren().get(0)).setStyle(titleStyle);

        String chatAreaStyle = String.format(
                "-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 12px; " +
                        "-fx-background-color: %s; -fx-text-fill: %s; " +
                        "-fx-border-color: %s; -fx-border-width: 1px; -fx-border-radius: 5px; " +
                        "-fx-background-radius: 5px;",
                currentChatTheme.getInputColor(), currentChatTheme.getTextColor(), currentChatTheme.getTextColor());
        chatArea.setStyle(chatAreaStyle);

        String inputStyle = String.format(
                "-fx-font-size: 12px; -fx-padding: 8px; " +
                        "-fx-background-color: %s; -fx-text-fill: %s; " +
                        "-fx-border-color: %s; -fx-border-width: 1px; -fx-border-radius: 3px; " +
                        "-fx-background-radius: 3px;",
                currentChatTheme.getInputColor(), currentChatTheme.getTextColor(), currentChatTheme.getTextColor());
        chatInput.setStyle(inputStyle);
    }

    private void toggleChat() {
        chatVisible = !chatVisible;

        if (chatVisible) {
            // Show chat
            HBox editorContainer = (HBox) ((VBox) ((BorderPane) textArea.getScene().getRoot()).getCenter())
                    .getChildren().get(0);
            editorContainer.getChildren().add(chatPanel);
            chatPanel.setVisible(true);
            chatPanel.setManaged(true);
            toggleChatButton.setText("Hide Chat");
            toggleChatButton.setStyle(
                    "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6px 12px; -fx-background-radius: 3px;");
        } else {
            // Hide chat
            HBox editorContainer = (HBox) ((VBox) ((BorderPane) textArea.getScene().getRoot()).getCenter())
                    .getChildren().get(0);
            editorContainer.getChildren().remove(chatPanel);
            chatPanel.setVisible(false);
            chatPanel.setManaged(false);
            toggleChatButton.setText("Chat");
            toggleChatButton.setStyle(
                    "-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6px 12px; -fx-background-radius: 3px;");
        }
    }

    private void sendChatMessage() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty() && network != null) {
            network.sendChatMessage(message);
            chatInput.clear();
        }
    }

    public void addChatMessage(String message) {
        javafx.application.Platform.runLater(() -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            chatArea.appendText("[" + timestamp + "] " + message + "\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    public void updateUserCount(int count) {
        javafx.application.Platform.runLater(() -> {
            connectedUsersCount = count;
            connectedUsersLabel.setText("👥 Users: " + count);
        });
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String getCustomCSS() {
        return """
                .text-area {
                    -fx-background-color: white;
                    -fx-border-color: #bdc3c7;
                    -fx-border-width: 1px;
                    -fx-border-radius: 5px;
                    -fx-background-radius: 5px;
                }

                .text-field {
                    -fx-border-color: #bdc3c7;
                    -fx-border-width: 1px;
                    -fx-border-radius: 3px;
                    -fx-background-radius: 3px;
                }

                .button:hover {
                    -fx-opacity: 0.8;
                }

                .button:pressed {
                    -fx-opacity: 0.6;
                }

                .combo-box {
                    -fx-background-color: white;
                    -fx-border-radius: 3px;
                    -fx-background-radius: 3px;
                }
                """;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
