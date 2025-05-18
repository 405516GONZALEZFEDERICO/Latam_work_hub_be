package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.dtos.common.reports.admin.UserReportProjection;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByFirebaseUid(String firebaseUid);

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.enabled = true AND u.role.name = :roleName")
    long countActiveUsersByRole(@Param("roleName") String roleName);




    @Query("SELECT COUNT(u) > 0 FROM UserEntity u JOIN u.role r " +
            "WHERE u.email = :email AND r.name = :roleName")
    boolean existsByEmailAndRole(@Param("email") String email, @Param("roleName") String roleName);

}

