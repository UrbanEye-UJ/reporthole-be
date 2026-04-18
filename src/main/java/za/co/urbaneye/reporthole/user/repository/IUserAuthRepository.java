package za.co.urbaneye.reporthole.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.urbaneye.reporthole.user.entity.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for managing {@link User} entities.
 *
 * <p>This repository provides standard CRUD operations through
 * {@link JpaRepository} as well as custom query methods for
 * authentication and user lookup functionality.</p>
 *
 * <p>Managed entity:</p>
 * <ul>
 *     <li>{@link User}</li>
 * </ul>
 *
 * <p>Primary key type:</p>
 * <ul>
 *     <li>{@link UUID}</li>
 * </ul>
 *
 * @author Refentse
 * @since 1.0
 */
@Repository
public interface IUserAuthRepository extends JpaRepository<User, UUID> {

    /**
     * Checks whether a user exists with the given email address.
     *
     * <p>Typically used during registration to prevent
     * duplicate accounts.</p>
     *
     * @param email user email address
     * @return {@code true} if a matching user exists,
     * otherwise {@code false}
     */
    boolean existsDistinctByEmail(String email);

    /**
     * Finds a user by the stored email hash value.
     *
     * <p>Used for privacy-preserving lookups where the
     * plaintext email is not directly queried.</p>
     *
     * @param emailHash hashed email value
     * @return optional containing the matching user if found,
     * otherwise empty
     */
    Optional<User> findByEmailHash(String emailHash);
}