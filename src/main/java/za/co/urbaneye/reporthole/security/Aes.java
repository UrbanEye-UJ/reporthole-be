package za.co.urbaneye.reporthole.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;


/**
 * JPA attribute converter that transparently encrypts and decrypts
 * {@link String} entity fields using AES encryption.
 *
 * <p>This converter encrypts plaintext values before persisting them
 * to the database and decrypts encrypted values when reading them back
 * into the entity.</p>
 *
 * <p>Encryption details:</p>
 * <ul>
 *     <li>Algorithm: AES/CBC/PKCS5Padding</li>
 *     <li>IV: Randomly generated per encryption operation</li>
 *     <li>Key source: Externalised application property {@code web.aes}</li>
 *     <li>Storage format: Base64 encoded value containing IV + ciphertext</li>
 * </ul>
 *
 * <p>Typical usage:</p>
 *
 * <pre>
 * {@code
 * @Convert(converter = Aes.class)
 * private String email;
 * }
 * </pre>
 *
 * <p>This allows sensitive fields such as emails, phone numbers,
 * ID numbers, or addresses to be stored encrypted at rest.</p>
 *
 * <p><strong>Important:</strong> The configured key should be stored securely
 * using environment variables, a secrets manager, or secure configuration,
 * and should never be hardcoded in source control.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Converter
@Component
public class Aes implements AttributeConverter<String, String> {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    @Value("${web.aes.key}")
    private String secretKey;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            byte[] iv = generateIv();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(), new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting field", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(dbData);

            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, buildKey(), new IvParameterSpec(iv));

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting field", e);
        }
    }

    private SecretKeySpec buildKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        byte[] paddedKey = new byte[32];
        System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
        return new SecretKeySpec(paddedKey, "AES");
    }

    private byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

}
