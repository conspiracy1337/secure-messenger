package cns.client.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    // Menu Items
    @FXML private MenuItem menuExit;
    @FXML private MenuItem menuGenerateKeys;
    @FXML private MenuItem menuExportKey;
    @FXML private MenuItem menuBackupKeys;
    @FXML private MenuItem menuImportKeys;
    @FXML private MenuItem menuAddContact;
    @FXML private MenuItem menuManageContacts;
    @FXML private MenuItem menuAbout;
    @FXML private MenuItem menuChangePassword;
    @FXML private MenuItem menuDeleteData;


    // Contact Panel
    @FXML private TextField searchField;
    @FXML private Button addContactButton;
    @FXML private ListView<String> contactListView;
    @FXML private Circle connectionStatusCircle;
    @FXML private Label connectionStatusLabel;

    // Chat Header
    @FXML private Label contactAvatarLabel;
    @FXML private Circle contactStatusCircle;
    @FXML private Label chatContactName;
    @FXML private Label chatContactStatus;
    @FXML private Button viewContactInfoButton;
    @FXML private Button searchMessagesButton;

    // Messages
    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox messagesContainer;

    // Message Composition
    @FXML private HBox attachmentPreview;
    @FXML private Label attachmentLabel;
    @FXML private Label attachmentSizeLabel;
    @FXML private Button removeAttachmentButton;
    @FXML private Button attachFileButton;
    @FXML private Button emojiButton;
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;
    @FXML private Label encryptionStatusLabel;
    @FXML private Label characterCountLabel;

    // Status Bar
    @FXML private Label userHashLabel;
    @FXML private Label statusMessageLabel;
    @FXML private BorderPane rootBorderPane;

    // Self Profile
    @FXML private Label selfAvatarLabel;
    @FXML private Label selfStatusLabel;
    @FXML private HBox selfPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupEventHandlers();
        setupContactList();
        setupAutoScroll();
        setupCharacterCounter();
        Platform.runLater(() -> {
            if (rootBorderPane != null) {
                rootBorderPane.requestFocus();
            }
        });
    }

    private void setupEventHandlers() {
        // Menu actions
        if (menuGenerateKeys != null) {
            menuGenerateKeys.setOnAction(e -> handleGenerateKeys());
        }
        if (menuExportKey != null) {
            menuExportKey.setOnAction(e -> handleExportKey());
        }
        if (menuAddContact != null) {
            menuAddContact.setOnAction(e -> handleAddContact());
        }
        if (menuAbout != null) {
            menuAbout.setOnAction(e -> handleAbout());
        }
        if (menuChangePassword != null) { // Delete Data
            menuChangePassword.setOnAction(e -> handleChangePassword());
        }
        if (menuDeleteData != null) { // Delete Data
            menuDeleteData.setOnAction(e -> handleDeleteData());
        }
        if (menuExit != null) {
            menuExit.setOnAction(e -> handleExit());
        }

        // Button actions
        if (searchMessagesButton != null) {
            searchMessagesButton.setOnAction(e -> handleSearchMessages());
        }
        if (viewContactInfoButton != null) {
            viewContactInfoButton.setOnAction(e -> handleViewContactInfo());
        }
        if (attachFileButton != null) {
            attachFileButton.setOnAction(e -> handleAttachFile());
        }
        if (emojiButton != null) {
            emojiButton.setOnAction(e -> handleEmoji());
        }
        if (sendButton != null) {
            sendButton.setOnAction(e -> handleSendMessage());
        }
    }

    private void setupContactList() {
        // TODO: Setup contact list cell factory
    }

    private void setupAutoScroll() {
        // TODO: Setup auto-scroll for messages
    }

    private void setupCharacterCounter() {
        // TODO: Setup character counter for message input
    }

    // ========== Window Opening Utility ==========

    /**
     * Opens a modal window with the specified FXML file
     * @param fxmlPath Path to the FXML file
     * @param title Window title
     * @param modal Whether the window should be modal
     */
    private void openWindow(String fxmlPath, String title, boolean modal, double width, double height, boolean resizable) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setResizable(resizable);

            if (modal) {
                stage.initModality(Modality.APPLICATION_MODAL);
            }

            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(
                    getClass().getResource("/cns/client/ui/styles.css").toExternalForm()
            );

            stage.setScene(scene);

            if (modal) {
                stage.showAndWait();
            } else {
                stage.show();
            }

        } catch (IOException e) {
            System.err.println("Failed to open window: " + title);
            e.printStackTrace();
        }
    }

    /**
     * Opens a window and returns its controller
     * @param fxmlPath Path to the FXML file
     * @param title Window title
     * @param modal Whether the window should be modal
     * @return The controller instance
     */
    private <T> T openWindowWithController(String fxmlPath, String title, boolean modal) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(title);

            if (modal) {
                stage.initModality(Modality.APPLICATION_MODAL);
            }

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/cns/client/ui/styles.css").toExternalForm()
            );

            stage.setScene(scene);

            T controller = loader.getController();

            if (modal) {
                stage.showAndWait();
            } else {
                stage.show();
            }

            return controller;

        } catch (IOException e) {
            System.err.println("Failed to open window: " + title);
            e.printStackTrace();
            showError("Error", "Failed to open " + title);
            return null;
        }
    }

    /**
     * Shows an error dialog
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ========== Message Display ==========

    private void addMessageToUI(String messageText, boolean isSent, long timestamp, String status) {
        // TODO: Add message to UI
    }

    private String formatTimestamp(long timestamp) {
        // TODO: Format timestamp
        return "";
    }

    private String getStatusIcon(String status) {
        // TODO: Return status icon
        return "";
    }

    // ========== Event Handlers ==========

    private void handleGenerateKeys() {
        openWindow("/cns/client/ui/GenerateNewKeys.fxml", "Generate New Keys", true, 575, 750, false);
    }

    private void handleExportKey() {
        openWindow("/cns/client/ui/ShareKey.fxml", "Share Public Key", true, 500, 840, false);
    }

    private void handleBackupKeys() {
        // TODO: Handle backup keys
        showError("Not Implemented", "Backup keys feature coming soon!");
    }

    private void handleImportKeys() {
        // TODO: Handle import keys
        showError("Not Implemented", "Import keys feature coming soon!");
    }

    private void handleAddContact() {
        openWindow("/cns/client/ui/AddContact.fxml", "Add Contact", true, 500, 730, false);
    }

    private void handleAbout() {
        openWindow("/cns/client/ui/About.fxml", "About", true, 450, 650, false);
    }

    private void handleChangePassword() {
        openWindow("/cns/client/ui/ChangePassword.fxml", "Change Password", true, 550, 750, false);
    }

    private void handleDeleteData() {
        openWindow("/cns/client/ui/DeleteData.fxml", "Delete All Data", true, 515, 720, false);
    }

    private void handleExit() {
        Platform.exit();
    }

    private void handleSearchMessages() {
        // TODO: Handle search messages
        showError("Not Implemented", "Search messages feature coming soon!");
    }

    private void handleContactSelection(String contact) {
        // TODO: Handle contact selection
    }

    private void filterContacts(String searchText) {
        // TODO: Filter contacts
    }

    private void handleEditContact() {
        // TODO: Handle edit contact
    }

    private void handleViewContactInfo() {
        // TODO: Handle view contact info - could show contact details
        showError("Not Implemented", "View contact info feature coming soon!");
    }

    private void handleAttachFile() {
        // TODO: Handle attach file
        showError("Not Implemented", "Attach file feature coming soon!");
    }

    private void handleEmoji() {
        // TODO: Handle emoji picker
        showError("Not Implemented", "Emoji picker coming soon!");
    }

    private void handleRemoveAttachment() {
        // TODO: Handle remove attachment
    }

    private void handleSendMessage() {
        // TODO: Handle send message
        String message = messageInput.getText().trim();
        if (!message.isEmpty()) {
            System.out.println("Sending message: " + message);
            messageInput.clear();
        }
    }

    // ========== Public Methods ==========

    public void onMessageReceived(String sender, String message, long timestamp) {
        // TODO: Handle received message
    }

    public void updateConnectionStatus(boolean connected) {
        if (connectionStatusCircle != null && connectionStatusLabel != null) {
            if (connected) {
                connectionStatusCircle.getStyleClass().add("connected");
                connectionStatusLabel.setText("Connected");
            } else {
                connectionStatusCircle.getStyleClass().remove("connected");
                connectionStatusLabel.setText("Disconnected");
            }
        }
    }

    public void updateMessageStatus(String messageId, String status) {
        // TODO: Update message status
    }
}