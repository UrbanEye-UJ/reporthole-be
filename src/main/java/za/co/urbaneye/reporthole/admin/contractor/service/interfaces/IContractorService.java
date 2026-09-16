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
     * Returns every CONTRACTOR account with its current active-job count. Caller must be an ADMIN
     * or SECURITY_ADMIN. An ADMIN always sees only their own municipality; a SECURITY_ADMIN sees
     * every contractor platform-wide, or just one municipality's when {@code municipalityId} is given.
     * Emails and phone numbers are masked (e.g. {@code "jo***@example.com"}, {@code "082***890"}) —
     * use {@link #revealEmail} to view them in full.
     *
     * @param municipalityId optional filter, honoured only for SECURITY_ADMIN callers
     */
    List<ContractorResponse> getContractors(UUID municipalityId);

    /**
     * Returns a contractor's decrypted email and phone number after verifying the calling
     * admin's own current password as a step-up re-authentication check. Caller must be an ADMIN.
     *
     * @param contractorId the contractor whose details should be revealed
     * @param password     the calling admin's own current password
     */
    RevealEmailResponse revealEmail(UUID contractorId, String password);
}
