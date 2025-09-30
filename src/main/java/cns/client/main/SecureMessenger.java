// SecureMessenger.java
package cns.client.main;

import cns.client.crypto.KeyManager;
import cns.client.data.StorageHandler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class SecureMessenger extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        System.out.println("Initializing application...");

        KeyManager keyManager = new KeyManager();
        StorageHandler storage = new StorageHandler();

        storage.createDb();

        String[] keys = keyManager.generateKeyPair();
        String privateKey = keys[0];
        String publicKey = keys[1];
        String publicKeyHash = keys[2];
        String masterPassword = "password123";

        System.out.println("-----BEGIN PRIVATE KEY-----");
        System.out.println(privateKey);
        System.out.println("-----END PRIVATE KEY-----");
        System.out.println("-----BEGIN PUBLIC KEY-----");
        System.out.println(publicKey);
        System.out.println("-----END PUBLIC KEY-----");

        keyManager.setMasterPassword(masterPassword);
        keyManager.saveKeyPair(masterPassword, privateKey, publicKey, publicKeyHash);

        if (keyManager.unlockApp(masterPassword)) {
            keyManager.sharePublicKey();
            System.out.println("App is ready for use!");
        } else {
            System.out.println("Failed to unlock app. Exiting.");
            System.exit(1);
        }

        System.out.println("Sender Hash: " + keyManager.getSenderHash());
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/cns/client/ui/AppWindow.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 700);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/cns/client/ui/styles.css")).toExternalForm());

        primaryStage.setTitle("Secure Messenger");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        System.out.println("Application closing...");
    }
}