package za.co.urbaneye.reporthole.notification.service.impl;

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
import java.time.Year;

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

    private void send(String to, String subject, String html) throws Exception {
        MimeMessageHelper helper = new MimeMessageHelper(
                mailSender.createMimeMessage(), true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(helper.getMimeMessage());
    }
}
