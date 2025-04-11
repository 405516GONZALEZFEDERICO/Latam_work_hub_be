package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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

    /**
     * Verifica si existe un usuario con el email especificado
     */
    boolean existsByEmail(String email);

    /**
     * Verifica si existe un usuario con el UID de Firebase especificado
     */
    boolean existsByFirebaseUid(String firebaseUid);

    /**
     * Busca usuarios por rol
     */
    @Query("SELECT u FROM UserEntity u JOIN u.role r WHERE r.name = :roleName")
    List<UserEntity> findByRoleName(@Param("roleName") String roleName);

    /**
     * Busca usuarios que se registraron después de una fecha determinada
     */
    List<UserEntity> findByRegistrationDateAfter(LocalDateTime date);

    /**
     * Busca usuarios activos (enabled = true)
     */
    List<UserEntity> findByEnabledTrue();

    /**
     * Busca usuarios inactivos (enabled = false)
     */
    List<UserEntity> findByEnabledFalse();

    /**
     * Busca usuarios por compañía
     */
    @Query("SELECT u FROM UserEntity u WHERE u.company.id = :companyId")
    List<UserEntity> findByCompanyId(@Param("companyId") Long companyId);

    /**
     * Busca usuarios por tipo de documento y número de documento
     */
    List<UserEntity> findByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);

    /**
     * Busca usuarios por departamento
     */
    List<UserEntity> findByDepartment(String department);

    /**
     * Busca usuarios por título de trabajo (job title)
     */
    List<UserEntity> findByJobTitle(String jobTitle);

    /**
     * Busca usuarios que coincidan parcialmente con un nombre
     */
    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<UserEntity> findByNameContainingIgnoreCase(@Param("name") String name);
}