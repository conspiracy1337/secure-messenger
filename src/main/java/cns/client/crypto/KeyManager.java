// KeyManager.java
package cns.client.crypto;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public class KeyManager {

    private String privateKey;
    private String publicKey;
    private final EncryptionManager encryptionManager;

    public KeyManager() {
        this.encryptionManager = new EncryptionManager();
    }

    public String[] generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(4096);
            KeyPair keypair = kpg.generateKeyPair();
            String privateKey = Base64.getEncoder().encodeToString(keypair.getPrivate().getEncoded());
            String publicKey = Base64.getEncoder().encodeToString(keypair.getPublic().getEncoded());

            MessageDigest publicHashDigest = MessageDigest.getInstance("SHA-256");
            byte[] publicHash = publicHashDigest.digest(publicKey.getBytes(StandardCharsets.UTF_8));
            String publicKeyHash = Base64.getEncoder().encodeToString(publicHash).substring(0, 12);

            return new String[]{privateKey, publicKey, publicKeyHash};

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Crypto algorithm not available", e);
        }
    }

    public void setMasterPassword(String masterPassword) {
        encryptionManager.setMasterPassword(masterPassword);
    }

    public void saveKeyPair(String masterPassword, String privateKey, String publicKey, String publicKeyHash) {
        encryptionManager.encryptAndStoreKeyPair(masterPassword, privateKey, publicKey, publicKeyHash);
    }

    public boolean unlockApp(String masterPassword) {
        if (encryptionManager.verifyMasterPassword(masterPassword)) {
            String[] decryptedKeys = encryptionManager.decryptKeyPair(masterPassword);

            if (decryptedKeys != null) {
                this.privateKey = decryptedKeys[0];
                this.publicKey = decryptedKeys[1];
                System.out.println("App Unlocked and Keys decrypted!");
                return true;
            } else {
                System.out.println("Failed to decrypt keys");
                return false;
            }
        } else {
            System.out.println("Incorrect password");
            return false;
        }
    }

    public void sharePublicKey() {
        try {
            if (publicKey == null) {
                throw new IllegalStateException("App not unlocked");
            }

            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            MessageDigest publicHashDigest = MessageDigest.getInstance("SHA-256");
            byte[] publicHash = publicHashDigest.digest(publicKey.getBytes(StandardCharsets.UTF_8));
            String publicKeyHash = Base64.getEncoder().encodeToString(publicHash).substring(0, 8);
            StringSelection publicKeyExport = new StringSelection(publicKey + "SM:" + publicKeyHash);
            clipboard.setContents(publicKeyExport, null);
            System.out.println("Copied Public Key to Clipboard!");

        } catch (Exception e) {
            System.out.println("Could not copy Public Key");
        }
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public boolean isUnlocked() {
        return privateKey != null && publicKey != null;
    }

    public String getSenderHash() {
        try {
            if (publicKey == null) {
                throw new IllegalStateException("App not unlocked");
            }
            MessageDigest publicHashDigest = MessageDigest.getInstance("SHA-256");
            byte[] publicHash = publicHashDigest.digest(publicKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(publicHash).substring(0, 12);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}