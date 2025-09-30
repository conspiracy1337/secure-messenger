// DecryptedMessage.java
package cns.shared.objects;

public class DecryptedMessage {
    String sender;
    String recipient;
    String decryptedMessage;
    String messageId;
    boolean hasFile;
    String attachedFile;
    boolean isEdited;
    String status;
    long timestamp;

    public DecryptedMessage(String sender, String recipient, String decryptedMessage, String messageId, boolean hasFile, String attachedFile, boolean isEdited, String status, long timestamp) {
        this.sender = sender;
        this.recipient = recipient;
        this.decryptedMessage = decryptedMessage;
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

    public String getDecryptedMessage() {
        return decryptedMessage;
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
