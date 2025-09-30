package cns.client.ui;

import cns.client.data.StorageHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class AddContactController {
    StorageHandler storageHandler = new StorageHandler();
    @FXML private TextField contactNameField;
    @FXML private TextArea publicKeyArea;

    public void addContact() {
        String tableName = "contacts";
        String name = contactNameField.getText();
        String publicKey = publicKeyArea.getText().substring(0, publicKeyArea.getText().length()-11);
        String publicKeyHash = publicKeyArea.getText().substring(publicKeyArea.getText().length()-8);
        List<String> columnArray = List.of(
                "name",
                "publicKey",
                "publicKeyHash"
        );

        List<String> valueArray = List.of(
                name,
                publicKey,
                publicKeyHash
        );
        System.out.println(valueArray);
        //storageHandler.insertIntoDb(tableName, columnArray, valueArray);
    }

    public void handleClose() {
        Stage stage = (Stage) contactNameField.getScene().getWindow();
        stage.close();
    }
}
