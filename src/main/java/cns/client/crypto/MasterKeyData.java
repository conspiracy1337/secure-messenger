package cns.client.crypto;

public class MasterKeyData {
    String passwordHash;
    String salt;
    int iterations;
    String privateKeyIv;
    String publicKeyIv;
    String encryptedPrivateKey;
    String encryptedPublicKey;

    public MasterKeyData(String passwordHash, String salt, int iterations, String privateKeyIv, String publicKeyIv, String encryptedPrivateKey, String encryptedPublicKey) {
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.iterations = iterations;
        this.privateKeyIv = privateKeyIv;
        this.publicKeyIv = publicKeyIv;
        this.encryptedPrivateKey = encryptedPrivateKey;
        this.encryptedPublicKey = encryptedPublicKey;
    }
}
