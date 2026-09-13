package za.co.urbaneye.reporthole.user.service.interfaces;

import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.user.dto.UpdateProfileRequest;
import za.co.urbaneye.reporthole.user.dto.UserProfileResponse;

import java.util.Set;

/**
 * Service contract for reading, updating, and soft-deleting the authenticated user's profile.
 */
public interface IUserProfileService {

    /** Returns the profile of the currently authenticated user. */
    UserProfileResponse getProfile();

    /** Updates the authenticated user's first name, last name, and phone number. */
    UserProfileResponse updateProfile(UpdateProfileRequest request);

    /** Soft-deletes the authenticated user's account by setting status to DELETED. */
    void deleteAccount();

    /**
     * Verifies that {@code password} matches the authenticated user's stored hash.
     * Throws {@link org.springframework.web.server.ResponseStatusException} 401 if it does not.
     */
    void verifyPassword(String password);

    /**
     * Replaces the authenticated contractor's specialisation set.
     * Throws {@link za.co.urbaneye.reporthole.user.exception.UserServiceException} if the caller is not a CONTRACTOR.
     */
    UserProfileResponse updateSpecialisations(Set<IssueType> specialisations);
}
