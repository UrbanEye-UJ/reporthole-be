package za.co.urbaneye.reporthole.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.co.urbaneye.reporthole.security.Jwt;
import za.co.urbaneye.reporthole.user.dto.AuthResponse;
import za.co.urbaneye.reporthole.user.dto.IUserMapper;
import za.co.urbaneye.reporthole.user.dto.LoginRequest;
import za.co.urbaneye.reporthole.user.dto.RegisterRequest;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.entity.UserStatus;
import za.co.urbaneye.reporthole.user.exception.UserServiceException;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;
import za.co.urbaneye.reporthole.user.service.impl.LoginServiceImpl;
import za.co.urbaneye.reporthole.user.service.impl.RegistrationServiceImpl;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IUserAuthServiceImplTest {

    @Mock
    private IUserAuthRepository repository;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IUserMapper mapper;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private Jwt jwt;

    @InjectMocks
    RegistrationServiceImpl registrationService;

    @InjectMocks
    LoginServiceImpl loginService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setup() {
        registerRequest = new RegisterRequest(
                "John", "Doe", "john@mail.com", UserRole.CIVILIAN, "Test@Pass1", "0711111111"
        );
    }

    @Test
    void shouldRegisterUser() {
        when(repository.findByEmailHash(anyString())).thenReturn(Optional.empty());
        when(mapper.toAuthEntity(registerRequest)).thenReturn(UserAuth.builder()
                .status(UserStatus.ACTIVE).retries(0).build());
        when(mapper.toUserEntity(registerRequest)).thenReturn(new User());
        when(repository.save(any(UserAuth.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(encoder.encode(anyString())).thenReturn("hashed");

        registrationService.registerUser(registerRequest);

        verify(repository).save(any(UserAuth.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowWhenUserExists() {
        UserAuth existing = UserAuth.builder()
                .authId(UUID.randomUUID())
                .status(UserStatus.ACTIVE)
                .retries(0)
                .build();
        when(repository.findByEmailHash(anyString())).thenReturn(Optional.of(existing));

        assertThrows(UserServiceException.class,
                () -> registrationService.registerUser(registerRequest));
    }

    @Test
    void shouldLoginUser() {
        UUID userId = UUID.randomUUID();

        UserAuth userAuth = UserAuth.builder()
                .authId(userId)
                .password("hashed")
                .status(UserStatus.ACTIVE)
                .retries(0)
                .build();

        User user = new User();
        user.setUserId(userId);
        user.setRole(UserRole.CIVILIAN);

        when(repository.findByEmailHash(anyString())).thenReturn(Optional.of(userAuth));
        when(encoder.matches("Test@Pass1", "hashed")).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwt.generateToken(any(), any())).thenReturn("token");

        AuthResponse result = loginService.loginUser(new LoginRequest("john@mail.com", "Test@Pass1"));

        assertEquals("token", result.token());
        assertEquals(UserRole.CIVILIAN, result.role());
        verify(repository).save(userAuth);
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(repository.findByEmailHash(anyString())).thenReturn(Optional.empty());

        assertThrows(UserServiceException.class,
                () -> loginService.loginUser(new LoginRequest("a@b.com", "pass")));
    }

    @Test
    void shouldThrowWhenPasswordWrong() {
        UserAuth userAuth = UserAuth.builder()
                .authId(UUID.randomUUID())
                .password("hashed")
                .status(UserStatus.ACTIVE)
                .retries(0)
                .build();

        when(repository.findByEmailHash(anyString())).thenReturn(Optional.of(userAuth));
        when(encoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(UserServiceException.class,
                () -> loginService.loginUser(new LoginRequest("a@b.com", "wrong")));
    }

    @Test
    void shouldIncrementRetriesOnWrongPassword() {
        UserAuth userAuth = UserAuth.builder()
                .authId(UUID.randomUUID())
                .password("hashed")
                .status(UserStatus.ACTIVE)
                .retries(0)
                .build();

        when(repository.findByEmailHash(anyString())).thenReturn(Optional.of(userAuth));
        when(encoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(UserServiceException.class,
                () -> loginService.loginUser(new LoginRequest("a@b.com", "wrong")));

        assertEquals(1, userAuth.getRetries());
        verify(repository).save(userAuth);
    }

    @Test
    void shouldLockAccountAfterThreeFailedAttempts() {
        UserAuth userAuth = UserAuth.builder()
                .authId(UUID.randomUUID())
                .password("hashed")
                .status(UserStatus.ACTIVE)
                .retries(2)
                .build();

        when(repository.findByEmailHash(anyString())).thenReturn(Optional.of(userAuth));
        when(encoder.matches(anyString(), anyString())).thenReturn(false);

        UserServiceException ex = assertThrows(UserServiceException.class,
                () -> loginService.loginUser(new LoginRequest("a@b.com", "wrong")));

        assertEquals(UserStatus.LOCKED, userAuth.getStatus());
        assertTrue(ex.getMessage().contains("locked"));
        verify(repository).save(userAuth);
    }

    @Test
    void shouldThrowWhenAccountLocked() {
        UserAuth userAuth = UserAuth.builder()
                .authId(UUID.randomUUID())
                .password("hashed")
                .status(UserStatus.LOCKED)
                .retries(3)
                .build();

        when(repository.findByEmailHash(anyString())).thenReturn(Optional.of(userAuth));

        UserServiceException ex = assertThrows(UserServiceException.class,
                () -> loginService.loginUser(new LoginRequest("a@b.com", "pass")));

        assertTrue(ex.getMessage().contains("locked"));
        verify(encoder, never()).matches(anyString(), anyString());
    }

    @Test
    void shouldThrowWhenAccountPendingVerification() {
        UserAuth userAuth = UserAuth.builder()
                .authId(UUID.randomUUID())
                .password("hashed")
                .status(UserStatus.PENDING_VERIFICATION)
                .retries(0)
                .build();

        when(repository.findByEmailHash(anyString())).thenReturn(Optional.of(userAuth));

        UserServiceException ex = assertThrows(UserServiceException.class,
                () -> loginService.loginUser(new LoginRequest("a@b.com", "pass")));

        assertTrue(ex.getMessage().contains("not verified") || ex.getMessage().contains("verif"));
        verify(encoder, never()).matches(anyString(), anyString());
    }
}
