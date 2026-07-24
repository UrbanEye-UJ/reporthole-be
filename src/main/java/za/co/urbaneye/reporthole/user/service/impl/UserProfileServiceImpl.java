package za.co.urbaneye.reporthole.user.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.user.dto.UpdateProfileRequest;
import za.co.urbaneye.reporthole.user.dto.UserProfileResponse;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserStatus;
import za.co.urbaneye.reporthole.user.exception.UserServiceException;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;
import za.co.urbaneye.reporthole.user.service.interfaces.IUserProfileService;

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

    private UserProfileResponse toResponse(User user, UserAuth auth) {
        return new UserProfileResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                auth.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    private UUID currentUserId() {
        return UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        );
    }
}
