package za.co.urbaneye.reporthole.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.security.Jwt;
import za.co.urbaneye.reporthole.security.SecretUtil;
import za.co.urbaneye.reporthole.user.dto.AuthResponse;
import za.co.urbaneye.reporthole.user.dto.IUserMapper;
import za.co.urbaneye.reporthole.user.dto.LoginRequest;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserStatus;
import za.co.urbaneye.reporthole.user.exception.UserServiceException;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;
import za.co.urbaneye.reporthole.user.service.interfaces.ILoginService;
import za.co.urbaneye.reporthole.user.service.interfaces.IUserAuthService;

import java.util.Optional;

/**
 * Service implementation responsible for user registration
 * and authentication operations.
 *
 * <p>This class contains the business logic for:</p>
 * <ul>
 *     <li>Registering new users</li>
 *     <li>Hashing and storing passwords securely</li>
 *     <li>Generating email hashes for private lookups</li>
 *     <li>Authenticating users during login</li>
 *     <li>Generating JWT access tokens</li>
 * </ul>
 *
 * <p>Implements {@link IUserAuthService}.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements ILoginService {

    /**
     * Repository for auth persistence operations.
     */
    private final IUserAuthRepository authRepository;

    private final IUserRepository userRepository;
    /**
     * Mapper for converting DTOs to entities.
     */
    private final IUserMapper mapper;

    /**
     * Password encoder for secure password hashing and verification.
     */
    private final PasswordEncoder encoder;

    /**
     * JWT utility for token generation.
     */
    private final Jwt jwt;

    /**
     * Authenticates a user and returns a JWT token.
     *
     * <p>Steps performed:</p>
     * <ul>
     *     <li>Hashes the provided email</li>
     *     <li>Looks up the user by email hash</li>
     *     <li>Validates password</li>
     *     <li>Generates JWT token if successful</li>
     * </ul>
     *
     * @param user login request credentials
     * @return authentication response containing the JWT token and user role
     * @throws UserServiceException if user is not found
     *                              or credentials are invalid
     */
    @Override
    public AuthResponse loginUser(LoginRequest user) {
        log.info("Login attempt received");

        final String emailHash = SecretUtil.hashEmail(user.email());
        log.debug("Looking up user by email hash: {}", emailHash);
        final Optional<UserAuth> savedUserAuth = authRepository.findByEmailHash(emailHash);

        if (!savedUserAuth.isPresent()) {
            log.info("User with email {} not found", emailHash);
            throw new UserServiceException("User not found");

        }

        final UserAuth userAuth = savedUserAuth.get();

        if (userAuth.getStatus().equals(UserStatus.DELETED)) {
            // treat deleted account same as not found to avoid leaking account existence
            throw new UserServiceException("User not found");
        } else if (userAuth.getStatus().equals(UserStatus.PENDING_VERIFICATION)) {
            log.info("User email {} not verified", emailHash);
            throw new UserServiceException("User not verified");
        } else if (userAuth.getStatus().equals(UserStatus.LOCKED)) {
            log.info("User with email {} locked", emailHash);
            throw new UserServiceException("User account locked");
        }
        else if (!encoder.matches(user.password(), userAuth.getPassword())) {
            userAuth.setRetries(userAuth.getRetries() + 1);
            if (userAuth.getRetries() >= 3) {
                userAuth.setStatus(UserStatus.LOCKED);
                authRepository.save(userAuth);
                log.warn("User {} locked after {} failed attempts", emailHash, userAuth.getRetries());
                throw new UserServiceException("Too many failed attempts. Your account has been locked. Please reset your password.");
            }
            authRepository.save(userAuth);
            int remaining = 3 - userAuth.getRetries();
            log.info("Failed login for {}. {} attempt(s) remaining.", emailHash, remaining);
            throw new UserServiceException(String.format("Incorrect login credentials. %d attempt(s) remaining before lockout.", remaining));
        }

        userAuth.setRetries(0);
        authRepository.save(userAuth);

        final User found = userRepository.findById(userAuth.getAuthId()).get();
        final String token = jwt.generateToken(found.getUserId(), found.getRole());

        return new AuthResponse(token, found.getRole(), found.getUserId());
    }
}
