// EncryptionManager.java
package cns.client.crypto;

import cns.client.data.StorageHandler;
import cns.client.data.StoredFiles;
import cns.shared.objects.DecryptedMessage;
import cns.shared.objects.EncryptedMessage;
import org.mindrot.jbcrypt.BCrypt;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

public class EncryptionManager {
    StoredFiles configData = new StoredFiles();
    String ownPrivateKey;

    public void setMasterPassword(String masterPassword) {
        try {
            String passwordHash = BCrypt.hashpw(masterPassword, BCrypt.gensalt(12));
            File masterKeyFile = configData.getMasterKeyFile();
            MasterKeyData keyData;

            if (masterKeyFile.exists()) {
                try (FileReader reader = new FileReader(masterKeyFile)) {
                    Gson gson = new Gson();
                    keyData = gson.fromJson(reader, MasterKeyData.class);
                    keyData.passwordHash = passwordHash;
                    keyData.iterations = 100000;
                }
            } else {
                keyData = new MasterKeyData(passwordHash, null, null, 100000, null, null, null, null);
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonData = gson.toJson(keyData);

            try (FileWriter writer = new FileWriter(masterKeyFile)) {
                writer.write(jsonData);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save password hash", e);
        }
    }

    public boolean verifyMasterPassword(String masterPassword) {
        try {
            File masterKeyFile = configData.getMasterKeyFile();

            if (masterKeyFile.exists()) {
                try (FileReader reader = new FileReader(masterKeyFile)) {
                    Gson gson = new Gson();
                    MasterKeyData keyData = gson.fromJson(reader, MasterKeyData.class);
                    return BCrypt.checkpw(masterPassword, keyData.passwordHash);
                }
            } else {
                return false;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read the master.key file", e);
        }
    }

    public void encryptAndStoreKeyPair(String masterPassword, String privateKeyPEM, String publicKeyPEM, String publicKeyHash) {
        /*
            password + salt + 100000 iterations → PBKDF2 produces derived key  → AES uses derived key as AES key
            AES key + random IV + keypair → encrypted keypair
            Stores: salt, iterations, IVs, encrypted keypair, (not derived/AES key)
         */
        File masterKeyFile = configData.getMasterKeyFile();
        MasterKeyData keyData;

        try (FileReader reader = new FileReader(masterKeyFile)) {
            Gson gson = new Gson();
            keyData = gson.fromJson(reader, MasterKeyData.class);

            SecureRandom secureRandom = new SecureRandom();
            byte[] pbkdfSalt = new byte[32];
            byte[] privateKeyIv = new byte[12];
            byte[] publicKeyIv = new byte[12];
            secureRandom.nextBytes(pbkdfSalt);
            secureRandom.nextBytes(privateKeyIv);
            secureRandom.nextBytes(publicKeyIv);

            try {
                // PBKDF2 key derivation
                PBEKeySpec keySpec = new PBEKeySpec(masterPassword.toCharArray(), pbkdfSalt, keyData.iterations, 32 * 8);
                SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                SecretKey derivedKey = secretKeyFactory.generateSecret(keySpec);
                byte[] secretKey = derivedKey.getEncoded();
                SecretKeySpec aesKey = new SecretKeySpec(secretKey, "AES");

                // Encrypt private key
                Cipher privateKeyCipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec privateGcmSpec = new GCMParameterSpec(128, privateKeyIv);
                privateKeyCipher.init(Cipher.ENCRYPT_MODE, aesKey, privateGcmSpec);
                byte[] privateKeyCipherText = privateKeyCipher.doFinal(privateKeyPEM.getBytes(StandardCharsets.UTF_8));

                // Encrypt public key
                Cipher publicKeyCipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec publicGcmSpec = new GCMParameterSpec(128, publicKeyIv);
                publicKeyCipher.init(Cipher.ENCRYPT_MODE, aesKey, publicGcmSpec);
                byte[] publicKeyCipherText = publicKeyCipher.doFinal(publicKeyPEM.getBytes(StandardCharsets.UTF_8));

                String saltBase64 = Base64.getEncoder().encodeToString(pbkdfSalt);
                String privateIvBase64 = Base64.getEncoder().encodeToString(privateKeyIv);
                String publicIvBase64 = Base64.getEncoder().encodeToString(publicKeyIv);
                String encryptedPrivateKey = Base64.getEncoder().encodeToString(privateKeyCipherText);
                String encryptedPublicKey = Base64.getEncoder().encodeToString(publicKeyCipherText);

                keyData.publicKeyHash = publicKeyHash;
                keyData.salt = saltBase64;
                keyData.privateKeyIv = privateIvBase64;
                keyData.publicKeyIv = publicIvBase64;
                keyData.encryptedPrivateKey = encryptedPrivateKey;
                keyData.encryptedPublicKey = encryptedPublicKey;

                Gson gsonWriter = new GsonBuilder().setPrettyPrinting().create();
                String jsonData = gsonWriter.toJson(keyData);

                try (FileWriter writer = new FileWriter(masterKeyFile)) {
                    writer.write(jsonData);
                }

            } catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException |
                     InvalidKeyException | InvalidAlgorithmParameterException |
                     IllegalBlockSizeException | BadPaddingException e) {
                throw new RuntimeException("Encryption error", e);
            }

        } catch (IOException e) {
            throw new RuntimeException("File operation error", e);
        }
    }

    public String[] decryptKeyPair(String masterPassword) {
        /*
        password + stored salt + stored iterations → PBKDF2 reproduces derived key → AES uses derived key as AES key
        recreated AES key + stored IV + encrypted keypair → decrypts to original keypair
         */
        File masterKeyFile = configData.getMasterKeyFile();
        MasterKeyData keyData;

        try (FileReader reader = new FileReader(masterKeyFile)) {
            Gson gson = new Gson();
            keyData = gson.fromJson(reader, MasterKeyData.class);
            byte[] salt = Base64.getDecoder().decode(keyData.salt);
            byte[] privateKeyIv = Base64.getDecoder().decode(keyData.privateKeyIv);
            byte[] publicKeyIv = Base64.getDecoder().decode(keyData.publicKeyIv);
            byte[] encryptedPrivateKey = Base64.getDecoder().decode(keyData.encryptedPrivateKey);
            byte[] encryptedPublicKey = Base64.getDecoder().decode(keyData.encryptedPublicKey);

            try {
                PBEKeySpec keySpec = new PBEKeySpec(masterPassword.toCharArray(), salt, keyData.iterations, 32 * 8);
                SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                SecretKey derivedKey = secretKeyFactory.generateSecret(keySpec);
                byte[] aesKey = derivedKey.getEncoded();
                SecretKeySpec secretKey = new SecretKeySpec(aesKey, "AES");

                Cipher privateKeyCipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec privateGcmSpec = new GCMParameterSpec(128, privateKeyIv);
                privateKeyCipher.init(Cipher.DECRYPT_MODE, secretKey, privateGcmSpec);
                byte[] privateKeyDecipheredText = privateKeyCipher.doFinal(encryptedPrivateKey);
                String decryptedPrivateKey = new String(privateKeyDecipheredText, StandardCharsets.UTF_8);

                Cipher publicKeyCipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec publicGcmSpec = new GCMParameterSpec(128, publicKeyIv);
                publicKeyCipher.init(Cipher.DECRYPT_MODE, secretKey, publicGcmSpec);
                byte[] publicKeyDecipheredText = publicKeyCipher.doFinal(encryptedPublicKey);
                String decryptedPublicKey = new String(publicKeyDecipheredText, StandardCharsets.UTF_8);
                ownPrivateKey = decryptedPrivateKey;
                return new String[]{decryptedPrivateKey, decryptedPublicKey};

            } catch (AEADBadTagException e) {
                System.out.println("Incorrect master password. Decryption failed.");
                return null;

            } catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException |
                     InvalidKeyException | InvalidAlgorithmParameterException |
                     IllegalBlockSizeException | BadPaddingException e) {
                throw new RuntimeException("Encryption error", e);
            }
        } catch (IOException e) {
            throw new RuntimeException("File not found", e);
        }
    }

    public EncryptedMessage encryptMessage(String message, KeyManager keyManager) {

        String recipientPublicKey = "recipient.publicKey";
        EncodedKeySpec recipientPublicKeySpec;

        String senderHash = keyManager.getSenderHash();
        String recipientHash = "recipient.recipentHash";
        String encryptedMessage = "";
        String encryptedAesKey = "";
        String iv = "";
        String id = "";
        boolean hasFile = false;
        String attachedFile = "";
        boolean isEdited = false;
        String status = "";
        long timestamp = Instant.now().getEpochSecond();

        try {

            byte[] decodedKey = Base64.getDecoder().decode(recipientPublicKey);
            recipientPublicKeySpec = new X509EncodedKeySpec(decodedKey);

            SecureRandom secureRandom = new SecureRandom();
            byte[] messageIv = new byte[12];
            byte[] messageId = new byte[12];
            secureRandom.nextBytes(messageIv);
            secureRandom.nextBytes(messageId);

            KeyGenerator aesKeyGenerator = KeyGenerator.getInstance("AES");
            aesKeyGenerator.init(256);
            SecretKey aesKey = aesKeyGenerator.generateKey();

            Cipher messageCipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec messageGcmSpec = new GCMParameterSpec(128, messageIv);
            messageCipher.init(Cipher.ENCRYPT_MODE, aesKey, messageGcmSpec);
            byte[] messageCipherText = messageCipher.doFinal(message.getBytes(StandardCharsets.UTF_8));


            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(recipientPublicKeySpec);
            Cipher aesCipher = Cipher.getInstance("RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING");
            aesCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            encryptedAesKey = Base64.getEncoder().encodeToString(aesCipher.doFinal(aesKey.getEncoded()));

            encryptedMessage = Base64.getEncoder().encodeToString(messageCipherText);
            iv = Base64.getEncoder().encodeToString(messageIv);
            id = Base64.getEncoder().encodeToString(messageId);
            EncryptedMessage encryptedBundle = new EncryptedMessage(senderHash, recipientHash, encryptedMessage, encryptedAesKey, iv, id, hasFile, attachedFile, isEdited, status, timestamp);
            StorageHandler messageStore = new StorageHandler();
            return encryptedBundle;


        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                 InvalidKeyException | InvalidAlgorithmParameterException |
                 IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException e) {
            throw new RuntimeException("Encryption error", e);
        }
    }

    public DecryptedMessage decryptMessage(EncryptedMessage encryptedBundle) {
        String sender = encryptedBundle.getSender();
        String recipient = encryptedBundle.getRecipient();
        String encryptedMessage = encryptedBundle.getEncryptedMessage();
        String encryptedAesKey = encryptedBundle.getEncryptedAesKey();
        String messageIv = encryptedBundle.getIv();
        String messageId = encryptedBundle.getMessageId();
        boolean hasFile = encryptedBundle.getHasFile();
        String attachedFile = encryptedBundle.getAttachedFile();
        boolean isEdited = encryptedBundle.getIsEdited();
        String status = encryptedBundle.getStatus();
        long timestamp = encryptedBundle.getTimestamp();
        EncodedKeySpec privateKeySpec;
        
        try {
            byte[] decodedPrivateKey = Base64.getDecoder().decode(ownPrivateKey);
            privateKeySpec = new PKCS8EncodedKeySpec(decodedPrivateKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
            Cipher aesCipher = Cipher.getInstance("RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING");
            aesCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] encryptedAesKeyBytes = Base64.getDecoder().decode(encryptedAesKey);
            byte[] decryptedAesKeyBytes = aesCipher.doFinal(encryptedAesKeyBytes);
            SecretKeySpec aesKey = new SecretKeySpec(decryptedAesKeyBytes, "AES");

            byte[] ivBytes = Base64.getDecoder().decode(messageIv);
            Cipher messageCipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec messageGcmSpec = new GCMParameterSpec(128, ivBytes);
            messageCipher.init(Cipher.DECRYPT_MODE, aesKey, messageGcmSpec);
            byte[] encryptedMessageBytes = Base64.getDecoder().decode(encryptedMessage);
            byte[] decryptedMessageBytes = messageCipher.doFinal(encryptedMessageBytes);
            String decryptedMessage = new String(decryptedMessageBytes, StandardCharsets.UTF_8);

            return new DecryptedMessage(sender, recipient, decryptedMessage, messageId, hasFile, attachedFile, isEdited, status, timestamp);
            

        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                 InvalidKeyException | InvalidAlgorithmParameterException |
                 IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException e) {
            throw new RuntimeException("Decryption error", e);
        }
    }
}