package com.omnipico.pluralkitmc.database;

import com.omnipico.pluralkitmc.PluralKitMC;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import static com.omnipico.pluralkitmc.PluralKitMC.getInstance;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

public class KeyManager {
    private static byte[] key;
    private static final PluralKitMC plugin = getInstance();

    public static void setKey() {
        File cacheDir = new File(plugin.getDataFolder(), "cache");
        if (!cacheDir.exists() && cacheDir.mkdirs()) {
            plugin.getLogger().info("Created cache directory");
        }

        Path keyPath = cacheDir.toPath().resolve("cache.key");
        try {
            if (!Files.exists(keyPath)) {
                key = SecureRandom.getInstanceStrong().generateSeed(32);
                Files.write(keyPath, key, CREATE_NEW);
            } else {
                key = Files.readAllBytes(keyPath);
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            plugin.getLogger().severe("Could not create key file, error: " + e.getMessage());
        }
    }

    public static byte[] getKey() {
        if (key == null) {
            KeyManager.setKey();
        }
        return key;
    }

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    public static String encrypt(byte[] key, String plaintext) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        SecureRandom.getInstanceStrong().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKey secretKey = new SecretKeySpec(key, "AES");
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

        // Prepend IV to ciphertext
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public static String decrypt(byte[] key, String encryptedBase64) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedBase64);

        byte[] iv = new byte[IV_LENGTH];
        byte[] ciphertext = new byte[combined.length - IV_LENGTH];

        System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
        System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKey secretKey = new SecretKeySpec(key, "AES");
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);

        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        byte[] plaintext = cipher.doFinal(ciphertext);

        return new String(plaintext);
    }
}

