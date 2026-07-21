package za.co.urbaneye.reporthole.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.urbaneye.reporthole.notification.service.interfaces.IMailService;
import za.co.urbaneye.reporthole.security.SecretUtil;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserStatus;
import za.co.urbaneye.reporthole.user.exception.UserServiceException;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;
import za.co.urbaneye.reporthole.user.service.interfaces.IPasswordResetService;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implements the two-step password reset / account unlock flow.
 *
 * <p>The reset token is stored directly on {@link UserAuth} — no separate
 * token table is needed. Resetting the password via a valid token also sets
 * account status to {@link UserStatus#ACTIVE} and clears the retry counter,
 * effectively unlocking accounts locked after too many failed login attempts.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements IPasswordResetService {

    private final IUserAuthRepository authRepository;
    private final IUserRepository userRepository;
    private final PasswordEncoder encoder;
    private final IMailService mailService;

    @Value("${mail.password-reset-url}")
    private String passwordResetBaseUrl;

    /**
     * Generates a reset token on the {@link UserAuth} record for {@code email},
     * then emails the full reset link to the user.
     *
     * @param email plaintext email of the account to reset
     * @throws UserServiceException if no account exists for that email
     */
    @Override
    @Transactional
    public void requestReset(String email) {
        final String emailHash = SecretUtil.hashEmail(email);
        final UserAuth userAuth = authRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> new UserServiceException("User not found"));

        userAuth.setPasswordResetToken(UUID.randomUUID().toString());
        userAuth.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusHours(1));
        authRepository.save(userAuth);

        final String firstName = userRepository.findById(userAuth.getAuthId())
                .map(u -> u.getFirstName())
                .orElse("there");

        final String resetUrl = passwordResetBaseUrl + userAuth.getPasswordResetToken();
        mailService.sendPasswordResetEmail(userAuth.getEmail(), firstName, resetUrl);

        log.info("Password reset link issued for user {}", userAuth.getAuthId());
    }

    /**
     * Validates the reset token, sets the new password, and unlocks the account.
     * Clears the token from {@link UserAuth} once consumed.
     *
     * @param token       the reset token string from the emailed link
     * @param newPassword the new plaintext password to hash and store
     * @throws UserServiceException if the token is not found, expired, or already used
     */
    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        final UserAuth userAuth = authRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new UserServiceException("Invalid or expired reset link"));

        if (userAuth.getPasswordResetTokenExpiresAt() == null
                || userAuth.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UserServiceException("Invalid or expired reset link");
        }

        userAuth.setPassword(encoder.encode(newPassword));
        userAuth.setStatus(UserStatus.ACTIVE);
        userAuth.setRetries(0);
        userAuth.setPasswordResetToken(null);
        userAuth.setPasswordResetTokenExpiresAt(null);
        authRepository.save(userAuth);

        log.info("Password reset successfully for user {}", userAuth.getAuthId());
    }
}
