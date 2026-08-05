package za.co.urbaneye.reporthole.admin.contractor.service.interfaces;

import za.co.urbaneye.reporthole.admin.contractor.dto.ContractorResponse;
import za.co.urbaneye.reporthole.admin.contractor.dto.CreateContractorRequest;

import java.util.List;

public interface IContractorService {

    /** Creates an active CONTRACTOR account. Caller must be an ADMIN. */
    ContractorResponse createContractor(CreateContractorRequest request);

    /** Returns every CONTRACTOR account with its current active-job count. Caller must be an ADMIN. */
    List<ContractorResponse> getContractors();
}
