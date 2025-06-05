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
    @Query("""
        SELECT b FROM BookingEntity b
        WHERE b.space.id = :spaceId
        AND b.status IN :statuses
        AND ((b.startDate BETWEEN :startDate AND :endDate)
        OR (b.endDate BETWEEN :startDate AND :endDate)
        OR (:startDate BETWEEN b.startDate AND b.endDate))
    """)
    List<BookingEntity> findBySpaceIdAndDateRangeAndStatuses(
            Long spaceId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<BookingStatus> statuses
    );

    /**
     * Busca reservas que se solapan con el período especificado para un espacio
     * Solo considera reservas ACTIVE o reservas CONFIRMED cuyo período de inicio ya llegó
     */
    @Query("SELECT b FROM BookingEntity b " +
            "WHERE b.space.id = :spaceId " +
            "AND ((b.status = 'ACTIVE') OR " +
            "     (b.status = 'CONFIRMED' AND b.startDate <= CURRENT_TIMESTAMP)) " +
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
     * Busca reservas confirmadas cuya fecha de inicio ha llegado para activarlas
     */
   @Query("""
       SELECT b FROM BookingEntity b
       WHERE b.status = 'CONFIRMED'
       AND b.startDate <= :now
   """)
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

    /**
     * Verifica si existe al menos una reserva activa o confirmada para un espacio específico
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BookingEntity b " +
           "WHERE b.space.id = :spaceId " +
           "AND b.status IN :statuses " +
           "AND b.active = true")
    boolean existsBySpaceIdAndStatusIn(@Param("spaceId") Long spaceId, 
                                       @Param("statuses") List<BookingStatus> statuses);

    // ===== MÉTODOS PARA DASHBOARD PROVEEDOR =====
    
    /**
     * Cuenta reservas de un proveedor en un rango de fechas
     */
    @Query("SELECT COUNT(b) FROM BookingEntity b " +
           "WHERE b.space.owner.id = :providerId " +
           "AND b.startDate >= :startDate " +
           "AND b.startDate <= :endDate " +
           "AND b.status IN :statuses")
    long countReservationsByProviderInDateRange(
            @Param("providerId") Long providerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("statuses") List<BookingStatus> statuses
    );

    /**
     * Suma ingresos de un proveedor en un rango de fechas
     */
    @Query("SELECT SUM(CASE WHEN b.status = 'CANCELED' THEN -1 * COALESCE(b.refundAmount, 0) " +
           "            ELSE (b.totalAmount - COALESCE(b.refundAmount, 0)) END) " +
           "FROM BookingEntity b " +
           "WHERE b.space.owner.id = :providerId " +
           "AND (b.status != 'PENDING_PAYMENT' AND b.status != 'DRAFT') " +
           "AND b.startDate >= :startDate " +
           "AND b.startDate <= :endDate")
    Double sumRevenueByProviderInDateRange(
            @Param("providerId") Long providerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Obtiene ingresos mensuales de un proveedor
     */
    @Query("SELECT FUNCTION('YEAR', b.startDate) as year, FUNCTION('MONTH', b.startDate) as month, " +
           "SUM(CASE WHEN b.status = 'CANCELED' THEN -1 * COALESCE(b.refundAmount, 0) " +
           "     ELSE (b.totalAmount - COALESCE(b.refundAmount, 0)) END) as revenue " +
           "FROM BookingEntity b " +
           "WHERE b.space.owner.id = :providerId " +
           "AND (b.status != 'PENDING_PAYMENT' AND b.status != 'DRAFT') " +
           "AND b.startDate >= :startDate " +
           "GROUP BY FUNCTION('YEAR', b.startDate), FUNCTION('MONTH', b.startDate) " +
           "ORDER BY year ASC, month ASC")
    List<Object[]> findMonthlyRevenueByProvider(
            @Param("providerId") Long providerId,
            @Param("startDate") LocalDateTime startDate
    );

    // ===== MÉTODOS PARA DASHBOARD CLIENTE =====
    
    /**
     * Cuenta reservas de un cliente por estados
     */
    @Query("SELECT COUNT(b) FROM BookingEntity b " +
           "WHERE b.user.id = :clientId " +
           "AND b.status IN :statuses")
    long countByUserIdAndStatuses(
            @Param("clientId") Long clientId,
            @Param("statuses") List<BookingStatus> statuses
    );

    /**
     * Suma gastos de un cliente en un rango de fechas
     * Usa updatedAt cuando está disponible, incluye todas las reservas como fallback
     */
    @Query("SELECT SUM(b.totalAmount) " +
           "FROM BookingEntity b " +
           "WHERE b.user.id = :clientId " +
           "AND b.status IN ('CONFIRMED', 'COMPLETED', 'ACTIVE') " +
           "AND (b.updatedAt IS NULL OR b.updatedAt >= :startDate) " +
           "AND (b.updatedAt IS NULL OR b.updatedAt <= :endDate)")
    Double sumSpendingByClientInDateRange(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Obtiene gastos mensuales de un cliente
     */
    @Query("SELECT FUNCTION('YEAR', b.startDate) as year, FUNCTION('MONTH', b.startDate) as month, " +
           "SUM(b.totalAmount) as spending " +
           "FROM BookingEntity b " +
           "WHERE b.user.id = :clientId " +
           "AND b.status IN ('CONFIRMED', 'COMPLETED', 'ACTIVE') " +
           "AND b.startDate >= :startDate " +
           "GROUP BY FUNCTION('YEAR', b.startDate), FUNCTION('MONTH', b.startDate) " +
           "ORDER BY year ASC, month ASC")
    List<Object[]> findMonthlySpendingByClient(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate
    );

    /**
     * Obtiene conteo de reservas por tipo de espacio para un cliente
     */
    @Query("SELECT b.space.type.name, COUNT(b.id) " +
           "FROM BookingEntity b " +
           "WHERE b.user.id = :clientId " +
           "AND b.space.type.name IS NOT NULL " +
           "GROUP BY b.space.type.name " +
           "ORDER BY COUNT(b.id) DESC")
    List<Object[]> findBookingCountBySpaceTypeForClient(@Param("clientId") Long clientId);

    // ===== NUEVOS MÉTODOS PARA DISTINGUIR INGRESOS BRUTOS VS NETOS EN RESERVAS =====
    
    /**
     * Ingresos BRUTOS totales de reservas - INCLUYE CANCELADAS porque representan dinero generado
     * Usa updatedAt cuando está disponible, startDate como fallback
     */
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0.0) " +
           "FROM BookingEntity b " +
           "WHERE b.status IN ('CONFIRMED', 'COMPLETED', 'ACTIVE', 'CANCELED') " +
           "AND b.status != 'PENDING_PAYMENT' " +
           "AND b.status != 'DRAFT' " +
           "AND (b.updatedAt IS NULL OR b.updatedAt >= :startDate) " +
           "AND (b.updatedAt IS NULL OR b.updatedAt <= :endDate)")
    Double sumGrossRevenueByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Total de REEMBOLSOS en reservas canceladas - ADMIN
     * Usa updatedAt cuando está disponible, incluye todos como fallback
     */
    @Query("SELECT COALESCE(SUM(b.refundAmount), 0.0) " +
           "FROM BookingEntity b " +
           "WHERE b.status = 'CANCELED' " +
           "AND b.refundAmount IS NOT NULL " +
           "AND b.refundAmount > 0 " +
           "AND (b.updatedAt IS NULL OR b.updatedAt >= :startDate) " +
           "AND (b.updatedAt IS NULL OR b.updatedAt <= :endDate)")
    Double sumTotalRefundsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Ingresos BRUTOS de reservas por proveedor - INCLUYE CANCELADAS porque representan dinero generado
     * Usa updatedAt cuando está disponible, startDate como fallback
     */
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0.0) " +
           "FROM BookingEntity b " +
           "WHERE b.space.owner.id = :providerId " +
           "AND b.status IN ('CONFIRMED', 'COMPLETED', 'ACTIVE', 'CANCELED') " +
           "AND b.status != 'PENDING_PAYMENT' " +
           "AND b.status != 'DRAFT' " +
           "AND (b.updatedAt IS NULL OR b.updatedAt >= :startDate) " +
           "AND (b.updatedAt IS NULL OR b.updatedAt <= :endDate)")
    Double sumGrossRevenueByProviderInDateRange(
            @Param("providerId") Long providerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Total de REEMBOLSOS en reservas canceladas por proveedor - PROVEEDOR
     * Usa updatedAt cuando está disponible, incluye todos como fallback
     */
    @Query("SELECT COALESCE(SUM(b.refundAmount), 0.0) " +
           "FROM BookingEntity b " +
           "WHERE b.space.owner.id = :providerId " +
           "AND b.status = 'CANCELED' " +
           "AND b.refundAmount IS NOT NULL " +
           "AND b.refundAmount > 0 " +
           "AND (b.updatedAt IS NULL OR b.updatedAt >= :startDate) " +
           "AND (b.updatedAt IS NULL OR b.updatedAt <= :endDate)")
    Double sumTotalRefundsByProviderInDateRange(
            @Param("providerId") Long providerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Gastos BRUTOS de reservas por cliente - INCLUYE CANCELADAS porque representan dinero gastado
     * Los reembolsos se descuentan por separado en otra consulta
     */
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0.0) " +
           "FROM BookingEntity b " +
           "WHERE b.user.id = :clientId " +
           "AND b.status IN ('CONFIRMED', 'COMPLETED', 'ACTIVE', 'CANCELED') " +
           "AND b.status != 'PENDING_PAYMENT' " +
           "AND b.status != 'DRAFT' " +
           "AND (b.updatedAt IS NULL OR b.updatedAt >= :startDate) " +
           "AND (b.updatedAt IS NULL OR b.updatedAt <= :endDate)")
    Double sumGrossSpendingByClientInDateRange(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Total de REEMBOLSOS recibidos por cliente en reservas canceladas - CLIENTE
     * Usa updatedAt cuando está disponible, incluye todos los reembolsos como fallback
     */
    @Query("SELECT COALESCE(SUM(b.refundAmount), 0.0) " +
           "FROM BookingEntity b " +
           "WHERE b.user.id = :clientId " +
           "AND b.status = 'CANCELED' " +
           "AND b.refundAmount IS NOT NULL " +
           "AND b.refundAmount > 0 " +
           "AND (b.updatedAt IS NULL OR b.updatedAt >= :startDate) " +
           "AND (b.updatedAt IS NULL OR b.updatedAt <= :endDate)")
    Double sumTotalRefundsByClientInDateRange(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

}
