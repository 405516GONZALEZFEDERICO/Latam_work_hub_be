package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.entities.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestEntityManager entityManager;
    UserEntity userEntity;
    @BeforeEach
    public void setUp() {
         userEntity=new UserEntity();
        userEntity.setEmail("fede@fede.com");
        userEntity.setName("Federico Gonzalez");
        userEntity.setFirebaseUid("asd123456");
        entityManager.persist(userEntity);
    }
    @Test
    void findByEmail() {
        Optional<UserEntity> user=userRepository.findByEmail("fede@fede.com");
        assert user.isPresent();
        assertEquals(user.get().getEmail(),"fede@fede.com");
    }

    @Test
    void findByFirebaseUid() {
        Optional<UserEntity> user=userRepository.findByFirebaseUid("asd123456");
        assert user.isPresent();
        assertEquals(user.get().getFirebaseUid(),"asd123456");
    }
}