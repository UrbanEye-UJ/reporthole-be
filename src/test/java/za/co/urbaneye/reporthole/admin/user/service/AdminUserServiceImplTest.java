package za.co.urbaneye.reporthole.admin.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.urbaneye.reporthole.admin.user.dto.CivilianSummaryResponse;
import za.co.urbaneye.reporthole.admin.user.service.impl.AdminUserServiceImpl;
import za.co.urbaneye.reporthole.incident.repository.IncidentReporterRepository;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.entity.UserStatus;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminUserServiceImpl}: masking helpers and the getCivilians() aggregation.
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock private IUserRepository userRepository;
    @Mock private IUserAuthRepository userAuthRepository;
    @Mock private IncidentReporterRepository incidentReporterRepository;

    @InjectMocks
    private AdminUserServiceImpl service;

    // ── maskName ──────────────────────────────────────────────────────────────

    @Test
    void maskName_normalInput_returnsFirstNameAndLastInitial() {
        assertThat(AdminUserServiceImpl.maskName("Jane", "Doe")).isEqualTo("Jane D.");
    }

    @Test
    void maskName_singleLetter_lastNameStillInitialised() {
        assertThat(AdminUserServiceImpl.maskName("Al", "B")).isEqualTo("Al B.");
    }

    @Test
    void maskName_emptyLastName_returnsFirstNameOnly() {
        assertThat(AdminUserServiceImpl.maskName("Jane", "")).isEqualTo("Jane");
    }

    @Test
    void maskName_nullLastName_returnsFirstNameOnly() {
        assertThat(AdminUserServiceImpl.maskName("Jane", null)).isEqualTo("Jane");
    }

    @Test
    void maskName_nullFirstName_usesEmptyString() {
        assertThat(AdminUserServiceImpl.maskName(null, "Doe")).isEqualTo(" D.");
    }

    @Test
    void maskName_bothNull_returnsDash() {
        assertThat(AdminUserServiceImpl.maskName(null, null)).isEqualTo("—");
    }

    @Test
    void maskName_bothBlank_returnsDash() {
        assertThat(AdminUserServiceImpl.maskName("  ", "  ")).isEqualTo("—");
    }

    // ── maskEmail ─────────────────────────────────────────────────────────────

    @Test
    void maskEmail_normalEmail_masksLocalPart() {
        assertThat(AdminUserServiceImpl.maskEmail("jane@gmail.com")).isEqualTo("j***@gmail.com");
    }

    @Test
    void maskEmail_singleCharLocal_masksRemainder() {
        assertThat(AdminUserServiceImpl.maskEmail("a@domain.co.za")).isEqualTo("a***@domain.co.za");
    }

    @Test
    void maskEmail_atSignAtStart_prefixesStars() {
        assertThat(AdminUserServiceImpl.maskEmail("@domain.com")).isEqualTo("***@domain.com");
    }

    @Test
    void maskEmail_null_returnsDash() {
        assertThat(AdminUserServiceImpl.maskEmail(null)).isEqualTo("—");
    }

    @Test
    void maskEmail_noAtSign_returnsDash() {
        assertThat(AdminUserServiceImpl.maskEmail("notanemail")).isEqualTo("—");
    }

    // ── getCivilians ──────────────────────────────────────────────────────────

    @Test
    void getCivilians_returnsProjectedSummaries_sortedByCreatedAtDesc() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        User civ1 = User.builder()
                .userId(id1).role(UserRole.CIVILIAN)
                .firstName("Alice").lastName("Khumalo")
                .createdAt(LocalDateTime.of(2024, 1, 10, 0, 0))
                .build();
        User civ2 = User.builder()
                .userId(id2).role(UserRole.CIVILIAN)
                .firstName("Bob").lastName("Mokoena")
                .createdAt(LocalDateTime.of(2024, 3, 5, 0, 0))
                .build();

        UserAuth auth1 = UserAuth.builder().authId(id1).email("alice@example.com").status(UserStatus.ACTIVE).build();
        UserAuth auth2 = UserAuth.builder().authId(id2).email("bob@example.com").status(UserStatus.ACTIVE).build();

        when(userRepository.findByRole(UserRole.CIVILIAN)).thenReturn(List.of(civ1, civ2));
        when(userAuthRepository.findAll()).thenReturn(List.of(auth1, auth2));
        when(incidentReporterRepository.countByUser_UserId(id1)).thenReturn(3L);
        when(incidentReporterRepository.countByUser_UserId(id2)).thenReturn(7L);

        List<CivilianSummaryResponse> result = service.getCivilians();

        assertThat(result).hasSize(2);
        // civ2 has a later createdAt so it should come first
        assertThat(result.get(0).userId()).isEqualTo(id2);
        assertThat(result.get(0).maskedName()).isEqualTo("Bob M.");
        assertThat(result.get(0).maskedEmail()).isEqualTo("b***@example.com");
        assertThat(result.get(0).incidentCount()).isEqualTo(7L);

        assertThat(result.get(1).userId()).isEqualTo(id1);
        assertThat(result.get(1).maskedName()).isEqualTo("Alice K.");
        assertThat(result.get(1).incidentCount()).isEqualTo(3L);
    }

    @Test
    void getCivilians_missingAuth_usesDashEmailAndActiveStatus() {
        UUID id = UUID.randomUUID();
        User civ = User.builder()
                .userId(id).role(UserRole.CIVILIAN)
                .firstName("Ghost").lastName("User")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByRole(UserRole.CIVILIAN)).thenReturn(List.of(civ));
        when(userAuthRepository.findAll()).thenReturn(List.of());
        when(incidentReporterRepository.countByUser_UserId(id)).thenReturn(0L);

        List<CivilianSummaryResponse> result = service.getCivilians();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).maskedEmail()).isEqualTo("—");
        assertThat(result.get(0).status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void getCivilians_noCivilians_returnsEmptyList() {
        when(userRepository.findByRole(UserRole.CIVILIAN)).thenReturn(List.of());
        when(userAuthRepository.findAll()).thenReturn(List.of());

        assertThat(service.getCivilians()).isEmpty();
    }
}
