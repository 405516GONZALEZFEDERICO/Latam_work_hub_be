package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.entities.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByFirebaseUid(String firebaseUid);

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.role.name = :roleName AND u.enabled = true")
    long countActiveUsersByRole(@Param("roleName") String roleName);

    @Query("SELECT COUNT(u) > 0 FROM UserEntity u JOIN u.role r " +
            "WHERE u.email = :email AND r.name = :roleName")
    boolean existsByEmailAndRole(@Param("email") String email, @Param("roleName") String roleName);

    @Query("SELECT u FROM UserEntity u LEFT JOIN u.role r WHERE " + // LEFT JOIN por si un usuario pudiese no tener rol
            "(:roleName IS NULL OR r.name = :roleName) AND " +
            "(:isEnabled IS NULL OR u.enabled = :isEnabled) AND " + // Filtro por estado (enabled)
            "(:startDate IS NULL OR u.registrationDate >= :startDate)")
    Page<UserEntity> findUsersForReport(
            @Param("roleName") String roleName,
            @Param("isEnabled") Boolean isEnabled, // Nuevo parámetro
            @Param("startDate") LocalDateTime startDate,
            Pageable pageable
    );
    @Query("SELECT u FROM UserEntity u LEFT JOIN u.role r WHERE " +
            "(:roleName IS NULL OR r.name = :roleName) and u.enabled = true or u.enabled    = false")
    List<UserEntity> findUsersEnabledByRole(
            @Param("roleName") String roleName
    );
}

