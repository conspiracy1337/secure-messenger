package cns.client.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * WindowTester - Opens all FXML windows at once for testing and preview
 * This class helps visualize all UI screens simultaneously
 */
public class WindowTester extends Application {

    private static final String CSS_FILE = "/cns/client/ui/styles.css";

    // Window definitions: [FXML file, Title, Width, Height]
    private static final String[][] WINDOWS = {
            {"InitialSetup.fxml", "Initial Setup", "500", "500"},
            {"Login.fxml", "Login", "450", "400"},
            {"ShareKey.fxml", "Share Public Key", "500", "550"},
            {"AddContact.fxml", "Add Contact", "500", "450"},
            {"About.fxml", "About", "450", "400"},
            {"DeleteData.fxml", "Delete Data", "500", "450"},
            {"ChangePassword.fxml", "Change Password", "500", "500"},
            {"GenerateNewKeys.fxml", "Generate New Keys", "500", "500"}
    };

    @Override
    public void start(Stage primaryStage) {
        List<Stage> stages = new ArrayList<>();

        // Get screen bounds for positioning
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double screenWidth = screenBounds.getWidth();
        double screenHeight = screenBounds.getHeight();

        // Calculate grid layout (3 columns)
        int columns = 3;
        int rows = (int) Math.ceil(WINDOWS.length / (double) columns);

        double windowSpacing = 20;
        double startX = 50;
        double startY = 50;

        // Create and position all windows
        for (int i = 0; i < WINDOWS.length; i++) {
            String[] windowData = WINDOWS[i];
            String fxmlFile = windowData[0];
            String title = windowData[1];
            double width = Double.parseDouble(windowData[2]);
            double height = Double.parseDouble(windowData[3]);

            try {
                // Load FXML
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
                Scene scene = new Scene(loader.load(), width, height);

                // Apply CSS
                String cssPath = getClass().getResource(CSS_FILE).toExternalForm();
                scene.getStylesheets().add(cssPath);

                // Create stage
                Stage stage = new Stage();
                stage.setScene(scene);
                stage.setTitle(title);

                // Calculate position in grid
                int row = i / columns;
                int col = i % columns;

                double xPos = startX + (col * (width + windowSpacing));
                double yPos = startY + (row * (height + windowSpacing));

                // Adjust if window goes off screen
                if (xPos + width > screenWidth) {
                    xPos = screenWidth - width - 20;
                }
                if (yPos + height > screenHeight) {
                    yPos = screenHeight - height - 20;
                }

                stage.setX(xPos);
                stage.setY(yPos);

                stages.add(stage);

                System.out.println("✓ Loaded: " + title + " at (" + (int)xPos + ", " + (int)yPos + ")");

            } catch (Exception e) {
                System.err.println("✗ Failed to load " + fxmlFile + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Show all windows
        System.out.println("\n=== Opening " + stages.size() + " windows ===\n");
        for (Stage stage : stages) {
            stage.show();
        }

        // Use the first stage as primary (or create a control window)
        if (!stages.isEmpty()) {
            primaryStage = stages.get(0);
            primaryStage.setOnCloseRequest(event -> {
                System.out.println("\n=== Closing all windows ===");
                stages.forEach(Stage::close);
            });
        }

        System.out.println("\n=== All windows opened successfully ===");
        System.out.println("Close any window to exit all windows");
    }

    public static void main(String[] args) {
        launch(args);
    }
}