package za.co.urbaneye.reporthole.notification.service.impl;

import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.notification.service.interfaces.IMailService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;

/**
 * Sends HTML transactional emails asynchronously via the auto-configured
 * {@link JavaMailSender}.
 *
 * <p>A new {@link MimeMessageHelper} is created per invocation so concurrent
 * calls do not share mutable per-email state.</p>
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class MailServiceImpl implements IMailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String from;

    @Value("${mail.verification-subject}")
    private String verificationSubject;

    @Value("${mail.password-reset-subject}")
    private String passwordResetSubject;

    @Value("classpath:templates/verification-email.html")
    private Resource verificationTemplate;

    @Value("classpath:templates/password-reset-email.html")
    private Resource passwordResetTemplate;

    @Value("${mail.admin-application-subject}")
    private String adminApplicationSubject;

    @Value("classpath:templates/admin-application-email.html")
    private Resource adminApplicationTemplate;

    @Value("${mail.job-assigned-subject}")
    private String jobAssignedSubject;

    @Value("${mail.job-assigned-url}")
    private String jobAssignedUrl;

    @Value("classpath:templates/job-assigned-email.html")
    private Resource jobAssignedTemplate;

    @Value("${mail.admin-application-approved-subject}")
    private String adminApplicationApprovedSubject;

    @Value("${mail.admin-application-rejected-subject}")
    private String adminApplicationRejectedSubject;

    @Value("classpath:templates/admin-application-decision-email.html")
    private Resource adminApplicationDecisionTemplate;

    @Value("${mail.contractor-invite-subject}")
    private String contractorInviteSubject;

    @Value("classpath:templates/contractor-invite-email.html")
    private Resource contractorInviteTemplate;

    @Value("${mail.municipality-token-subject}")
    private String municipalityTokenSubject;

    @Value("classpath:templates/municipality-token-email.html")
    private Resource municipalityTokenTemplate;

    @Value("${mail.contractor-verification-subject}")
    private String contractorVerificationSubject;

    @Value("classpath:templates/contractor-verification-email.html")
    private Resource contractorVerificationTemplate;

    @Value("${mail.admin-verification-subject}")
    private String adminVerificationSubject;

    @Value("classpath:templates/admin-verification-email.html")
    private Resource adminVerificationTemplate;

    /**
     * Sends an HTML email verification link to a newly registered user.
     *
     * @param to        recipient email address
     * @param firstname recipient's first name
     * @param verifyUrl full verification URL including the token
     */
    @Override
    @Async("asyncExecutor")
    public void sendVerificationEmail(String to, String firstname, String verifyUrl) {
        try {
            log.info("Sending verification email to {}", to);

            String html = verificationTemplate.getContentAsString(StandardCharsets.UTF_8)
                    .replace("{{userName}}", firstname)
                    .replace("{{verificationUrl}}", verifyUrl)
                    .replace("{{expiryHours}}", "24")
                    .replace("{{currentYear}}", String.valueOf(Year.now().getValue()));

            send(to, verificationSubject, html);
            log.info("Verification email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Sends an HTML password reset link.
     *
     * @param to        recipient email address
     * @param firstname recipient's first name
     * @param resetUrl  full reset URL including the token
     */
    @Override
    @Async("asyncExecutor")
    public void sendPasswordResetEmail(String to, String firstname, String resetUrl) {
        try {
            log.info("Sending password reset email to {}", to);

            String html = passwordResetTemplate.getContentAsString(StandardCharsets.UTF_8)
                    .replace("{{userName}}", firstname)
                    .replace("{{resetUrl}}", resetUrl)
                    .replace("{{expiryHours}}", "1")
                    .replace("{{currentYear}}", String.valueOf(Year.now().getValue()));

            send(to, passwordResetSubject, html);
            log.info("Password reset email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Notifies the project inbox of a new admin access request.
     *
     * <p>Reply-To is set to the applicant's actual email so the reviewer
     * can respond directly.</p>
     *
     * @param userName          applicant's first name
     * @param emailHash         SHA-256 hash from AUTH_EMAIL_HASH
     * @param municipalityToken token submitted by the applicant
     * @param replyTo           applicant's decrypted email address
     */
    @Override
    @Async("asyncExecutor")
    public void sendAdminApplicationEmail(String userName, String userId, String emailHash, String municipalityToken, String replyTo) {
        try {
            log.info("Sending admin application email to inbox for applicant hash={}", emailHash);

            String submitted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            String html = adminApplicationTemplate.getContentAsString(StandardCharsets.UTF_8)
                    .replace("{{userName}}", userName)
                    .replace("{{userId}}", userId)
                    .replace("{{emailHash}}", emailHash)
                    .replace("{{municipalityToken}}", municipalityToken)
                    .replace("{{submittedAt}}", submitted)
                    .replace("{{currentYear}}", String.valueOf(Year.now().getValue()));

            // send to the noreply address itself — the Reply-To header routes replies to the applicant
            sendWithReplyTo(from, replyTo, adminApplicationSubject, html);
            log.info("Admin application email sent to inbox");
        } catch (Exception e) {
            log.error("Failed to send admin application email: {}", e.getMessage());
            throw new RuntimeException("Failed to send admin application email", e);
        }
    }

    /**
     * Notifies a contractor by email that a new incident has been assigned to them.
     *
     * @param to              contractor's decrypted email address
     * @param firstname       contractor's first name
     * @param incidentType    display name of the assigned {@code IssueType}
     * @param locationAddress human-readable location of the incident
     * @param incidentId      UUID of the assigned incident, included for reference
     */
    @Override
    @Async("asyncExecutor")
    public void sendJobAssignedEmail(String to, String firstname, String incidentType, String locationAddress, String incidentId) {
        try {
            log.info("Sending job assignment email to {}", to);

            String html = jobAssignedTemplate.getContentAsString(StandardCharsets.UTF_8)
                    .replace("{{userName}}", firstname)
                    .replace("{{incidentType}}", incidentType)
                    .replace("{{locationAddress}}", locationAddress)
                    .replace("{{incidentId}}", incidentId)
                    .replace("{{dashboardUrl}}", jobAssignedUrl)
                    .replace("{{currentYear}}", String.valueOf(Year.now().getValue()));

            send(to, jobAssignedSubject, html);
            log.info("Job assignment email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send job assignment email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send job assignment email", e);
        }
    }

    /**
     * Notifies an applicant of the outcome of their admin access application.
     *
     * @param to        applicant's decrypted email address
     * @param firstname applicant's first name
     * @param approved  {@code true} if the application was approved, {@code false} if rejected
     */
    @Override
    @Async("asyncExecutor")
    public void sendAdminApplicationDecisionEmail(String to, String firstname, boolean approved) {
        try {
            log.info("Sending admin application decision email ({}) to {}", approved ? "APPROVED" : "REJECTED", to);

            String decisionTitle = approved ? "Application Approved" : "Application Not Approved";
            String decisionMessage = approved
                    ? "Your admin access application has been approved. You now have admin privileges on Reporthole — sign out and back in for the change to take effect."
                    : "Your admin access application was not approved at this time. If you believe this is a mistake, please contact the Reporthole team.";
            String decisionColor = approved ? "#16A34A" : "#DC2626";

            String html = adminApplicationDecisionTemplate.getContentAsString(StandardCharsets.UTF_8)
                    .replace("{{userName}}", firstname)
                    .replace("{{decisionTitle}}", decisionTitle)
                    .replace("{{decisionMessage}}", decisionMessage)
                    .replace("{{decisionColor}}", decisionColor)
                    .replace("{{currentYear}}", String.valueOf(Year.now().getValue()));

            String subject = approved ? adminApplicationApprovedSubject : adminApplicationRejectedSubject;
            send(to, subject, html);
            log.info("Admin application decision email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send admin application decision email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send admin application decision email", e);
        }
    }

    /**
     * Sends a contractor invite email with a single-use registration link.
     *
     * @param to        contractor's email address
     * @param inviteUrl full registration URL including the invite token
     */
    @Override
    @Async("asyncExecutor")
    public void sendContractorInviteEmail(String to, String inviteUrl) {
        try {
            log.info("Sending contractor invite email to {}", to);

            String html = contractorInviteTemplate.getContentAsString(StandardCharsets.UTF_8)
                    .replace("{{inviteUrl}}", inviteUrl)
                    .replace("{{currentYear}}", String.valueOf(Year.now().getValue()));

            send(to, contractorInviteSubject, html);
            log.info("Contractor invite email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send contractor invite email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send contractor invite email", e);
        }
    }

    /**
     * Sends a municipality admin-specific email verification link after registration.
     *
     * @param to        admin's email address
     * @param firstname admin's first name
     * @param verifyUrl full verification URL including the token
     */
    @Override
    @Async("asyncExecutor")
    public void sendAdminVerificationEmail(String to, String firstname, String verifyUrl) {
        try {
            log.info("Sending admin verification email to {}", to);

            String html = adminVerificationTemplate.getContentAsString(StandardCharsets.UTF_8)
                    .replace("{{userName}}", firstname)
                    .replace("{{verificationUrl}}", verifyUrl)
                    .replace("{{expiryHours}}", "24")
                    .replace("{{currentYear}}", String.valueOf(Year.now().getValue()));

            send(to, adminVerificationSubject, html);
            log.info("Admin verification email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send admin verification email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Sends a municipality admin registration token to the supplied email address.
     *
     * @param to               recipient email address
     * @param tokenValue       the MUNI-XXXXXXXXXXXX token to enter at registration
     * @param municipalityName name of the municipality this token is scoped to
     * @param expiresLabel     human-readable expiry, e.g. "in 30 days" or "never"
     * @param registrationUrl  full URL of the registration page
     */
    @Override
    @Async("asyncExecutor")
    public void sendMunicipalityTokenEmail(String to, String tokenValue, String municipalityName,
                                           String expiresLabel, String registrationUrl) {
        try {
            log.info("Sending municipality token email to {}", to);
            String html = municipalityTokenTemplate.getContentAsString(StandardCharsets.UTF_8)
                    .replace("{{tokenValue}}", tokenValue)
                    .replace("{{municipalityName}}", municipalityName)
                    .replace("{{expiresLabel}}", expiresLabel)
                    .replace("{{registrationUrl}}", registrationUrl)
                    .replace("{{currentYear}}", String.valueOf(Year.now().getValue()));
            send(to, municipalityTokenSubject, html);
            log.info("Municipality token email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send municipality token email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send municipality token email", e);
        }
    }

    /**
     * Sends a contractor-specific email verification link after registration.
     *
     * @param to        contractor's email address
     * @param firstname contractor's first name
     * @param verifyUrl full verification URL including the token
     */
    @Override
    @Async("asyncExecutor")
    public void sendContractorVerificationEmail(String to, String firstname, String verifyUrl) {
        try {
            log.info("Sending contractor verification email to {}", to);

            String html = contractorVerificationTemplate.getContentAsString(StandardCharsets.UTF_8)
                    .replace("{{userName}}", firstname)
                    .replace("{{verificationUrl}}", verifyUrl)
                    .replace("{{expiryHours}}", "24")
                    .replace("{{currentYear}}", String.valueOf(Year.now().getValue()));

            send(to, contractorVerificationSubject, html);
            log.info("Contractor verification email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send contractor verification email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private void send(String to, String subject, String html) throws Exception {
        MimeMessageHelper helper = new MimeMessageHelper(
                mailSender.createMimeMessage(), true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(helper.getMimeMessage());
    }

    private void sendWithReplyTo(String to, String replyTo, String subject, String html) throws Exception {
        MimeMessageHelper helper = new MimeMessageHelper(
                mailSender.createMimeMessage(), true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        helper.getMimeMessage().setReplyTo(InternetAddress.parse(replyTo));
        mailSender.send(helper.getMimeMessage());
    }
}
