package za.co.urbaneye.reporthole.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.security.Aes;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class IUserAuthServiceImpl implements IUserAuthService {

    private final IUserAuthRepository repository;
    private final IUserMapper mapper;
    private final PasswordEncoder encoder;
    private final Jwt jwt;

    @Override
    public void registerUser(RegisterRequest user) {
        try {
            log.info("Registering user");

            if(repository.existsDistinctByEmail(user.email())) {
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

    @Override
    public String loginUser(LoginRequest user) {
        log.info("Login user with email {}", user.email());

       final String emailHash = SecretUtil.hashEmail(user.email());
       final Optional<User> savedUser = repository.findByEmailHash(emailHash);

        if(!savedUser.isPresent()) {
            log.info("User with email {} not found", emailHash);
            throw new UserServiceException("User not found");
        } else if(!encoder.matches(user.password(), savedUser.get().getPassword())) {
            log.info("Passwords don't match");
            throw new UserServiceException("Incorrect password");
        }

        return jwt.generateToken(savedUser.get().getUserId(),savedUser.get().getRole());
    }
}
