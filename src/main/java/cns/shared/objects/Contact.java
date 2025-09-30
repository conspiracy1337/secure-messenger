package cns.shared.objects;

public class Contact {
    String name;
    String publicKey;
    String publicKeyHash;
    String notes;
    int messageCount;
    long dateAdded;
    long lastMessage;
    boolean isBlocked;
    boolean isOnline;

    public Contact(String name, String publicKey, String publicKeyHash) {
        this.name = name;
        this.publicKey = publicKey;
        this.publicKeyHash = publicKeyHash;
    }
}
