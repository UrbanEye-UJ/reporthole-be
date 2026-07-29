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
}
