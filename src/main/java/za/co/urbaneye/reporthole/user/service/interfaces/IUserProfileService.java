package za.co.urbaneye.reporthole.user.service.interfaces;

import za.co.urbaneye.reporthole.user.dto.UpdateProfileRequest;
import za.co.urbaneye.reporthole.user.dto.UserProfileResponse;

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
}
