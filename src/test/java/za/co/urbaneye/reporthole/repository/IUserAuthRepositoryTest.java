package za.co.urbaneye.reporthole.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import za.co.urbaneye.reporthole.security.SecretUtil;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserStatus;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class IUserAuthRepositoryTest {

    @Autowired
    private IUserAuthRepository repository;

    @Test
    void shouldFindByEmailHash() {

        UserAuth user = UserAuth.builder()
                .email("john@mail.com")
                .emailHash(SecretUtil.hashEmail("john@mail.com"))
                .password("123")
                .status(UserStatus.ACTIVE)
                .retries(0)
                .build();

        repository.save(user);

        Optional<UserAuth> found =
                repository.findByEmailHash(SecretUtil.hashEmail("john@mail.com"));

        assertTrue(found.isPresent());
    }

    @Test
    void shouldReturnFalseIfEmailDoesNotExist() {
        assertFalse(repository.existsDistinctByEmail("none@mail.com"));
    }
}