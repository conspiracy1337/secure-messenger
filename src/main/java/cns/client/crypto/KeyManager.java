// KeyManager.java
package cns.client.crypto;

import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class KeyManager {

    private String privateKey;
    private String publicKey;

    private File getMasterKeyFile() {

        String configPath;
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            configPath = System.getenv("APPDATA");
        } else if (os.contains("mac")) {
            String home = System.getProperty("user.home");
            configPath = home + "/Library/Application Support";
        } else {
            String home = System.getProperty("user.home");
            configPath = home + "/.config";
        }

        File appDir = new File(configPath, "cns/secure-messenger");

        if (!appDir.exists() && !appDir.mkdirs()) {
            throw new RuntimeException("Failed to create directory: " + appDir.getAbsolutePath());
        }

        return new File(appDir, "master.key");
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
            String publicKeyHash = Base64.getEncoder().encodeToString(publicHash).substring(0, 8);

            return new String[]{privateKey, publicKey, publicKeyHash};

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Crypto algorithm not available", e);
        }
    }

    public void encryptAndStoreKeyPair(String masterPassword, String privateKeyPEM, String publicKeyPEM) {
    /*
        password + salt + 100000 iterations → PBKDF2 produces derived key  → AES uses derived key as AES key
        AES key + random IV + keypair → encrypted keypair
        Store: salt, iterations, IVs, encrypted keypair, (not derived/AES key)
     */

        File masterKeyFile = getMasterKeyFile();
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
                byte[] aesKey = derivedKey.getEncoded();
                SecretKeySpec secretKey = new SecretKeySpec(aesKey, "AES");

                // Encrypt private key
                Cipher privateKeyCipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec privateGcmSpec = new GCMParameterSpec(128, privateKeyIv);
                privateKeyCipher.init(Cipher.ENCRYPT_MODE, secretKey, privateGcmSpec);
                byte[] privateKeyCipherText = privateKeyCipher.doFinal(privateKeyPEM.getBytes(StandardCharsets.UTF_8));

                // Encrypt public key
                Cipher publicKeyCipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec publicGcmSpec = new GCMParameterSpec(128, publicKeyIv);
                publicKeyCipher.init(Cipher.ENCRYPT_MODE, secretKey, publicGcmSpec);
                byte[] publicKeyCipherText = publicKeyCipher.doFinal(publicKeyPEM.getBytes(StandardCharsets.UTF_8));

                String saltBase64 = Base64.getEncoder().encodeToString(pbkdfSalt);
                String privateIvBase64 = Base64.getEncoder().encodeToString(privateKeyIv);
                String publicIvBase64 = Base64.getEncoder().encodeToString(publicKeyIv);
                String encryptedPrivateKey = Base64.getEncoder().encodeToString(privateKeyCipherText);
                String encryptedPublicKey = Base64.getEncoder().encodeToString(publicKeyCipherText);

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

    public void setMasterPassword(String masterPassword) {
        /*
            enter master password → saved as hash
         */

        try {
            String passwordHash = BCrypt.hashpw(masterPassword, BCrypt.gensalt(12));
            File masterKeyFile = getMasterKeyFile();
            MasterKeyData keyData;

            if (masterKeyFile.exists()) {
                try (FileReader reader = new FileReader(masterKeyFile)) {
                    Gson gson = new Gson();
                    keyData = gson.fromJson(reader, MasterKeyData.class);
                    keyData.passwordHash = passwordHash;
                    keyData.iterations = 100000;
                }
            } else {
                keyData = new MasterKeyData(passwordHash, null, 100000, null, null, null, null);
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

    public boolean unlockApp(String masterPassword) {
        /*
            when unlocking app ask for password and compare to hash → unlock app and decrypt keys into memory
         */

        try {
            File masterKeyFile = getMasterKeyFile();
            MasterKeyData keyData;

            if (masterKeyFile.exists()) {
                try (FileReader reader = new FileReader(masterKeyFile)) {
                    Gson gson = new Gson();
                    keyData = gson.fromJson(reader, MasterKeyData.class);
                    return BCrypt.checkpw(masterPassword, keyData.passwordHash);
                }
            } else {
                return false;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read the master.key file", e);
        }
    }

    public void decryptKeyPair(String masterPassword) {
        /*
        password + stored salt + stored iterations → PBKDF2 reproduces derived key → AES uses derived key as AES key
        recreated AES key + stored IV + encrypted keypair → decrypts to original keypair
         */

        File masterKeyFile = getMasterKeyFile();
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

                this.privateKey = decryptedPrivateKey;
                this.publicKey = decryptedPublicKey;

            } catch (javax.crypto.AEADBadTagException e) {
                System.out.println("Incorrect master password. Decryption failed.");

            } catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException |
                     InvalidKeyException | InvalidAlgorithmParameterException |
                     IllegalBlockSizeException | BadPaddingException e) {
                throw new RuntimeException("Encryption error", e);
            }
        } catch (IOException e) {
            throw new RuntimeException("File not found", e);
        }
    }

    public String getPrivateKey() { return privateKey; }

    public String getPublicKey() { return publicKey; }

}
