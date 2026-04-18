package za.co.urbaneye.reporthole.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.urbaneye.reporthole.user.entity.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IUserAuthRepository extends JpaRepository<User, UUID> {
    boolean existsDistinctByEmail(String email);

    Optional<User> findByEmailHash(String emailHash);
}
