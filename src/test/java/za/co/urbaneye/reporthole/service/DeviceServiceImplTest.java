package za.co.urbaneye.reporthole.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.urbaneye.reporthole.device.dto.DeviceTokenResponse;
import za.co.urbaneye.reporthole.device.entity.DashcamDevice;
import za.co.urbaneye.reporthole.device.exception.DeviceServiceException;
import za.co.urbaneye.reporthole.device.repository.DashcamDeviceRepository;
import za.co.urbaneye.reporthole.device.service.impl.DeviceServiceImpl;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeviceServiceImpl}.
 *
 * <p>Verifies that device token generation correctly creates a
 * {@link DashcamDevice} linked to the requesting user, and that
 * missing user accounts produce an appropriate exception.</p>
 */
@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {

    @Mock
    private DashcamDeviceRepository deviceRepository;

    @Mock
    private IUserAuthRepository userRepository;

    @InjectMocks
    private DeviceServiceImpl deviceService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private User stubUser() {
        User user = new User();
        user.setUserId(USER_ID);
        user.setRole(UserRole.CIVILIAN);
        return user;
    }

    @Test
    void generateToken_returnsTokenAndSavesDevice_whenUserExists() {
        User user = stubUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DeviceTokenResponse response = deviceService.generateToken(USER_ID.toString());

        assertThat(response).isNotNull();
        assertThat(response.deviceToken()).isNotBlank();
        // UUID device tokens never contain dots — the filter uses this to distinguish them from JWTs
        assertThat(response.deviceToken()).doesNotContain(".");

        ArgumentCaptor<DashcamDevice> captor = ArgumentCaptor.forClass(DashcamDevice.class);
        verify(deviceRepository).save(captor.capture());
        DashcamDevice saved = captor.getValue();
        assertThat(saved.getDeviceToken()).isEqualTo(response.deviceToken());
        assertThat(saved.getUser()).isEqualTo(user);
    }

    @Test
    void generateToken_producesUniqueTokensOnSubsequentCalls() {
        User user = stubUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DeviceTokenResponse first = deviceService.generateToken(USER_ID.toString());
        DeviceTokenResponse second = deviceService.generateToken(USER_ID.toString());

        assertThat(first.deviceToken()).isNotEqualTo(second.deviceToken());
    }

    @Test
    void generateToken_throwsDeviceServiceException_whenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.generateToken(USER_ID.toString()))
                .isInstanceOf(DeviceServiceException.class)
                .hasMessageContaining(USER_ID.toString());

        verify(deviceRepository, never()).save(any());
    }
}
