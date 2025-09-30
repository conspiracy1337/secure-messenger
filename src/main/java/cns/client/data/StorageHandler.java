package cns.client.data;

import cns.shared.objects.DecryptedMessage;
import cns.shared.objects.EncryptedMessage;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class StorageHandler {
    StoredFiles configData = new StoredFiles();
    String sender;
    String recipient;
    String encryptedMessage;
    String messageId;
    boolean hasFile;
    String attachedFile;
    boolean isEdited;
    String status;
    long timestamp;


    public void storeMessage(DecryptedMessage decryptedBundle) {
        this.sender = decryptedBundle.getSender();
        this.recipient = decryptedBundle.getRecipient();
        this.encryptedMessage = decryptedBundle.getDecryptedMessage();
        this.messageId = decryptedBundle.getMessageId();
        this.hasFile = decryptedBundle.getHasFile();
        this.attachedFile = decryptedBundle.getAttachedFile();
        this.isEdited = decryptedBundle.getIsEdited();
        this.status = decryptedBundle.getStatus();
        this.timestamp = decryptedBundle.getTimestamp();

        String tableName = "messages";

        List<String> columnArray = List.of(
                "messageId",
                "sender",
                "recipient",
                "messageContent",
                "hasFile",
                "attachedFile",
                "timestamp",
                "status",
                "isEdited"
        );

        List<String> valueArray = List.of(
                this.messageId,
                this.sender,
                this.recipient,
                this.encryptedMessage,
                String.valueOf(this.hasFile),
                this.attachedFile,
                String.valueOf(this.timestamp),
                this.status,
                String.valueOf(this.isEdited)
        );

        insertIntoDb(tableName, columnArray, valueArray);
    }

    public void createDb() {
        String dbUrl = "jdbc:sqlite:" + configData.getDbFile().toString();
        try (var conn = DriverManager.getConnection(dbUrl)) {
            if (conn != null) {
                String contactsTable = "CREATE TABLE IF NOT EXISTS contacts ("
                        + "id integer PRIMARY KEY AUTOINCREMENT NOT NULL,"
                        + "name text NOT NULL,"
                        + "publicKey text,"
                        + "hashes text,"
                        + "notes text,"
                        + "messageCount integer DEFAULT 0,"
                        + "lastOnline text,"
                        + "lastMessage text,"
                        + "isBlocked boolean DEFAULT FALSE,"
                        + "createdAt DATETIME DEFAULT CURRENT_TIMESTAMP"
                        + ");";

                String messagesTable = "CREATE TABLE IF NOT EXISTS messages ("
                        + "id integer PRIMARY KEY AUTOINCREMENT,"
                        + "messageId text UNIQUE NOT NULL,"
                        + "sender text NOT NULL,"
                        + "recipient text NOT NULL,"
                        + "messageContent text,"
                        + "hasFile boolean DEFAULT FALSE,"
                        + "attachedFile text,"
                        + "timestamp integer,"
                        + "status text DEFAULT 'pending',"
                        + "isEdited boolean DEFAULT FALSE"
                        + ");";

                String hashHistoryTable = "CREATE TABLE IF NOT EXISTS hashhistory ("
                        + "hash text"
                        + ");";

                String[] indexes = {
                        "CREATE INDEX IF NOT EXISTS idx_sender ON messages(sender);",
                        "CREATE INDEX IF NOT EXISTS idx_recipient ON messages(recipient);",
                        "CREATE INDEX IF NOT EXISTS idx_timestamp ON messages(timestamp);",
                        "CREATE INDEX IF NOT EXISTS idx_file ON messages(hasFile);"
                };

                try (var statement = conn.createStatement()) {
                    statement.execute(contactsTable);
                    statement.execute(messagesTable);
                    statement.execute(hashHistoryTable);

                    for (String index : indexes) {
                        statement.execute(index);
                    }

                    System.out.println("Database initialized successfully");
                } catch (SQLException e) {
                    System.out.println("Error creating tables: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("Error connecting to database: " + e.getMessage());
        }
    }

    public void insertIntoDb(String tableName, List<String> columnArray, List<String> valueArray) {
        if (columnArray.size() != valueArray.size()) {
            throw new IllegalArgumentException("Column and value arrays must have same size");
        }

        String dbUrl = "jdbc:sqlite:" + configData.getDbFile().toString();

        String columns = String.join(", ", columnArray);

        String placeholders = "?, ".repeat(valueArray.size());
        placeholders = placeholders.substring(0, placeholders.length() - 2);

        String insertStatement = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";

        try (var conn = DriverManager.getConnection(dbUrl);
             PreparedStatement preparedStatement = conn.prepareStatement(insertStatement)) {

            for (int i = 0; i < valueArray.size(); i++) {
                preparedStatement.setString(i + 1, valueArray.get(i));
            }

            int rowsAffected = preparedStatement.executeUpdate();
            System.out.println("Inserted " + rowsAffected + " row(s) successfully");

        } catch (SQLException e) {
            System.err.println("Error inserting into " + tableName + ": " + e.getMessage());
        }
    }
}
