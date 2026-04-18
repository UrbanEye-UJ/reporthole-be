package za.co.urbaneye.reporthole.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import za.co.urbaneye.reporthole.security.SecretUtil;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class IUserAuthRepositoryTest {

    @Autowired
    private IUserAuthRepository repository;

    @Test
    void shouldFindByEmailHash() {

        User user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@mail.com")
                .emailHash(SecretUtil.hashEmail("john@mail.com"))
                .password("123")
                .role(UserRole.CIVILIAN)
                .build();

        repository.save(user);

        Optional<User> found =
                repository.findByEmailHash(SecretUtil.hashEmail("john@mail.com"));

        assertTrue(found.isPresent());
    }

    @Test
    void shouldReturnFalseIfEmailDoesNotExist() {
        assertFalse(repository.existsDistinctByEmail("none@mail.com"));
    }
}