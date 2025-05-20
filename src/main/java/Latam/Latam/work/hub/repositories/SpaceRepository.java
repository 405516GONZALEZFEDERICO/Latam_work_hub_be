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

//    @Query("SELECT COUNT(s) FROM SpaceEntity s WHERE s.active = true AND s.available = true AND s.deleted = false")
//    long countByActiveTrueAndAvailableTrueAndDeletedFalse();

    @Query(value = "SELECT s " +
            "FROM SpaceEntity s JOIN FETCH s.owner ow " +
            "WHERE s.deleted = false " +
            "AND (:providerId IS NULL OR ow.id = :providerId) " +
            "AND (:spaceStatusParam IS NULL " +
            "   OR (:spaceStatusParam = 'Disponible' AND s.active = true AND s.available = true AND s.deleted = false) " +
            "   OR (:spaceStatusParam = 'Ocupado' AND s.active = true AND s.available = false AND s.deleted = false) " +
            "   OR (:spaceStatusParam = 'Inactivo' AND s.active = false)" +
            ")",
            countQuery = "SELECT COUNT(s) FROM SpaceEntity s JOIN s.owner ow " +
                    "WHERE s.deleted = false " +
                    "AND (:providerId IS NULL OR ow.id = :providerId) " +
                    "AND (:spaceStatusParam IS NULL " +
                    "   OR (:spaceStatusParam = 'Disponible' AND s.active = true AND s.available = true AND s.deleted = false) " +
                    "   OR (:spaceStatusParam = 'Ocupado' AND s.active = true AND s.available = false AND s.deleted = false) " +
                    "   OR (:spaceStatusParam = 'Inactivo' AND s.active = false)" +
                    ")")
    Page<SpaceEntity> findSpacesForReportPage(
            @Param("providerId") Long providerId,
            @Param("spaceStatusParam") String spaceStatusParam,
            Pageable pageable
    );


    @Query("SELECT COUNT(s) FROM SpaceEntity s WHERE s.owner.id = :ownerId AND s.deleted = false")
    Long countByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT COUNT(s) FROM SpaceEntity s WHERE s.active = true AND s.deleted = false")
    long countPublishedSpaces();

    @Query("SELECT s FROM SpaceEntity s WHERE s.active = true or s.active = false ")
    List<SpaceEntity> findAllActiveSpaces();
}