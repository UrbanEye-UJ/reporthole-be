package za.co.urbaneye.reporthole.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.co.urbaneye.reporthole.security.Jwt;
import za.co.urbaneye.reporthole.user.dto.IUserMapper;
import za.co.urbaneye.reporthole.user.dto.LoginRequest;
import za.co.urbaneye.reporthole.user.dto.RegisterRequest;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.exception.UserServiceException;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.service.impl.IUserAuthServiceImpl;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IUserAuthServiceImplTest {

    @Mock
    private IUserAuthRepository repository;

    @Mock
    private IUserMapper mapper;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private IUserAuthServiceImpl service;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setup() {
        registerRequest = new RegisterRequest(
                "John","Doe","john@mail.com","","CIVILIAN","123","0711111111"
        );
    }

    @Test
    void shouldRegisterUser() {

        when(repository.existsDistinctByEmail(anyString())).thenReturn(false);
        when(mapper.toEntity(registerRequest)).thenReturn(User.builder().build());
        when(encoder.encode(anyString())).thenReturn("hashed");

        service.registerUser(registerRequest);

        verify(repository).save(any(User.class));
    }

    @Test
    void shouldThrowWhenUserExists() {

        when(repository.existsDistinctByEmail(anyString())).thenReturn(true);

        assertThrows(UserServiceException.class,
                () -> service.registerUser(registerRequest));
    }

    @Test
    void shouldLoginUser() {

        User user = User.builder()
                .userId(UUID.randomUUID())
                .password("hashed")
                .role(UserRole.CIVILIAN)
                .build();

        when(repository.findByEmailHash(anyString()))
                .thenReturn(Optional.of(user));

        when(encoder.matches("123","hashed")).thenReturn(true);
        when(jwt.generateToken(any(), any())).thenReturn("token");

        String token = service.loginUser(new LoginRequest("john@mail.com","123"));

        assertEquals("token", token);
    }

    @Test
    void shouldThrowWhenUserNotFound() {

        when(repository.findByEmailHash(anyString()))
                .thenReturn(Optional.empty());

        assertThrows(UserServiceException.class,
                () -> service.loginUser(new LoginRequest("a","b")));
    }

    @Test
    void shouldThrowWhenPasswordWrong() {

        User user = User.builder().password("hashed").build();

        when(repository.findByEmailHash(anyString()))
                .thenReturn(Optional.of(user));

        when(encoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(UserServiceException.class,
                () -> service.loginUser(new LoginRequest("a","b")));
    }
}