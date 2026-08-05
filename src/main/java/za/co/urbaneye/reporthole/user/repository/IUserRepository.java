package za.co.urbaneye.reporthole.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserRole;

import java.util.List;
import java.util.UUID;

public interface IUserRepository extends JpaRepository<User, UUID> {

    List<User> findByRole(UserRole role);
}
