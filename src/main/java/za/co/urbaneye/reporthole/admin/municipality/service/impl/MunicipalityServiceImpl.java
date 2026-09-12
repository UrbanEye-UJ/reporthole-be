package za.co.urbaneye.reporthole.admin.municipality.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.urbaneye.reporthole.admin.municipality.dto.CreateMunicipalityRequest;
import za.co.urbaneye.reporthole.admin.municipality.dto.IssueTokenRequest;
import za.co.urbaneye.reporthole.admin.municipality.dto.MunicipalityResponse;
import za.co.urbaneye.reporthole.admin.municipality.dto.MunicipalityTokenResponse;
import za.co.urbaneye.reporthole.admin.municipality.entity.Municipality;
import za.co.urbaneye.reporthole.admin.municipality.entity.MunicipalityToken;
import za.co.urbaneye.reporthole.admin.municipality.exception.MunicipalityException;
import za.co.urbaneye.reporthole.admin.municipality.repository.IMunicipalityRepository;
import za.co.urbaneye.reporthole.admin.municipality.repository.IMunicipalityTokenRepository;
import za.co.urbaneye.reporthole.admin.municipality.service.interfaces.IMunicipalityService;
import za.co.urbaneye.reporthole.notification.service.interfaces.IMailService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;

/**
 * Default {@link IMunicipalityService}.
 *
 * <p>Guards every operation with a {@code SECURITY_ADMIN} check (mirrors
 * {@code SecurityAdminServiceImpl} / {@code AdminApplicationServiceImpl}). Token strings are
 * generated from a {@link SecureRandom} over an unambiguous alphabet.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MunicipalityServiceImpl implements IMunicipalityService {

    /** Excludes easily-confused characters (0/O, 1/I). */
    private static final char[] TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int TOKEN_BODY_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final IMunicipalityRepository municipalityRepository;
    private final IMunicipalityTokenRepository tokenRepository;
    private final IUserRepository userRepository;
    private final IMailService mailService;

    @Value("${mail.municipality-token-registration-url}")
    private String registrationUrl;

    @Override
    @Transactional
    public MunicipalityResponse createMunicipality(CreateMunicipalityRequest request) {
        User actor = requireSecurityAdmin();

        String name = request.name().trim();
        if (municipalityRepository.existsByNameIgnoreCase(name)) {
            throw new MunicipalityException("A municipality with that name already exists");
        }

        String province = request.province() == null || request.province().isBlank()
                ? "Gauteng"
                : request.province().trim();

        Municipality saved = municipalityRepository.save(
                Municipality.builder().name(name).province(province).createdBy(actor).build());

        log.info("SECURITY_ADMIN {} created municipality {} ({})", actor.getUserId(), saved.getId(), name);
        return MunicipalityResponse.from(saved, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MunicipalityResponse> listMunicipalities() {
        requireSecurityAdmin();
        return municipalityRepository.findAllByOrderByNameAsc().stream()
                .map(m -> MunicipalityResponse.from(m, tokenRepository.countByMunicipality_Id(m.getId())))
                .toList();
    }

    @Override
    @Transactional
    public MunicipalityTokenResponse issueToken(UUID municipalityId, IssueTokenRequest request) {
        User actor = requireSecurityAdmin();

        Municipality municipality = municipalityRepository.findById(municipalityId)
                .orElseThrow(() -> new MunicipalityException("Municipality not found"));

        LocalDateTime expiresAt = request.expiresInDays() == null
                ? null
                : LocalDateTime.now().plusDays(request.expiresInDays());

        MunicipalityToken saved = tokenRepository.save(MunicipalityToken.builder()
                .token(generateUniqueTokenValue())
                .municipality(municipality)
                .issuedBy(actor)
                .expiresAt(expiresAt)
                .note(request.note())
                .build());

        log.info("SECURITY_ADMIN {} issued token {} for municipality {}",
                actor.getUserId(), saved.getId(), municipalityId);

        if (request.recipientEmail() != null && !request.recipientEmail().isBlank()) {
            String expiresLabel = request.expiresInDays() == null
                    ? "never"
                    : "in " + request.expiresInDays() + " day(s)";
            mailService.sendMunicipalityTokenEmail(
                    request.recipientEmail(), saved.getToken(),
                    municipality.getName(), expiresLabel, registrationUrl);
        }

        return MunicipalityTokenResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MunicipalityTokenResponse> listTokens(UUID municipalityId) {
        requireSecurityAdmin();

        List<MunicipalityToken> tokens = municipalityId == null
                ? tokenRepository.findAllByOrderByIssuedAtDesc()
                : tokenRepository.findByMunicipality_IdOrderByIssuedAtDesc(municipalityId);

        return tokens.stream().map(MunicipalityTokenResponse::from).toList();
    }

    @Override
    @Transactional
    public void revokeToken(UUID tokenId) {
        User actor = requireSecurityAdmin();

        MunicipalityToken token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new MunicipalityException("Token not found"));
        if (token.getRevokedAt() != null) {
            throw new MunicipalityException("Token is already revoked");
        }

        token.setRevokedAt(LocalDateTime.now());
        tokenRepository.save(token);
        log.info("SECURITY_ADMIN {} revoked municipality token {}", actor.getUserId(), tokenId);
    }

    // ------------------------------------------------------------------

    /**
     * Loads the calling account and asserts it holds {@code SECURITY_ADMIN}.
     *
     * @return the caller's {@link User}
     * @throws MunicipalityException if the caller is unknown or not a security admin
     */
    private User requireSecurityAdmin() {
        User caller = userRepository.findById(currentUserId())
                .orElseThrow(() -> new MunicipalityException("User not found"));
        if (caller.getRole() != UserRole.SECURITY_ADMIN) {
            throw new MunicipalityException("Only security admins can perform this action");
        }
        return caller;
    }

    /**
     * @return a {@code MUNI-XXXXXXXXXXXX} token string not already present in the table
     */
    private String generateUniqueTokenValue() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder body = new StringBuilder("MUNI-");
            for (int i = 0; i < TOKEN_BODY_LENGTH; i++) {
                body.append(TOKEN_ALPHABET[RANDOM.nextInt(TOKEN_ALPHABET.length)]);
            }
            String candidate = body.toString();
            if (tokenRepository.findByToken(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new MunicipalityException("Could not generate a unique token, please retry");
    }

    /**
     * @return the authenticated caller's user id from the security context principal
     */
    private UUID currentUserId() {
        return UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }
}
