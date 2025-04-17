package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.entities.RoleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

@ActiveProfiles("test")
@DataJpaTest
class RoleRepositoryTest {
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private TestEntityManager entityManager;


    @BeforeEach
    public void setUp() {
        RoleEntity roleEntity=new RoleEntity();
        roleEntity = new RoleEntity();
        roleEntity.setName("ADMIN");
        entityManager.persist(roleEntity);
    }
    @Test
    void findByName() {
        String name= "ADMIN";
        Optional<RoleEntity> roleEntity = roleRepository.findByName(name);
        assert roleEntity.isPresent();
    }
}