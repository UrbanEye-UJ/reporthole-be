package za.co.urbaneye.reporthole.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.admin.application.entity.AdminApplication;
import za.co.urbaneye.reporthole.admin.application.entity.AdminApplicationStatus;
import za.co.urbaneye.reporthole.admin.application.repository.IAdminApplicationRepository;
import za.co.urbaneye.reporthole.admin.contractor.entity.ContractorInvite;
import za.co.urbaneye.reporthole.admin.contractor.repository.ContractorInviteRepository;
import za.co.urbaneye.reporthole.admin.municipality.entity.MunicipalityToken;
import za.co.urbaneye.reporthole.admin.municipality.repository.IMunicipalityTokenRepository;
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
import java.util.HashSet;
import java.util.Optional;
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
    private final IMunicipalityTokenRepository municipalityTokenRepository;
    private final IAdminApplicationRepository adminApplicationRepository;
    private final ContractorInviteRepository contractorInviteRepository;

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
                    if (existingUser != null && existingUser.getRole() == UserRole.CONTRACTOR) {
                        mailService.sendContractorVerificationEmail(auth.getEmail(), firstName, verifyUrl);
                    } else if (existingUser != null && existingUser.getRole() == UserRole.ADMIN) {
                        mailService.sendAdminVerificationEmail(auth.getEmail(), firstName, verifyUrl);
                    } else {
                        mailService.sendVerificationEmail(auth.getEmail(), firstName, verifyUrl);
                    }
                    log.info("Resent verification email for pending account {}", auth.getAuthId());
                    return;
                }
                throw new UserServiceException("User already exists");
            }

            // Resolve the optional token before persisting anything — a bad token should fail fast.
            // A UUID string → contractor invite. Any other string → municipality admin token.
            final ContractorInvite contractorInvite = resolveContractorInvite(user.token());
            final MunicipalityToken municipalityToken = contractorInvite != null
                    ? null
                    : resolveMunicipalityToken(user.token());

            final UserAuth authEntity = mapper.toAuthEntity(user);
            authEntity.setEmailHash(emailHash);
            authEntity.setPassword(encoder.encode(user.password()));
            authEntity.setStatus(emailVerificationEnabled
                    ? UserStatus.PENDING_VERIFICATION : UserStatus.ACTIVE);

            if (emailVerificationEnabled) {
                authEntity.setVerificationToken(UUID.randomUUID().toString());
                authEntity.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
            }

            final UserAuth savedAuth = authRepository.save(authEntity);

            final User userEntity = mapper.toUserEntity(user);
            userEntity.setUserId(savedAuth.getAuthId());

            if (contractorInvite != null) {
                userEntity.setRole(UserRole.CONTRACTOR);
                userEntity.setSpecialisations(new HashSet<>(contractorInvite.getSpecialisations()));
            } else if (municipalityToken != null) {
                userEntity.setRole(UserRole.ADMIN);
            } else {
                userEntity.setRole(UserRole.CIVILIAN);
            }

            final User savedUser = userRepository.save(userEntity);

            if (contractorInvite != null) {
                contractorInvite.setUsed(true);
                contractorInviteRepository.save(contractorInvite);
                log.info("User {} registered as CONTRACTOR via invite token", savedUser.getUserId());
            } else if (municipalityToken != null) {
                // Record how this admin was onboarded, so it shows in GET /admin/applications
                // alongside the legacy apply/approve flow. Already APPROVED — the security admin
                // vouched by issuing the token.
                adminApplicationRepository.save(AdminApplication.builder()
                        .user(savedUser)
                        .municipalityToken(municipalityToken.getToken())
                        .municipality(municipalityToken.getMunicipality())
                        .status(AdminApplicationStatus.APPROVED)
                        .build());
                log.info("User {} registered as ADMIN via municipality token for '{}'",
                        savedUser.getUserId(), municipalityToken.getMunicipality().getName());
            }

            log.info("User registered successfully: {}", savedUser.getUserId());

            if (emailVerificationEnabled) {
                final String verifyUrl = verificationBaseUrl + savedAuth.getVerificationToken();
                if (contractorInvite != null) {
                    mailService.sendContractorVerificationEmail(savedAuth.getEmail(), savedUser.getFirstName(), verifyUrl);
                } else if (municipalityToken != null) {
                    mailService.sendAdminVerificationEmail(savedAuth.getEmail(), savedUser.getFirstName(), verifyUrl);
                } else {
                    mailService.sendVerificationEmail(savedAuth.getEmail(), savedUser.getFirstName(), verifyUrl);
                }
            }

        } catch (final UserServiceException ex) {
            throw ex;
        } catch (final Exception ex) {
            log.error(ex.getMessage(), ex);
            throw new UserServiceException(ex.getMessage());
        }
    }

    /**
     * Tries to interpret {@code rawToken} as a contractor invite (UUID format).
     *
     * @return the matching, unused, non-expired {@link ContractorInvite}, or {@code null} if the
     *         token is blank or not a valid UUID (caller should then try the municipality path)
     * @throws UserServiceException if the UUID parses but the invite is not found, already used, or expired
     */
    private ContractorInvite resolveContractorInvite(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        final Optional<UUID> uuid = tryParseUuid(rawToken.trim());
        if (uuid.isEmpty()) {
            return null; // not UUID-shaped — caller tries municipality token instead
        }
        final ContractorInvite invite = contractorInviteRepository.findByToken(uuid.get())
                .orElseThrow(() -> new UserServiceException("Invalid or expired invite token"));
        if (invite.isUsed()) {
            throw new UserServiceException("This invite token has already been used");
        }
        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UserServiceException("This invite token has expired");
        }
        return invite;
    }

    /**
     * Resolves an optional municipality registration token.
     *
     * @param rawToken the value from the registration form; null / blank means a normal signup
     * @return the resolved, usable {@link MunicipalityToken}, or {@code null} when no token was given
     * @throws UserServiceException if a token was given but does not exist, is revoked, or has expired
     */
    private MunicipalityToken resolveMunicipalityToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        final MunicipalityToken token = municipalityTokenRepository.findByToken(rawToken.trim())
                .orElseThrow(() -> new UserServiceException("Invalid or expired invite token"));
        if (!token.isUsable()) {
            throw new UserServiceException("Invalid or expired invite token");
        }
        return token;
    }

    private static Optional<UUID> tryParseUuid(String s) {
        try {
            return Optional.of(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
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
