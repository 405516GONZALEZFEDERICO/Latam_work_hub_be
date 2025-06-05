package Latam.Latam.work.hub.repositories;
import Latam.Latam.work.hub.entities.SpaceEntity;
import Latam.Latam.work.hub.enums.BookingStatus;
import Latam.Latam.work.hub.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SpaceRepository extends JpaRepository<SpaceEntity, Long> {
    @Query("SELECT DISTINCT s FROM SpaceEntity s " +
            "LEFT JOIN s.images i " +
            "LEFT JOIN s.type t " +
            "LEFT JOIN s.address a " +
            "LEFT JOIN a.city c " +
            "LEFT JOIN c.country " +
            "LEFT JOIN s.amenities am " +
            "WHERE s.active = true AND s.available = true " +
            "AND s.deleted = false " +
            "AND (:pricePerHour IS NULL OR s.pricePerHour <= :pricePerHour) " +
            "AND (:pricePerDay IS NULL OR s.pricePerDay <= :pricePerDay) " +
            "AND (:pricePerMonth IS NULL OR s.pricePerMonth <= :pricePerMonth) " +
            "AND (:area IS NULL OR s.area <= :area) " +
            "AND (:capacity IS NULL OR s.capacity <= :capacity) " +
            "AND (:spaceTypeId IS NULL OR t.id <= :spaceTypeId) " +
            "AND (:cityId IS NULL OR c.id = :cityId) " +
            "AND (:countryId IS NULL OR c.country.id = :countryId) " +
            "AND (:amenityIds IS NULL OR " +
            "   (SELECT COUNT(DISTINCT a.id) FROM SpaceEntity sp " +
            "    JOIN sp.amenities a " +
            "    WHERE sp = s AND a.id IN :amenityIds) = " +
            "   (CASE WHEN :amenityIds IS NULL THEN 0 ELSE (SELECT COUNT(DISTINCT ae.id) FROM AmenityEntity ae WHERE ae.id IN :amenityIds) END))")
    Page<SpaceEntity> findActiveAvailableSpacesWithoutAmenityFilters(
            @Param("pricePerHour") Double pricePerHour,
            @Param("pricePerDay") Double pricePerDay,
            @Param("pricePerMonth") Double pricePerMonth,
            @Param("area") Double area,
            @Param("capacity") Integer capacity,
            @Param("spaceTypeId") Long spaceTypeId,
            @Param("cityId") Long cityId,
            @Param("countryId") Long countryId,
            @Param("amenityIds") List<Long> amenityIds,
            Pageable pageable
    );
    @Query("SELECT DISTINCT s FROM SpaceEntity s " +
            "LEFT JOIN s.images i " +
            "LEFT JOIN s.type t " +
            "LEFT JOIN s.address a " +
            "LEFT JOIN a.city c " +
            "LEFT JOIN c.country " +
            "LEFT JOIN s.amenities am " +
            "WHERE s.owner.firebaseUid = :uid " +
            "AND s.deleted = false " +
            "AND (:pricePerHour IS NULL OR s.pricePerHour >= :pricePerHour) " +
            "AND (:pricePerDay IS NULL OR s.pricePerDay >= :pricePerDay) " +
            "AND (:pricePerMonth IS NULL OR s.pricePerMonth >= :pricePerMonth) " +
            "AND (:area IS NULL OR s.area >= :area) " +
            "AND (:capacity IS NULL OR s.capacity >= :capacity) " +
            "AND (:spaceTypeId IS NULL OR t.id >= :spaceTypeId) " +
            "AND (:cityId IS NULL OR c.id = :cityId) " +
            "AND (:countryId IS NULL OR c.country.id = :countryId) " +
            "AND (:amenityIds IS NULL OR " +
            "   (SELECT COUNT(DISTINCT a.id) FROM SpaceEntity sp " +
            "    JOIN sp.amenities a " +
            "    WHERE sp = s AND a.id IN :amenityIds) = " +
            "   (CASE WHEN :amenityIds IS NULL THEN 0 ELSE (SELECT COUNT(DISTINCT ae.id) FROM AmenityEntity ae WHERE ae.id IN :amenityIds) END))")
    Page<SpaceEntity> findSpacesByOwnerUid(
            @Param("uid") String uid,
            @Param("pricePerHour") Double pricePerHour,
            @Param("pricePerDay") Double pricePerDay,
            @Param("pricePerMonth") Double pricePerMonth,
            @Param("area") Double area,
            @Param("capacity") Integer capacity,
            @Param("spaceTypeId") Long spaceTypeId,
            @Param("cityId") Long cityId,
            @Param("countryId") Long countryId,
            @Param("amenityIds") List<Long> amenityIds,
            Pageable pageable
    );



    // QUERY CORREGIDA - Maneja correctamente cuando spaceStatusParam es NULL o vacío
    @Query(value = "SELECT s FROM SpaceEntity s JOIN FETCH s.owner ow " +
            "WHERE ((:spaceStatusParam IS NULL OR :spaceStatusParam = '') " +
            "   OR (:spaceStatusParam = 'Disponible' AND s.active = true AND s.available = true AND s.deleted = false) " +
            "   OR (:spaceStatusParam = 'Ocupado' AND s.active = true AND s.available = false AND s.deleted = false) " +
            "   OR (:spaceStatusParam = 'Activo' AND s.active = true AND s.deleted = false) " +
            "   OR (:spaceStatusParam = 'Inactivo' AND s.active = false AND s.deleted = false)" +
            ")",
            countQuery = "SELECT COUNT(s) FROM SpaceEntity s JOIN s.owner ow " +
                    "WHERE ((:spaceStatusParam IS NULL OR :spaceStatusParam = '') " +
                    "   OR (:spaceStatusParam = 'Disponible' AND s.active = true AND s.available = true AND s.deleted = false) " +
                    "   OR (:spaceStatusParam = 'Ocupado' AND s.active = true AND s.available = false AND s.deleted = false) " +
                    "   OR (:spaceStatusParam = 'Activo' AND s.active = true AND s.deleted = false) " +
                    "   OR (:spaceStatusParam = 'Inactivo' AND s.active = false AND s.deleted = false)" +
                    ")")
    Page<SpaceEntity> findSpacesForReportPage(
            @Param("spaceStatusParam") String spaceStatusParam,
            Pageable pageable
    );

    @Query("SELECT COUNT(s) FROM SpaceEntity s WHERE s.owner.id = :ownerId AND s.deleted = false")
    Long countByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT COUNT(s) FROM SpaceEntity s WHERE s.active = true AND s.deleted = false")
    long countPublishedSpaces();

    @Query("SELECT s FROM SpaceEntity s WHERE s.active = true or s.active = false ")
    List<SpaceEntity> findAllActiveSpaces();

    @Query("SELECT s.name, COUNT(DISTINCT rc.id) AS rentalCount, COUNT(DISTINCT b.id) AS reservationCount " +
            "FROM SpaceEntity s " +
            "LEFT JOIN s.rentalContracts rc " + // Asegúrate de que este es el nombre de la relación
            "LEFT JOIN s.bookings b " +         // Asegúrate de que este es el nombre de la relación
            "WHERE s.deleted = false " +
            "GROUP BY s.id, s.name " +
            "ORDER BY rentalCount DESC, reservationCount DESC")
    List<Object[]> findTop5SpacesByRentalsAndReservations(Pageable pageable);

    // ===== MÉTODOS PARA DASHBOARD PROVEEDOR =====
    
    /**
     * Cuenta espacios ocupados de un proveedor (que tienen contratos activos o reservas activas)
     */
    @Query("SELECT COUNT(DISTINCT s.id) FROM SpaceEntity s " +
           "WHERE s.owner.id = :ownerId " +
           "AND s.deleted = false " +
           "AND (s.available = false " +
           "     OR EXISTS (SELECT 1 FROM RentalContractEntity rc WHERE rc.space = s AND rc.contractStatus = 'ACTIVE') " +
           "     OR EXISTS (SELECT 1 FROM BookingEntity b WHERE b.space = s AND b.status IN ('CONFIRMED', 'ACTIVE')))")
    long countOccupiedSpacesByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Obtiene el rendimiento de espacios de un proveedor
     */
    @Query("SELECT s.name, " +
           "COUNT(DISTINCT b.id) as totalBookings, " +
           "COUNT(DISTINCT rc.id) as totalContracts, " +
           "COALESCE(SUM(CASE WHEN b.status != 'CANCELED' THEN b.totalAmount ELSE 0 END), 0) + " +
           "COALESCE(SUM(CASE WHEN rc.contractStatus = 'ACTIVE' THEN rc.monthlyAmount ELSE 0 END), 0) as totalRevenue, " +
           "CASE WHEN COUNT(DISTINCT b.id) + COUNT(DISTINCT rc.id) > 0 THEN " +
           "     (COUNT(DISTINCT CASE WHEN b.status IN ('CONFIRMED', 'ACTIVE', 'COMPLETED') THEN b.id END) + " +
           "      COUNT(DISTINCT CASE WHEN rc.contractStatus = 'ACTIVE' THEN rc.id END)) * 100.0 / " +
           "     (COUNT(DISTINCT b.id) + COUNT(DISTINCT rc.id)) " +
           "ELSE 0 END as occupancyRate " +
           "FROM SpaceEntity s " +
           "LEFT JOIN s.bookings b " +
           "LEFT JOIN s.rentalContracts rc " +
           "WHERE s.owner.id = :providerId " +
           "AND s.deleted = false " +
           "GROUP BY s.id, s.name " +
           "ORDER BY totalRevenue DESC")
    List<Object[]> findSpacePerformanceByProvider(@Param("providerId") Long providerId);

}