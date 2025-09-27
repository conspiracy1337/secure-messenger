// SecureMessenger.java
package cns.client.main;

import cns.client.crypto.KeyManager;
import java.security.NoSuchAlgorithmException;

public class SecureMessenger {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        KeyManager keyManager = new KeyManager();
        String[] keys = keyManager.generateKeyPair();
        String privateKey = keys[0];
        String publicKey = keys[1];
        String publicKeyHash = keys[2];
        String masterPassword = "password123";

        System.out.println ("-----BEGIN PRIVATE KEY-----");
        System.out.println(privateKey);
        System.out.println ("-----END PRIVATE KEY-----");
        System.out.println ("-----BEGIN PUBLIC KEY-----");
        System.out.println(publicKey);
        System.out.println ("-----END PUBLIC KEY-----");
        System.out.println(publicKeyHash.toUpperCase());

        keyManager.setMasterPassword(masterPassword);
        keyManager.encryptAndStoreKeyPair(masterPassword, privateKey, publicKey);
        keyManager.decryptKeyPair("password123");
        System.out.println("Decrypted: " + keyManager.getPublicKey());
    }
}
