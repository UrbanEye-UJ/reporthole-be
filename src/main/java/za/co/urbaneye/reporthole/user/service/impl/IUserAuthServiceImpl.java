package za.co.urbaneye.reporthole.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.user.dto.IUserMapper;
import za.co.urbaneye.reporthole.user.dto.LoginRequest;
import za.co.urbaneye.reporthole.user.dto.RegisterRequest;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.exception.UserServiceException;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.service.interfaces.IUserAuthService;

@Slf4j
@Service
@RequiredArgsConstructor
public class IUserAuthServiceImpl implements IUserAuthService {

    private final IUserAuthRepository repository;
    private final IUserMapper mapper;
    private final PasswordEncoder encoder;

    @Override
    public void registerUser(RegisterRequest user) {
        try {
            log.info("Registering user");

            if(repository.existsDistinctByEmail(user.email())) {
                throw new UserServiceException("User already exists");
            }

            final User userEntity = mapper.toEntity(user);
            userEntity.setPassword(encoder.encode(user.password()));

            repository.save(userEntity);
        } catch (final Exception ex) {
            log.error(ex.getMessage(), ex);
            throw new UserServiceException(ex.getMessage());
        }
    }

    @Override
    public String loginUser(LoginRequest user) {
        return "";
    }
}
