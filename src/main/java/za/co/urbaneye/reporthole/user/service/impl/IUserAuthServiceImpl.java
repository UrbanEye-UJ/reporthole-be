package za.co.urbaneye.reporthole.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.security.Jwt;
import za.co.urbaneye.reporthole.security.SecretUtil;
import za.co.urbaneye.reporthole.user.dto.IUserMapper;
import za.co.urbaneye.reporthole.user.dto.LoginRequest;
import za.co.urbaneye.reporthole.user.dto.RegisterRequest;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.exception.UserServiceException;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
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
public class IUserAuthServiceImpl implements IUserAuthService {

    /**
     * Repository for user persistence operations.
     */
    private final IUserAuthRepository repository;

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
     * Registers a new user account.
     *
     * <p>Steps performed:</p>
     * <ul>
     *     <li>Checks whether the user already exists</li>
     *     <li>Maps request DTO to entity</li>
     *     <li>Generates email hash</li>
     *     <li>Hashes password</li>
     *     <li>Saves user to the database</li>
     * </ul>
     *
     * @param user registration request details
     * @throws UserServiceException if registration fails
     */
    @Override
    public void registerUser(RegisterRequest user) {
        try {
            log.info("Registering user");

            if (repository.existsDistinctByEmail(user.email())) {
                throw new UserServiceException("User already exists");
            }

            final User userEntity = mapper.toEntity(user);
            userEntity.setEmailHash(SecretUtil.hashEmail(user.email()));
            userEntity.setPassword(encoder.encode(user.password()));

            repository.save(userEntity);

        } catch (final Exception ex) {
            log.error(ex.getMessage(), ex);
            throw new UserServiceException(ex.getMessage());
        }
    }

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
     * @return signed JWT token
     * @throws UserServiceException if user is not found
     *                              or credentials are invalid
     */
    @Override
    public String loginUser(LoginRequest user) {
        log.info("Login user with email {}", user.email());

        final String emailHash = SecretUtil.hashEmail(user.email());
        final Optional<User> savedUser = repository.findByEmailHash(emailHash);

        if (!savedUser.isPresent()) {
            log.info("User with email {} not found", emailHash);
            throw new UserServiceException("User not found");

        } else if (!encoder.matches(user.password(), savedUser.get().getPassword())) {
            log.info("Passwords don't match");
            throw new UserServiceException("Incorrect password");
        }

        return jwt.generateToken(
                savedUser.get().getUserId(),
                savedUser.get().getRole()
        );
    }
}