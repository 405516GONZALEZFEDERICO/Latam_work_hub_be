package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.dtos.common.reports.admin.BookingReportRowDto;
import Latam.Latam.work.hub.entities.BookingEntity;
import Latam.Latam.work.hub.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, Long> {
    /**
     * Busca reservas que se solapan con el período especificado para un espacio
     */
    @Query("SELECT b FROM BookingEntity b " +
            "WHERE b.space.id = :spaceId " +
            "AND b.active = true " +
            "AND (" +
            "  (b.bookingType = 'PER_HOUR' AND " +
            "   b.startDate = :startDate AND " +
            "   ((b.initHour <= :endHour AND b.endHour >= :initHour) OR " +
            "    (b.initHour IS NULL AND b.endHour IS NULL))) " +
            "  OR " +
            "  (b.bookingType = 'PER_DAY' AND " +
            "   b.startDate = :startDate) " +
            "  OR " +
            "  (b.bookingType = 'PER_MONTH' AND " +
            "   b.startDate <= :endDate AND " +
            "   (b.endDate >= :startDate OR b.endDate IS NULL))" +
            ")")
    List<BookingEntity> findOverlappingBookings(
            @Param("spaceId") Long spaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("initHour") LocalTime initHour,
            @Param("endHour") LocalTime endHour);

    List<BookingEntity> findByStatus(BookingStatus status);
    /**
     * Busca reservas confirmadas cuya fecha de inicio ha llegado o está cerca
     */
    @Query("SELECT b FROM BookingEntity b " +
            "WHERE b.status = 'CONFIRMED' " +
            "AND b.startDate <= :now " +
            "AND (b.endDate IS NULL OR b.endDate > :now)")
    List<BookingEntity> findUpcomingBookings(@Param("now") LocalDateTime now);

    /**
     * Busca reservas activas cuya fecha de finalización ha pasado
     */
    @Query("SELECT b FROM BookingEntity b " +
            "WHERE b.status = 'ACTIVE' " +
            "AND ((b.endDate IS NOT NULL AND b.endDate < :now) " +
            "     OR (b.endDate IS NULL AND b.startDate < :now AND b.bookingType = 'PER_DAY'))")
    List<BookingEntity> findExpiredBookings(@Param("now") LocalDateTime now);


    @Query("SELECT b FROM BookingEntity b WHERE b.user.firebaseUid = :uid " +
            "AND (:status IS NULL OR b.status = :status)")
    Page<BookingEntity> findByUserFirebaseUidAndStatus(
            @Param("uid") String uid,
            @Param("status") BookingStatus status,
            Pageable pageable);


    @Query("SELECT b.space.id AS spaceId, COUNT(b.id) AS count " +
            "FROM BookingEntity b " +
            "WHERE b.space.id IN :spaceIds AND b.status = :status " +
            "  AND (:startDate IS NULL OR b.startDate >= :startDate) " +
            "  AND (:endDate IS NULL OR b.startDate <= :endDate) " +
            "GROUP BY b.space.id")
    List<Object[]> countBookingsForSpacesInPeriod(
            @Param("spaceIds") List<Long> spaceIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") BookingStatus status
    );




    // Query principal simplificada
    @Query(value = "SELECT b " +
            "FROM BookingEntity b " +
            "JOIN FETCH b.space s " +
            "JOIN FETCH b.user c " + // b.user es el cliente
            "JOIN FETCH s.owner p " + // s.owner es el proveedor del espacio
            "WHERE (:startDateParam IS NULL OR b.startDate >= :startDateParam) " +
            "AND (:endDateParam IS NULL OR b.startDate <= :endDateParam) " + // Ajustar si el filtro es sobre b.endDate
            "AND (:clientIdParam IS NULL OR c.id = :clientIdParam) " +
            "AND (:providerIdParam IS NULL OR p.id = :providerIdParam) " +
            "AND (:spaceIdParam IS NULL OR s.id = :spaceIdParam) " +
            "AND (:bookingStatusEnum IS NULL OR b.status = :bookingStatusEnum)",
            countQuery = "SELECT COUNT(b) FROM BookingEntity b JOIN b.space s JOIN b.user c JOIN s.owner p " +
                    "WHERE (:startDateParam IS NULL OR b.startDate >= :startDateParam) " +
                    "AND (:endDateParam IS NULL OR b.startDate <= :endDateParam) " +
                    "AND (:clientIdParam IS NULL OR c.id = :clientIdParam) " +
                    "AND (:providerIdParam IS NULL OR p.id = :providerIdParam) " +
                    "AND (:spaceIdParam IS NULL OR s.id = :spaceIdParam) " +
                    "AND (:bookingStatusEnum IS NULL OR b.status = :bookingStatusEnum)")
    Page<BookingEntity> findBookingsForReport( // Devuelve Entidades
                                               @Param("startDateParam") LocalDateTime startDateParam,
                                               @Param("endDateParam") LocalDateTime endDateParam,
                                               @Param("clientIdParam") Long clientIdParam,
                                               @Param("providerIdParam") Long providerIdParam,
                                               @Param("spaceIdParam") Long spaceIdParam,
                                               @Param("bookingStatusEnum") BookingStatus bookingStatusEnum,
                                               Pageable pageable
    );


    @Query("SELECT COUNT(b) FROM BookingEntity b WHERE b.startDate BETWEEN :start AND :end AND b.status = :status")
    long countByStartDateBetweenAndStatus(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") BookingStatus status
    );

}
