package za.co.urbaneye.reporthole.user.service.interfaces;

/**
 * Handles the two-step password reset flow.
 *
 * <p>Step 1 — {@link #requestReset}: validates the email, issues a single-use
 * token, and emails the reset link to the user.</p>
 *
 * <p>Step 2 — {@link #resetPassword}: validates the token, sets the new
 * password, unlocks the account, and invalidates the token.</p>
 */
public interface IPasswordResetService {

    /**
     * Generates a password reset token for the account with the given email
     * and sends the reset link by email.
     *
     * @param email the email address of the account to reset
     */
    void requestReset(String email);

    /**
     * Applies a new password using a previously issued reset token.
     * Also sets account status to {@code ACTIVE} and clears the retry counter.
     *
     * @param token       the reset token from the emailed link
     * @param newPassword the plaintext password to hash and store
     */
    void resetPassword(String token, String newPassword);
}
