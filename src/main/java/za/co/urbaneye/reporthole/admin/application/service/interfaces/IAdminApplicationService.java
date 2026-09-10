package za.co.urbaneye.reporthole.admin.application.service.interfaces;

import za.co.urbaneye.reporthole.admin.application.dto.AdminApplicationRequest;

import java.util.UUID;

/**
 * Contract for the admin application submission flow.
 *
 * @author Refentse
 * @since 1.0
 */
public interface IAdminApplicationService {

    /**
     * Submits an admin access application for the currently authenticated CIVILIAN user.
     *
     * <p>Validates that the user exists, holds the CIVILIAN role, and has not
     * previously submitted an application. On success, persists the application
     * and sends an async notification email to the project inbox.</p>
     *
     * @param request DTO containing the municipality token
     * @throws za.co.urbaneye.reporthole.admin.application.exception.AdminApplicationException
     *         if the user is not found, is not a CIVILIAN, or has already applied
     */
    void apply(AdminApplicationRequest request);

    /**
     * Approves a pending application without requiring the caller to hold an active session.
     *
     * <p>Intended for bootstrap flows (e.g. a developer approving the very first admin
     * before any admin account exists). Promotes the applicant to {@code ADMIN} and
     * marks the application {@code APPROVED}.</p>
     *
     * @param applicationId the UUID of the application to approve
     * @throws za.co.urbaneye.reporthole.admin.application.exception.AdminApplicationException
     *         if the application is not found or has already been approved
     */
    void approveOpen(UUID applicationId);
}
