package za.co.urbaneye.reporthole.user.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.user.dto.UpdateProfileRequest;
import za.co.urbaneye.reporthole.user.dto.UserProfileResponse;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.entity.UserStatus;
import za.co.urbaneye.reporthole.user.exception.UserServiceException;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;
import za.co.urbaneye.reporthole.user.service.interfaces.IUserProfileService;

import java.util.Set;
import java.util.UUID;

/**
 * Service implementation for user profile management.
 *
 * <p>User and UserAuth share the same UUID primary key, so a single
 * {@code findById} resolves both. Email is read from UserAuth (where it
 * is stored encrypted); name and phone are read from User.</p>
 */
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements IUserProfileService {

    private final IUserRepository userRepository;
    private final IUserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfileResponse getProfile() {
        UUID userId = currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserServiceException("User not found"));
        UserAuth auth = userAuthRepository.findById(userId)
                .orElseThrow(() -> new UserServiceException("User not found"));
        return toResponse(user, auth);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        UUID userId = currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserServiceException("User not found"));
        UserAuth auth = userAuthRepository.findById(userId)
                .orElseThrow(() -> new UserServiceException("User not found"));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhoneNumber(request.phoneNumber());
        userRepository.save(user);
        return toResponse(user, auth);
    }

    @Override
    @Transactional
    public void deleteAccount() {
        UUID userId = currentUserId();
        UserAuth auth = userAuthRepository.findById(userId)
                .orElseThrow(() -> new UserServiceException("User not found"));
        auth.setStatus(UserStatus.DELETED);
        userAuthRepository.save(auth);
    }

    @Override
    @Transactional
    public UserProfileResponse updateSpecialisations(Set<IssueType> specialisations) {
        UUID userId = currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserServiceException("User not found"));
        UserAuth auth = userAuthRepository.findById(userId)
                .orElseThrow(() -> new UserServiceException("User not found"));
        if (user.getRole() != UserRole.CONTRACTOR) {
            throw new UserServiceException("Only contractors can update specialisations");
        }
        user.setSpecialisations(specialisations);
        userRepository.save(user);
        return toResponse(user, auth);
    }

    private UserProfileResponse toResponse(User user, UserAuth auth) {
        String municipalityName = user.getMunicipality() != null
                ? user.getMunicipality().getName()
                : null;
        Set<IssueType> specialisations = user.getRole() == UserRole.CONTRACTOR
                ? user.getSpecialisations()
                : null;
        return new UserProfileResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                auth.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                municipalityName,
                user.getCreatedAt(),
                specialisations
        );
    }

    @Override
    public void verifyPassword(String password) {
        UUID userId = currentUserId();
        UserAuth auth = userAuthRepository.findById(userId)
                .orElseThrow(() -> new UserServiceException("User not found"));
        if (!passwordEncoder.matches(password, auth.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect password");
        }
    }

    private UUID currentUserId() {
        return UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        );
    }
}
