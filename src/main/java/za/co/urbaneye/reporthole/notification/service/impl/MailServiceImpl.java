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
