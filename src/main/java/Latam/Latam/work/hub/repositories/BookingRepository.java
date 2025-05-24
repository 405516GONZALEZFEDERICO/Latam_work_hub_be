package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.entities.BookingEntity;
import Latam.Latam.work.hub.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
            "AND b.status != 'CANCELED' " +
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



    @Query("SELECT b FROM BookingEntity b WHERE b.user.firebaseUid = :uid " +
            "AND (:status IS NULL OR b.status = :status)")
    Page<BookingEntity> findByUserFirebaseUidAndStatus(
            @Param("uid") String uid,
            @Param("status") BookingStatus status,
            Pageable pageable);



    @Query("SELECT b.space.id, COUNT(b) " +
            "FROM BookingEntity b " +
            "WHERE b.space.id IN :spaceIds " +
            "AND b.status = :status " +
            "GROUP BY b.space.id")
    List<Object[]> countAllBookingsForSpaces(
            @Param("spaceIds") List<Long> spaceIds,
            @Param("status") BookingStatus status
    );

    @Query("SELECT b.space.id, COUNT(b) " +
            "FROM BookingEntity b " +
            "WHERE b.space.id IN :spaceIds " +
            "AND (b.status = 'CONFIRMED' OR b.status = 'COMPLETED' OR b.status = 'ACTIVE') " +
            "GROUP BY b.space.id")
    List<Object[]> countActiveBookingsForSpaces(@Param("spaceIds") List<Long> spaceIds);


    @Query(value = """
    SELECT b 
    FROM BookingEntity b 
    JOIN FETCH b.space s 
    JOIN FETCH b.user c 
    JOIN FETCH s.owner p 
    WHERE (:startDateParam IS NULL OR DATE(b.startDate) >= :startDateParam) 
    AND (:endDateParam IS NULL OR DATE(b.endDate) <= :endDateParam) 
    AND (:bookingStatusEnum IS NULL OR b.status = :bookingStatusEnum)
""",
            countQuery = """
    SELECT COUNT(b) FROM BookingEntity b 
    JOIN b.space s JOIN b.user c JOIN s.owner p 
    WHERE (:startDateParam IS NULL OR DATE(b.startDate) >= :startDateParam) 
    AND (:endDateParam IS NULL OR DATE(b.endDate) <= :endDateParam) 
    AND (:bookingStatusEnum IS NULL OR b.status = :bookingStatusEnum)
""")
    Page<BookingEntity> findBookingsForReport(
            @Param("startDateParam") LocalDate startDateParam,
            @Param("endDateParam") LocalDate endDateParam,
            @Param("bookingStatusEnum") BookingStatus bookingStatusEnum,
            Pageable pageable
    );



    @Query("SELECT COUNT(b) FROM BookingEntity b WHERE b.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);


    // HU1: Reservas este mes
    @Query("SELECT COUNT(b) FROM BookingEntity b WHERE b.startDate >= :startOfMonth AND b.startDate <= :endOfMonth AND b.status IN :statuses")
    long countReservationsInDateRangeWithStatuses(
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth,
            @Param("statuses") List<BookingStatus> statuses
    );

    // HU3: Reservas por Tipo de Espacio
    @Query("SELECT b.space.type.name, COUNT(b.id) FROM BookingEntity b WHERE b.space.type.name IS NOT NULL GROUP BY b.space.type.name ORDER BY COUNT(b.id) DESC")
    List<Object[]> findReservationsCountBySpaceType();





    // HU5: Histograma de horarios más alquilados.
    // Devuelve LocalTime (initHour) y el conteo. El procesamiento para extraer la HORA (int) se hará en el servicio.
    @Query("SELECT b.initHour, COUNT(b.id) FROM BookingEntity b WHERE b.status IN :statuses GROUP BY b.initHour ORDER BY b.initHour ASC")
    List<Object[]> findReservationCountsByInitHour(@Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT SUM(CASE WHEN b.status = 'CANCELED' THEN -1 * COALESCE(b.refundAmount, 0) " +
           "            ELSE (b.totalAmount - COALESCE(b.refundAmount, 0)) END) " +
           "FROM BookingEntity b " +
           "WHERE (b.status != 'PENDING_PAYMENT' AND b.status != 'DRAFT') " +
           "AND b.startDate >= :startDate " +
           "AND b.startDate <= :endDate")
    Double sumTotalRevenueByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT b.startDate, (CASE WHEN b.status = 'CANCELED' THEN 0 ELSE (b.totalAmount - COALESCE(b.refundAmount, 0)) END) " +
           "FROM BookingEntity b " +
           "WHERE (b.status = :status OR b.status = 'COMPLETED') " +
           "AND b.status != 'CANCELED' " +
           "AND b.startDate >= :startDate " +
           "ORDER BY b.startDate ASC")
    List<Object[]> findRawMonthlyRevenueData(
            @Param("status") BookingStatus status,
            @Param("startDate") LocalDateTime startDate
    );

    @Query("SELECT b FROM BookingEntity b " +
           "WHERE b.status IN (:statuses) " +
           "AND b.startDate >= :startDate " +
           "AND b.startDate <= :endDate")
    List<BookingEntity> findBookingsForRevenueCalculation(
            @Param("statuses") List<BookingStatus> statuses,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT b FROM BookingEntity b " +
           "WHERE b.status IN (:statuses) " +
           "AND b.startDate >= :startDate " +
           "ORDER BY b.startDate ASC")
    List<BookingEntity> findBookingsForMonthlyRevenueData(
            @Param("statuses") List<BookingStatus> statuses,
            @Param("startDate") LocalDateTime startDate
    );

    @Query("SELECT FUNCTION('YEAR', b.startDate) as year, FUNCTION('MONTH', b.startDate) as month, " +
           "SUM(CASE WHEN b.status = 'CANCELED' THEN -1 * COALESCE(b.refundAmount, 0) " +
           "     ELSE (b.totalAmount - COALESCE(b.refundAmount, 0)) END) as revenue " +
           "FROM BookingEntity b " +
           "WHERE (b.status != 'PENDING_PAYMENT' AND b.status != 'DRAFT') " +
           "AND b.startDate >= :startDate " +
           "GROUP BY FUNCTION('YEAR', b.startDate), FUNCTION('MONTH', b.startDate) " +
           "ORDER BY year ASC, month ASC")
    List<Object[]> findMonthlyRevenue(@Param("startDate") LocalDateTime startDate);




    @Query("SELECT COUNT(b) FROM BookingEntity b " +
            "WHERE b.space.id = :spaceId " +
            "AND (b.startDate <= :endDate AND b.endDate >= :startDate)")
    long countConflictingBookings(@Param("spaceId") Long spaceId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

}
