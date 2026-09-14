package za.co.urbaneye.reporthole.admin.security.dto;

/**
 * Response body for {@code POST /admin/security/users/{id}/reveal} — the target account's
 * decrypted name and email, returned only after the caller's step-up password check passes.
 *
 * @param name  the account's full decrypted name
 * @param email the account's decrypted email address
 * @author Refentse
 * @since 1.0
 */
public record RevealAccountResponse(String name, String email) {}
