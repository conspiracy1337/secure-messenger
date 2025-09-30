package cns.shared.objects;

public class Contact {
    String name;
    String publicKey;
    String publicKeyHash;
    String picture;
    String contactId;
    int messageCount;
    String status;
    long dateAdded;
    long lastMessage;
    boolean isBlocked;
    boolean isOnline;
    long lastOnline;


    public Contact(String name, String publicKey, String publicKeyHash) {
        this.name = name;
        this.publicKey = publicKey;
        this.publicKeyHash = publicKeyHash;
    }
}
