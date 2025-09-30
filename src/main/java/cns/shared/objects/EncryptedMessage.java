// EncryptedMessage.java
package cns.shared.objects;

public class EncryptedMessage {
    String sender;
    String recipient;
    String encryptedMessage;
    String encryptedAesKey;
    String iv;
    String messageId;
    boolean hasFile;
    String attachedFile;
    boolean isEdited;
    String status;
    long timestamp;

    public EncryptedMessage(String sender, String recipient, String encryptedMessage, String encryptedAesKey, String iv, String messageId, boolean hasFile, String attachedFile, boolean isEdited, String status, long timestamp) {
        this.sender = sender;
        this.recipient = recipient;
        this.encryptedMessage = encryptedMessage;
        this.encryptedAesKey = encryptedAesKey;
        this.iv = iv;
        this.messageId = messageId;
        this.hasFile = hasFile;
        this.attachedFile = attachedFile;
        this.isEdited = isEdited;
        this.status = status;
        this.timestamp = timestamp;
    }
    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getEncryptedMessage() {
        return encryptedMessage;
    }

    public String getEncryptedAesKey() {
        return encryptedAesKey;
    }

    public String getIv() {
        return iv;
    }

    public String getMessageId() {
        return messageId;
    }

    public boolean getHasFile() {
        return hasFile;
    }

    public String getAttachedFile() {
        return attachedFile;
    }

    public boolean getIsEdited() {
        return isEdited;
    }

    public String getStatus() {
        return status;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
