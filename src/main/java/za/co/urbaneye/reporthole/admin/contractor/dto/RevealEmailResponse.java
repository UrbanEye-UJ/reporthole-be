package za.co.urbaneye.reporthole.admin.contractor.dto;

/**
 * Response body for {@code POST /admin/contractors/{id}/reveal-email} —
 * the contractor's decrypted email and phone number, returned only after the calling
 * admin's own password has been verified.
 *
 * @param email       the contractor's decrypted email address
 * @param phoneNumber the contractor's decrypted phone number
 * @author Refentse
 * @since 1.0
 */
public record RevealEmailResponse(String email, String phoneNumber) {}
