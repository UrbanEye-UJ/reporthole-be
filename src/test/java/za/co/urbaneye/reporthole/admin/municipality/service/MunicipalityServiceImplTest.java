package za.co.urbaneye.reporthole.admin.municipality.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import za.co.urbaneye.reporthole.admin.municipality.dto.CreateMunicipalityRequest;
import za.co.urbaneye.reporthole.admin.municipality.dto.IssueTokenRequest;
import za.co.urbaneye.reporthole.admin.municipality.dto.MunicipalityTokenResponse;
import za.co.urbaneye.reporthole.admin.municipality.entity.Municipality;
import za.co.urbaneye.reporthole.admin.municipality.entity.MunicipalityToken;
import za.co.urbaneye.reporthole.admin.municipality.exception.MunicipalityException;
import za.co.urbaneye.reporthole.admin.municipality.repository.IMunicipalityRepository;
import za.co.urbaneye.reporthole.admin.municipality.repository.IMunicipalityTokenRepository;
import za.co.urbaneye.reporthole.admin.municipality.service.impl.MunicipalityServiceImpl;
import za.co.urbaneye.reporthole.notification.service.interfaces.IMailService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MunicipalityServiceImpl} — the SECURITY_ADMIN guard, name uniqueness,
 * token generation/expiry, and revocation.
 */
@ExtendWith(MockitoExtension.class)
class MunicipalityServiceImplTest {

    @Mock private IMunicipalityRepository municipalityRepository;
    @Mock private IMunicipalityTokenRepository tokenRepository;
    @Mock private IUserRepository userRepository;
    @Mock private IMailService mailService;

    @InjectMocks
    private MunicipalityServiceImpl service;

    private static final UUID CALLER_ID = UUID.randomUUID();
    private static final UUID MUNI_ID = UUID.randomUUID();

    @BeforeEach
    void mockSecurityContext() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(CALLER_ID.toString());
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private void callerIsSecurityAdmin() {
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(
                User.builder().userId(CALLER_ID).role(UserRole.SECURITY_ADMIN)
                        .firstName("Sam").lastName("Secure").build()));
    }

    private Municipality municipality() {
        return Municipality.builder().id(MUNI_ID).name("City of Tshwane").province("Gauteng").build();
    }

    @Test
    void createMunicipality_happyPath_savesWithTrimmedNameAndProvinceDefault() {
        callerIsSecurityAdmin();
        when(municipalityRepository.existsByNameIgnoreCase("City of Tshwane")).thenReturn(false);
        when(municipalityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.createMunicipality(new CreateMunicipalityRequest("  City of Tshwane  ", "  "));

        assertThat(result.name()).isEqualTo("City of Tshwane");
        assertThat(result.province()).isEqualTo("Gauteng");
        verify(municipalityRepository).save(any(Municipality.class));
    }

    @Test
    void createMunicipality_duplicateName_throwsConflict() {
        callerIsSecurityAdmin();
        when(municipalityRepository.existsByNameIgnoreCase("City of Tshwane")).thenReturn(true);

        assertThatThrownBy(() -> service.createMunicipality(new CreateMunicipalityRequest("City of Tshwane", null)))
                .isInstanceOf(MunicipalityException.class)
                .hasMessageContaining("already exists");

        verify(municipalityRepository, never()).save(any());
    }

    @Test
    void createMunicipality_callerNotSecurityAdmin_throwsForbidden() {
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(
                User.builder().userId(CALLER_ID).role(UserRole.ADMIN).build()));

        assertThatThrownBy(() -> service.createMunicipality(new CreateMunicipalityRequest("X", null)))
                .isInstanceOf(MunicipalityException.class)
                .hasMessageContaining("Only security admins");
    }

    @Test
    void issueToken_happyPath_generatesPrefixedTokenAndNoExpiry() {
        callerIsSecurityAdmin();
        when(municipalityRepository.findById(MUNI_ID)).thenReturn(Optional.of(municipality()));
        when(tokenRepository.findByToken(anyString())).thenReturn(Optional.empty());
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MunicipalityTokenResponse result = service.issueToken(MUNI_ID, new IssueTokenRequest(null, "for Jane", null));

        assertThat(result.token()).startsWith("MUNI-");
        assertThat(result.expiresAt()).isNull();
        assertThat(result.municipalityId()).isEqualTo(MUNI_ID);
        assertThat(result.status()).isEqualTo(MunicipalityTokenResponse.Status.ACTIVE);
    }

    @Test
    void issueToken_withExpiry_setsExpiresAtInFuture() {
        callerIsSecurityAdmin();
        when(municipalityRepository.findById(MUNI_ID)).thenReturn(Optional.of(municipality()));
        when(tokenRepository.findByToken(anyString())).thenReturn(Optional.empty());
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MunicipalityTokenResponse result = service.issueToken(MUNI_ID, new IssueTokenRequest(7, null, null));

        assertThat(result.expiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void issueToken_municipalityNotFound_throwsNotFound() {
        callerIsSecurityAdmin();
        when(municipalityRepository.findById(MUNI_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.issueToken(MUNI_ID, new IssueTokenRequest(null, null, null)))
                .isInstanceOf(MunicipalityException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void revokeToken_happyPath_setsRevokedAt() {
        callerIsSecurityAdmin();
        MunicipalityToken token = MunicipalityToken.builder()
                .id(UUID.randomUUID()).token("MUNI-ABCDEFGHJKLM").municipality(municipality()).build();
        when(tokenRepository.findById(token.getId())).thenReturn(Optional.of(token));

        service.revokeToken(token.getId());

        assertThat(token.getRevokedAt()).isNotNull();
        verify(tokenRepository).save(token);
    }

    @Test
    void revokeToken_alreadyRevoked_throwsConflict() {
        callerIsSecurityAdmin();
        MunicipalityToken token = MunicipalityToken.builder()
                .id(UUID.randomUUID()).token("MUNI-ABCDEFGHJKLM").municipality(municipality())
                .revokedAt(LocalDateTime.now().minusDays(1)).build();
        when(tokenRepository.findById(token.getId())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.revokeToken(token.getId()))
                .isInstanceOf(MunicipalityException.class)
                .hasMessageContaining("already revoked");
    }

    @Test
    void listTokens_withFilter_queriesByMunicipality() {
        callerIsSecurityAdmin();
        when(tokenRepository.findByMunicipality_IdOrderByIssuedAtDesc(MUNI_ID)).thenReturn(List.of());

        service.listTokens(MUNI_ID);

        verify(tokenRepository).findByMunicipality_IdOrderByIssuedAtDesc(MUNI_ID);
        verify(tokenRepository, never()).findAllByOrderByIssuedAtDesc();
    }
}
