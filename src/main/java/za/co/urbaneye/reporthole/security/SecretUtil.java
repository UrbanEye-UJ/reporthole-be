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

    /**
     * Masks an email address for display in admin lists, e.g. {@code "jo***@example.com"}.
     *
     * <p>Only the first character of the local part is shown; the rest of the local
     * part is replaced with {@code ***}. Used so decrypted PII is never rendered in a
     * list view — callers must go through an explicit "reveal" action (re-authentication
     * required) to see the full address.</p>
     *
     * @param email the decrypted email address to mask
     * @return the masked email, or {@code "***"} if the input has no {@code @}
     */
    public static String maskEmail(String email) {
        if (email == null) {
            return "***";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String localPart = email.substring(0, at);
        String domain = email.substring(at);
        String visible = localPart.length() <= 1 ? localPart : localPart.substring(0, 1);
        return visible + "***" + domain;
    }

    /**
     * Masks a person's name for display to other users, e.g. {@code "Jane D."}.
     *
     * <p>Keeps the first name and the last name's initial only — used anywhere a civilian's
     * identity is shown to someone other than themselves (admin directories, incident
     * comment threads) so the full name is never exposed outside an explicit "reveal" flow.</p>
     *
     * @param firstName decrypted first name
     * @param lastName  decrypted last name
     * @return masked display name, or {@code "—"} if both inputs are blank
     */
    public static String maskName(String firstName, String lastName) {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        if (first.isEmpty() && last.isEmpty()) {
            return "—";
        }
        if (last.isEmpty()) {
            return first;
        }
        return first + " " + last.charAt(0) + ".";
    }

    /**
     * Masks a phone number for display, e.g. {@code "0821***890"}.
     *
     * <p>Keeps the first three and last three digits, replacing the middle with {@code ***} —
     * used anywhere a phone number is shown to someone other than its owner, so it's never
     * exposed outside an explicit "reveal" flow.</p>
     *
     * @param phone the phone number to mask
     * @return the masked phone number, or {@code "—"} for null/blank input
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "—";
        }
        String trimmed = phone.trim();
        if (trimmed.length() <= 6) {
            return "*".repeat(trimmed.length());
        }
        String start = trimmed.substring(0, 3);
        String end = trimmed.substring(trimmed.length() - 3);
        return start + "***" + end;
    }
}