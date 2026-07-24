package za.co.urbaneye.reporthole.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.notification.service.interfaces.IMailService;
import za.co.urbaneye.reporthole.security.SecretUtil;
import za.co.urbaneye.reporthole.user.dto.IUserMapper;
import za.co.urbaneye.reporthole.user.dto.RegisterRequest;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.entity.UserStatus;
import za.co.urbaneye.reporthole.user.exception.UserServiceException;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;
import za.co.urbaneye.reporthole.user.service.interfaces.IRegistrationService;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Handles new user registration.
 *
 * <p>When {@code app.features.email-verification-enabled} is true, the account
 * is created in {@link UserStatus#PENDING_VERIFICATION} and a unique token is
 * stored on {@link UserAuth} — the token is embedded in the verification link
 * sent by email. Once the user clicks the link ({@code POST /auth/verify}), the
 * status is set to {@link UserStatus#ACTIVE} and the token is cleared.</p>
 *
 * <p>When the feature flag is false (default), the account is immediately
 * {@link UserStatus#ACTIVE} and no email is sent.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements IRegistrationService {

    private final IUserAuthRepository authRepository;
    private final IUserRepository userRepository;
    private final IUserMapper mapper;
    private final PasswordEncoder encoder;
    private final IMailService mailService;

    @Value("${app.features.email-verification-enabled:false}")
    private boolean emailVerificationEnabled;

    @Value("${mail.verification-url}")
    private String verificationBaseUrl;

    /**
     * Creates a new user account.
     *
     * @param user registration request details
     * @throws UserServiceException if the email is already registered
     */
    @Override
    @Transactional
    public void registerUser(RegisterRequest user) {
        try {
            log.info("Registering user");

            final String emailHash = SecretUtil.hashEmail(user.email());
            final var existingAuth = authRepository.findByEmailHash(emailHash);

            if (existingAuth.isPresent()) {
                final UserAuth auth = existingAuth.get();
                if (emailVerificationEnabled && auth.getStatus() == UserStatus.PENDING_VERIFICATION) {
                    // Resend a fresh verification email instead of blocking the user
                    auth.setVerificationToken(UUID.randomUUID().toString());
                    auth.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
                    authRepository.save(auth);
                    final User existingUser = userRepository.findById(auth.getAuthId()).orElse(null);
                    final String firstName = existingUser != null ? existingUser.getFirstName() : "there";
                    final String verifyUrl = verificationBaseUrl + auth.getVerificationToken();
                    mailService.sendVerificationEmail(auth.getEmail(), firstName, verifyUrl);
                    log.info("Resent verification email for pending account {}", auth.getAuthId());
                    return;
                }
                throw new UserServiceException("User already exists");
            }

            final UserAuth authEntity = mapper.toAuthEntity(user);
            authEntity.setEmailHash(emailHash);
            authEntity.setPassword(encoder.encode(user.password()));
            authEntity.setStatus(emailVerificationEnabled ? UserStatus.PENDING_VERIFICATION : UserStatus.ACTIVE);

            if (emailVerificationEnabled) {
                authEntity.setVerificationToken(UUID.randomUUID().toString());
                authEntity.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
            }

            final UserAuth savedAuth = authRepository.save(authEntity);

            final User userEntity = mapper.toUserEntity(user);
            userEntity.setUserId(savedAuth.getAuthId());
            userEntity.setRole(UserRole.CIVILIAN);
            final User savedUser = userRepository.save(userEntity);

            log.info("User registered successfully: {}", savedUser.getUserId());

            if (emailVerificationEnabled) {
                final String verifyUrl = verificationBaseUrl + savedAuth.getVerificationToken();
                mailService.sendVerificationEmail(savedAuth.getEmail(), savedUser.getFirstName(), verifyUrl);
            }

        } catch (final UserServiceException ex) {
            throw ex;
        } catch (final Exception ex) {
            log.error(ex.getMessage(), ex);
            throw new UserServiceException(ex.getMessage());
        }
    }

    /**
     * Activates the account associated with {@code token}, then clears the token.
     *
     * @param token the verification token stored on {@link UserAuth}
     * @throws UserServiceException if the token is not found
     */
    /**
     * Activates the account associated with {@code token}, then clears the token.
     *
     * @param token the verification token stored on {@link UserAuth}
     * @throws UserServiceException if the token is not found or has expired
     */
    @Override
    @Transactional
    public void verifyEmail(String token) {
        final UserAuth userAuth = authRepository.findByVerificationToken(token)
                .orElseThrow(() -> new UserServiceException("Invalid or already-used verification link"));

        if (userAuth.getVerificationTokenExpiresAt() != null
                && userAuth.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UserServiceException("Verification link has expired");
        }

        userAuth.setStatus(UserStatus.ACTIVE);
        userAuth.setVerificationToken(null);
        userAuth.setVerificationTokenExpiresAt(null);
        authRepository.save(userAuth);

        log.info("Email verified for user {}", userAuth.getAuthId());
    }
}
