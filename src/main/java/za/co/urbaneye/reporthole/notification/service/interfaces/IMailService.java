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

    /**
     * Notifies a contractor that a new incident has been assigned to them.
     *
     * @param to             contractor's decrypted email address
     * @param firstname      contractor's first name
     * @param incidentType   display name of the assigned {@code IssueType}
     * @param locationAddress human-readable location of the incident
     * @param incidentId     UUID of the assigned incident, included for reference
     */
    void sendJobAssignedEmail(String to, String firstname, String incidentType, String locationAddress, String incidentId);

    /**
     * Notifies an applicant of the outcome of their admin access application.
     *
     * @param to        applicant's decrypted email address
     * @param firstname applicant's first name
     * @param approved  {@code true} if the application was approved, {@code false} if rejected
     */
    void sendAdminApplicationDecisionEmail(String to, String firstname, boolean approved);

    /**
     * Sends a contractor invite email containing a single-use registration link.
     *
     * @param to        contractor's email address
     * @param inviteUrl full registration URL including the invite token
     */
    void sendContractorInviteEmail(String to, String inviteUrl);

    /**
     * Sends a contractor-specific email verification link after registration.
     * Distinct from {@link #sendVerificationEmail} because the copy is tailored
     * to contractors (dashboard access, job assignments) not civilian reporting.
     *
     * @param to        contractor's email address
     * @param firstname contractor's first name
     * @param verifyUrl full verification URL including the token
     */
    void sendContractorVerificationEmail(String to, String firstname, String verifyUrl);

    /**
     * Sends a municipality admin-specific email verification link after registration.
     * Distinct from {@link #sendVerificationEmail} because the copy is tailored
     * to admins (municipality dashboard, incident management) not civilian reporting.
     *
     * @param to        admin's email address
     * @param firstname admin's first name
     * @param verifyUrl full verification URL including the token
     */
    void sendAdminVerificationEmail(String to, String firstname, String verifyUrl);

    /**
     * Sends a municipality admin registration token to a prospective admin.
     *
     * @param to               recipient email address
     * @param tokenValue       the MUNI-XXXXXXXXXXXX token string to enter at registration
     * @param municipalityName name of the municipality this token is scoped to
     * @param expiresLabel     human-readable expiry description, e.g. "in 30 days" or "never"
     * @param registrationUrl  full URL of the registration page
     */
    void sendMunicipalityTokenEmail(String to, String tokenValue, String municipalityName,
                                    String expiresLabel, String registrationUrl);
}
