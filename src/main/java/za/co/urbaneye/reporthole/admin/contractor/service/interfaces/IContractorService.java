package za.co.urbaneye.reporthole.admin.contractor.service.interfaces;

import za.co.urbaneye.reporthole.admin.contractor.dto.CompleteContractorRegistrationRequest;
import za.co.urbaneye.reporthole.admin.contractor.dto.ContractorResponse;
import za.co.urbaneye.reporthole.admin.contractor.dto.InviteContractorRequest;
import za.co.urbaneye.reporthole.admin.contractor.dto.RevealEmailResponse;

import java.util.List;
import java.util.UUID;

public interface IContractorService {

    /**
     * Sends an invite email to the given address so the contractor can self-register.
     * Caller must be an ADMIN.
     */
    void inviteContractor(InviteContractorRequest request);

    /**
     * Completes contractor registration using the token from the invite email.
     * Creates the {@code UserAuth} and {@code User} records and marks the invite as used.
     * Public — no authentication required.
     *
     * @param request token + personal details + chosen password
     * @return the newly created contractor's profile
     */
    ContractorResponse completeContractorRegistration(CompleteContractorRegistrationRequest request);

    /**
     * Returns every CONTRACTOR account with its current active-job count. Caller must be an ADMIN.
     * Emails are masked (e.g. {@code "jo***@example.com"}) — use {@link #revealEmail} to view one in full.
     */
    List<ContractorResponse> getContractors();

    /**
     * Returns a contractor's decrypted email after verifying the calling admin's own
     * current password as a step-up re-authentication check. Caller must be an ADMIN.
     *
     * @param contractorId the contractor whose email should be revealed
     * @param password     the calling admin's own current password
     */
    RevealEmailResponse revealEmail(UUID contractorId, String password);
}
