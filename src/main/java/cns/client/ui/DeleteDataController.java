package cns.client.ui;

import cns.client.data.StoredFiles;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Files;

public class DeleteDataController {
    @FXML private TextField confirmationField;
    @FXML private Button deleteButton;
    @FXML private Button cancelButton;
    StoredFiles storedFiles = new StoredFiles();

    private Timeline countdownTimeline;
    private int countdown = 5;
    private String originalButtonText;

    @FXML
    public void initialize() {
        originalButtonText = deleteButton.getText();
    }

    public void deleteAllData() {
        if (confirmationField.getText().equals("DELETE EVERYTHING")) {
            confirmationField.setDisable(true);
            deleteButton.setDisable(true);

            countdown = 5;
            deleteButton.setText("Deleting in " + countdown + "...");

            countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                countdown--;
                if (countdown > 0) {
                    deleteButton.setText("Deleting in " + countdown + "...");
                } else {
                    performDeletion();
                }
            }));
            countdownTimeline.setCycleCount(5);
            countdownTimeline.play();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Confirmation Failed");
            alert.setHeaderText(null);
            alert.setContentText("Please type 'DELETE EVERYTHING' to confirm deletion.");
            alert.showAndWait();
        }
    }

    private void performDeletion() {
        try {
            Files.deleteIfExists(storedFiles.getDbFile().toPath());
            Files.deleteIfExists(storedFiles.getMasterKeyFile().toPath());
            Files.deleteIfExists(storedFiles.getConfigFolder());

            System.out.println("Deleted all data successfully");

            Platform.exit();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Deletion Failed");
            alert.setHeaderText(null);
            alert.setContentText("Failed to delete files: " + e.getMessage());
            alert.showAndWait();

            confirmationField.setDisable(false);
            deleteButton.setDisable(false);
            deleteButton.setText(originalButtonText);
        }
    }

    public void handleClose() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            deleteButton.setText(originalButtonText);
            deleteButton.setDisable(false);
            confirmationField.setDisable(false);
            System.out.println("Deletion aborted");
        } else {
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.close();
        }
    }
}