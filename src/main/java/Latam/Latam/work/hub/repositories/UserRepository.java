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
    Optional<UserEntity>getUserRoleByEmail(String email);

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.enabled = true AND u.role.name = :roleName")
    long countActiveUsersByRole(@Param("roleName") String roleName);

    @Query("SELECT u FROM UserEntity u WHERE " +
           "(:enabled IS NULL OR u.enabled = :enabled) AND " +
           "(:roleName IS NULL OR u.role.name = :roleName)")
    Page<UserEntity> findByEnabledAndRoleName(
            @Param("enabled") Boolean enabled, 
            @Param("roleName") String roleName, 
            Pageable pageable);


    // Query para obtener usuarios paginados con filtros básicos
    @Query("SELECT u FROM UserEntity u JOIN FETCH u.role r " + // JOIN FETCH para el rol
            "WHERE (:userStatusBoolean IS NULL OR u.enabled = :userStatusBoolean) " +
            "AND (:roleNameParam IS NULL OR r.name = :roleNameParam)")
    Page<UserEntity> findUsersForReportPage(
            @Param("userStatusBoolean") Boolean userStatusBoolean,
            @Param("roleNameParam") String roleNameParam,
            Pageable pageable
    );

    // Contar espacios por proveedor (para un conjunto de IDs de proveedores)
    @Query("SELECT s.owner.id AS providerId, COUNT(s.id) AS count " +
            "FROM SpaceEntity s " +
            "WHERE s.owner.id IN :providerIds AND s.active = true AND s.deleted = false " +
            "GROUP BY s.owner.id")
    List<Object[]> countSpacesByProviderIds(@Param("providerIds") List<Long> providerIds);

    // Contar bookings por cliente (para un conjunto de IDs de clientes)
    @Query("SELECT b.user.id AS clientId, COUNT(b.id) AS count " +
            "FROM BookingEntity b " +
            "WHERE b.user.id IN :clientIds " + // No hay filtro de 'active' en BookingEntity
            "GROUP BY b.user.id")
    List<Object[]> countBookingsByClientIds(@Param("clientIds") List<Long> clientIds);

    // Sumar ingresos para proveedores (para un conjunto de IDs de proveedores)
    @Query("SELECT sp.owner.id AS providerId, SUM(inv.totalAmount) AS totalRevenue " +
            "FROM InvoiceEntity inv " +
            "JOIN inv.booking book JOIN book.space sp " +
            "WHERE sp.owner.id IN :providerIds AND inv.status = :paidStatus " +
            "GROUP BY sp.owner.id")
    List<Object[]> sumRevenueForProvidersFromBookings(@Param("providerIds") List<Long> providerIds, @Param("paidStatus") InvoiceStatus paidStatus);

    @Query("SELECT spc.owner.id AS providerId, SUM(inv.totalAmount) AS totalRevenue " +
            "FROM InvoiceEntity inv " +
            "JOIN inv.rentalContract rc JOIN rc.space spc " +
            "WHERE spc.owner.id IN :providerIds AND inv.status = :paidStatus " +
            "GROUP BY spc.owner.id")
    List<Object[]> sumRevenueForProvidersFromContracts(@Param("providerIds") List<Long> providerIds, @Param("paidStatus") InvoiceStatus paidStatus);
}

