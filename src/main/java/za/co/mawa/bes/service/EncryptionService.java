package za.co.mawa.bes.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

@Service
public class EncryptionService {
    private static final String ALGORITHM = "AES";

    public String encrypt(String value, String secret) {
        if (value == null) {
            throw new IllegalArgumentException("Value to encrypt cannot be null");
        }

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, createSecretKey(secret));
            return Base64.getEncoder().encodeToString(
                    cipher.doFinal(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt value", exception);
        }
    }

    public String decrypt(String encryptedValue, String secret) {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            throw new IllegalArgumentException("Value to decrypt cannot be null or blank");
        }

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, createSecretKey(secret));
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedValue));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to decrypt value", exception);
        }
    }

    private SecretKeySpec createSecretKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Encryption secret cannot be null or blank");
        }

        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] key = sha.digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(Arrays.copyOf(key, 16), ALGORITHM);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Required SHA-1 algorithm is unavailable", exception);
        }
    }
}
