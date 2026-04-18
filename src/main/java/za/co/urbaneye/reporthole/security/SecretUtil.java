package za.co.urbaneye.reporthole.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Utility class for generating secure hashes of sensitive values.
 *
 * <p>This class currently provides functionality for hashing
 * email addresses using the SHA-256 algorithm.</p>
 *
 * <p>Email values are normalized before hashing by:</p>
 * <ul>
 *     <li>Converting to lowercase</li>
 *     <li>Removing leading and trailing spaces</li>
 * </ul>
 *
 * <p>The resulting hash is encoded using Base64 for easier
 * storage and comparison.</p>
 *
 * <p>Typical use cases include:</p>
 * <ul>
 *     <li>Searching encrypted email records</li>
 *     <li>Privacy-preserving lookups</li>
 *     <li>Duplicate detection without storing plaintext emails</li>
 * </ul>
 *
 * @author Refentse
 * @since 1.0
 */
public class SecretUtil {

    /**
     * Hashes an email address using SHA-256.
     *
     * <p>The email is normalized to lowercase and trimmed before hashing
     * to ensure consistent results for equivalent values.</p>
     *
     * @param email the email address to hash
     * @return Base64 encoded SHA-256 hash of the email
     * @throws RuntimeException if the hashing algorithm is unavailable
     */
    public static String hashEmail(String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    email.toLowerCase()
                            .trim()
                            .getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing email", e);
        }
    }
}