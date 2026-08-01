package za.co.urbaneye.reporthole.notification.service.interfaces;

/**
 * Contract for sending transactional emails to users.
 */
public interface IMailService {

    /**
     * Sends an HTML email verification link to a newly registered user.
     *
     * @param to        recipient email address
     * @param firstname recipient's first name
     * @param verifyUrl full verification URL including the token
     */
    void sendVerificationEmail(String to, String firstname, String verifyUrl);

    /**
     * Sends an HTML password reset link.
     * Clicking the link also unlocks a locked account.
     *
     * @param to        recipient email address
     * @param firstname recipient's first name
     * @param resetUrl  full reset URL including the token
     */
    void sendPasswordResetEmail(String to, String firstname, String resetUrl);

    /**
     * Sends an admin access application notification to the project's own noreply address
     * ({@code mail.from}). The Reply-To header is set to the applicant's actual email so
     * the reviewer can reply directly to them.
     *
     * @param userName          applicant's first name
     * @param emailHash         SHA-256 hash stored in the {@code AUTH_EMAIL_HASH} column
     * @param municipalityToken token submitted by the applicant
     * @param replyTo           applicant's decrypted email address (used as Reply-To header)
     */
    void sendAdminApplicationEmail(String userName, String userId, String emailHash, String municipalityToken, String replyTo);
}
