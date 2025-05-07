package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.entities.InvoiceEntity;
import Latam.Latam.work.hub.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {
    @Query(value = "SELECT * FROM FACTURAS " +
            "WHERE invoice_number LIKE :prefix " +
            "ORDER BY invoice_number DESC " +
            "LIMIT 1",
            nativeQuery = true)
    Optional<InvoiceEntity> findTopByInvoiceNumberLikeOrderByInvoiceNumberDesc(@Param("prefix") String prefix);
    Optional<InvoiceEntity> findByBookingId(Long bookingId);


    /**
     * Encuentra todas las facturas vencidas que no han sido pagadas
     * @param date Fecha antes de la cual buscar facturas vencidas
     * @return Lista de facturas vencidas no pagadas
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.dueDate < :date AND i.status != 'PAID'")
    List<InvoiceEntity> findByDueDateBeforeAndPaidFalse(LocalDate date);

    /**
     * Busca factura por número de factura
     */
    Optional<InvoiceEntity> findByInvoiceNumber(String invoiceNumber);


    /**
     * Busca facturas por ID de contrato de alquiler
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.rentalContract.id = :contractId " +
            "ORDER BY i.issueDate DESC")
    List<InvoiceEntity> findByRentalContractId(Long contractId);

    /**
     * Busca facturas pendientes por ID de contrato
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.rentalContract.id = :contractId " +
            "AND (i.status = 'DRAFT' OR i.status = 'ISSUED') " +
            "ORDER BY i.issueDate DESC")
    List<InvoiceEntity> findPendingInvoicesByContractId(Long contractId);

    /**
     * Busca la factura más reciente de un contrato
     */
    Optional<InvoiceEntity> findTopByRentalContractIdOrderByIssueDateDesc(Long contractId);

    /**
     * Verifica si existe una factura para un contrato en un rango de fechas
     */
    boolean existsByRentalContractIdAndIssueDateBetween(
            Long contractId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Busca facturas que vencen en una fecha específica
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE DATE(i.dueDate) = :dueDate " +
            "AND (i.status = 'DRAFT' OR i.status = 'ISSUED')")
    List<InvoiceEntity> findInvoicesExpiringOn(@Param("dueDate") LocalDate dueDate);

    /**
     * Busca facturas vencidas (pasaron la fecha de vencimiento pero no están pagadas)
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.dueDate < :currentDate AND " +
            "(i.status = 'DRAFT' OR i.status = 'ISSUED')")
    List<InvoiceEntity> findOverdueInvoices(@Param("currentDate") LocalDateTime currentDate);


    /**
     * Busca facturas por ID de contrato y estado
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.rentalContract.id = :contractId AND i.status = :status")
    List<InvoiceEntity> findByRentalContractIdAndStatus(
            @Param("contractId") Long contractId,
            @Param("status") InvoiceStatus status
    );
}
