package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    /**
     * Busca un usuario por su email
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Busca un usuario por su identificador de Firebase
     */
    Optional<UserEntity> findByFirebaseUid(String firebaseUid);

}