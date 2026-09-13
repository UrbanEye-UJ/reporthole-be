package za.co.urbaneye.reporthole.admin.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.urbaneye.reporthole.admin.user.dto.CivilianSummaryResponse;
import za.co.urbaneye.reporthole.admin.user.service.interfaces.IAdminUserService;
import za.co.urbaneye.reporthole.incident.repository.IncidentReporterRepository;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.entity.UserStatus;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Default {@link IAdminUserService}.
 *
 * <p>Names and emails are decrypted (the {@link za.co.urbaneye.reporthole.security.Aes} converter
 * handles that transparently) and then re-masked before leaving the service layer — admins see
 * just enough to identify a civilian without receiving their full PII.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserServiceImpl implements IAdminUserService {

    private final IUserRepository userRepository;
    private final IUserAuthRepository userAuthRepository;
    private final IncidentReporterRepository incidentReporterRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CivilianSummaryResponse> getCivilians() {
        List<User> civilians = userRepository.findByRole(UserRole.CIVILIAN);

        Map<UUID, UserAuth> authById = userAuthRepository.findAll().stream()
                .collect(Collectors.toMap(UserAuth::getAuthId, Function.identity()));

        return civilians.stream()
                .map(user -> {
                    UserAuth auth = authById.get(user.getUserId());
                    long incidentCount = incidentReporterRepository.countByUser_UserId(user.getUserId());
                    return new CivilianSummaryResponse(
                            user.getUserId(),
                            maskName(user.getFirstName(), user.getLastName()),
                            auth != null ? maskEmail(auth.getEmail()) : "—",
                            incidentCount,
                            auth != null ? auth.getStatus() : UserStatus.ACTIVE,
                            user.getCreatedAt()
                    );
                })
                .sorted((a, b) -> {
                    if (a.createdAt() == null || b.createdAt() == null) return 0;
                    return b.createdAt().compareTo(a.createdAt());
                })
                .toList();
    }

    /**
     * Returns {@code "FirstName L."} from a first and last name pair, keeping only the last initial.
     * Guards against blank/null input gracefully.
     *
     * @param firstName decrypted first name
     * @param lastName  decrypted last name
     * @return masked display name
     */
    public static String maskName(String firstName, String lastName) {
        String first = firstName != null ? firstName.trim() : "";
        String last  = lastName  != null ? lastName.trim()  : "";
        if (first.isEmpty() && last.isEmpty()) return "—";
        if (last.isEmpty()) return first;
        return first + " " + last.charAt(0) + ".";
    }

    /**
     * Returns {@code "x***@domain"} from a full email address, hiding the local-part after the
     * first character. Falls back to {@code "—"} for null or malformed addresses.
     *
     * @param email decrypted email address
     * @return masked email string
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "—";
        int at = email.indexOf('@');
        if (at == 0) return "***" + email.substring(at);
        return email.charAt(0) + "***" + email.substring(at);
    }
}
