package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.entities.BookingEntity;
import Latam.Latam.work.hub.enums.BookingStatus;
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
}
