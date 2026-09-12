package za.co.urbaneye.reporthole.admin.application.service.interfaces;

import za.co.urbaneye.reporthole.admin.application.dto.AdminApplicationRequest;
import za.co.urbaneye.reporthole.admin.application.dto.AdminApplicationResponse;
import za.co.urbaneye.reporthole.admin.application.entity.AdminApplicationStatus;

import java.util.List;
import java.util.UUID;

/**
 * Contract for the admin application submission and review flow.
 *
 * <p>Submission is open to any {@code CIVILIAN}. Review — listing, approving, rejecting — is
 * restricted to {@code SECURITY_ADMIN}, because approving an application grants the
 * {@code ADMIN} role and role grants are the security admin's responsibility.</p>
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
     * Returns admin-access records for the reviewing security admin.
     *
     * @param status optional filter; {@code null} returns every record (PENDING, APPROVED and
     *               REJECTED), most recently submitted first
     * @return the matching records
     * @throws za.co.urbaneye.reporthole.admin.application.exception.AdminApplicationException
     *         if the caller is not a SECURITY_ADMIN
     */
    List<AdminApplicationResponse> listApplications(AdminApplicationStatus status);

    /**
     * Approves a pending application: promotes the applicant to {@code ADMIN}, marks the
     * application {@code APPROVED}, invalidates the applicant's existing sessions, writes an
     * append-only access-control audit row, and sends a decision email to the applicant.
     *
     * @param applicationId the application to approve
     * @throws za.co.urbaneye.reporthole.admin.application.exception.AdminApplicationException
     *         if the caller is not a SECURITY_ADMIN, the application is not found, or it is not PENDING
     */
    void approve(UUID applicationId);

    /**
     * Rejects a pending application, marking it {@code REJECTED} without changing the
     * applicant's role. Sends a decision email to the applicant.
     *
     * @param applicationId the application to reject
     * @throws za.co.urbaneye.reporthole.admin.application.exception.AdminApplicationException
     *         if the caller is not a SECURITY_ADMIN, the application is not found, or it is not PENDING
     */
    void reject(UUID applicationId);
}
