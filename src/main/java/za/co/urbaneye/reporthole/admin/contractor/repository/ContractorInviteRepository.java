package za.co.urbaneye.reporthole.admin.contractor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.urbaneye.reporthole.admin.contractor.entity.ContractorInvite;

import java.util.Optional;
import java.util.UUID;

public interface ContractorInviteRepository extends JpaRepository<ContractorInvite, UUID> {

    Optional<ContractorInvite> findByToken(UUID token);

    boolean existsByEmailHash(String emailHash);
}
