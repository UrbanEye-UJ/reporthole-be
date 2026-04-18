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
 * <p>This converter automatically encrypts plaintext values before they are
 * stored in the database and decrypts them when records are read back
 * into the application.</p>
 *
 * <p>Encryption configuration:</p>
 * <ul>
 *     <li>Algorithm: AES/CBC/PKCS5Padding</li>
 *     <li>IV Length: 16 bytes</li>
 *     <li>Key source: application property {@code web.aes.key}</li>
 *     <li>Storage format: Base64 encoded (IV + encrypted data)</li>
 * </ul>
 *
 * <p>Recommended for protecting sensitive values such as:</p>
 * <ul>
 *     <li>Email addresses</li>
 *     <li>Phone numbers</li>
 *     <li>ID numbers</li>
 *     <li>Residential addresses</li>
 * </ul>
 *
 * <p>Example usage:</p>
 *
 * <pre>
 * {@code
 * @Convert(converter = Aes.class)
 * private String email;
 * }
 * </pre>
 *
 * <p><strong>Security Note:</strong> The encryption key should be managed
 * securely through environment variables, vaults, or secrets managers
 * rather than being hardcoded.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Converter
@Component
public class Aes implements AttributeConverter<String, String> {

    /**
     * AES transformation algorithm used for encryption and decryption.
     */
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    /**
     * Length of the initialization vector in bytes.
     */
    private static final int IV_LENGTH = 16;

    /**
     * Secret key loaded from application configuration.
     */
    @Value("${web.aes.key}")
    private String secretKey;

    /**
     * Encrypts a plaintext entity attribute before database persistence.
     *
     * <p>A random IV is generated for each encryption operation.
     * The final stored value contains the IV concatenated with
     * the encrypted bytes and encoded in Base64.</p>
     *
     * @param attribute plaintext value from the entity
     * @return encrypted Base64 encoded string, or {@code null} if input is null
     * @throws RuntimeException if encryption fails
     */
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

    /**
     * Decrypts a database column value back into plaintext.
     *
     * <p>The stored Base64 value is decoded, the IV extracted,
     * and the remaining bytes decrypted using the configured key.</p>
     *
     * @param dbData encrypted Base64 database value
     * @return decrypted plaintext string, or {@code null} if input is null
     * @throws RuntimeException if decryption fails
     */
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

    /**
     * Builds a 256-bit AES key from the configured secret.
     *
     * <p>If the provided key is shorter than 32 bytes,
     * it is padded with zeros. If longer, it is truncated.</p>
     *
     * @return AES secret key specification
     */
    private SecretKeySpec buildKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        byte[] paddedKey = new byte[32];

        System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));

        return new SecretKeySpec(paddedKey, "AES");
    }

    /**
     * Generates a secure random initialization vector (IV).
     *
     * @return random IV byte array
     */
    private byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}