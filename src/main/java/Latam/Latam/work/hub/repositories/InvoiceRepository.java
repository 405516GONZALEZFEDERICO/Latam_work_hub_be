package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.dtos.common.reports.admin.InvoiceReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.OverdueInvoiceAlertDto;
import Latam.Latam.work.hub.entities.InvoiceEntity;
import Latam.Latam.work.hub.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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


    @Query("SELECT bk.space.id AS spaceId, SUM(inv.totalAmount) AS totalRevenue " +
            "FROM InvoiceEntity inv JOIN inv.booking bk " +
            "WHERE bk.space.id IN :spaceIds AND inv.status = :status " +
            "  AND (:startDate IS NULL OR inv.issueDate >= :startDate) " +
            "  AND (:endDate IS NULL OR inv.issueDate <= :endDate) " +
            "GROUP BY bk.space.id")
    List<Object[]> sumRevenueForSpacesFromBookingsInPeriod(
            @Param("spaceIds") List<Long> spaceIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") InvoiceStatus status
    );

    @Query("SELECT rc_entity.space.id AS spaceId, SUM(inv.totalAmount) AS totalRevenue " + // Cambiado rc a rc_entity para evitar conflicto con el alias 'rc' de la entidad
            "FROM InvoiceEntity inv JOIN inv.rentalContract rc_entity " + // rc_entity es RentalContractEntity
            "WHERE rc_entity.space.id IN :spaceIds AND inv.status = :status " +
            "  AND (:startDate IS NULL OR inv.issueDate >= :startDate) " +
            "  AND (:endDate IS NULL OR inv.issueDate <= :endDate) " +
            "GROUP BY rc_entity.space.id")
    List<Object[]> sumRevenueForSpacesFromContractsInPeriod(
            @Param("spaceIds") List<Long> spaceIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") InvoiceStatus status
    );

















    // Query principal simplificada
    @Query(value = "SELECT i " +
            "FROM InvoiceEntity i " +
            "LEFT JOIN FETCH i.booking b LEFT JOIN FETCH b.user bc " + // Cliente de booking
            "LEFT JOIN FETCH i.rentalContract rcontract LEFT JOIN FETCH rcontract.tenant rct " + // Cliente de contrato
            "WHERE ((:filterStartDate IS NULL AND :filterEndDate IS NULL) " +
            "    OR (i.issueDate >= :filterStartDate AND i.issueDate <= :filterEndDate)) " +
            "AND (:clientId IS NULL OR bc.id = :clientId OR rct.id = :clientId) " + // Filtro de cliente
            "AND (:invoiceStatusEnum IS NULL OR i.status = :invoiceStatusEnum)",
            countQuery = "SELECT COUNT(i) FROM InvoiceEntity i " +
                    "LEFT JOIN i.booking b LEFT JOIN b.user bc " +
                    "LEFT JOIN i.rentalContract rcontract LEFT JOIN rcontract.tenant rct " +
                    "WHERE ((:filterStartDate IS NULL AND :filterEndDate IS NULL) " +
                    "    OR (i.issueDate >= :filterStartDate AND i.issueDate <= :filterEndDate)) " +
                    "AND (:clientId IS NULL OR bc.id = :clientId OR rct.id = :clientId) " +
                    "AND (:invoiceStatusEnum IS NULL OR i.status = :invoiceStatusEnum)")
    Page<InvoiceEntity> findInvoicesForReport( // Devuelve Entidades
                                               @Param("filterStartDate") LocalDateTime filterStartDate,
                                               @Param("filterEndDate") LocalDateTime filterEndDate,
                                               @Param("clientId") Long clientId,
                                               @Param("invoiceStatusEnum") InvoiceStatus invoiceStatusEnum,
                                               Pageable pageable
    );

    // Query para Alertas simplificada
    @Query(value = "SELECT i " +
            "FROM InvoiceEntity i " +
            "LEFT JOIN FETCH i.booking b LEFT JOIN FETCH b.user bc " +
            "LEFT JOIN FETCH i.rentalContract rcontract LEFT JOIN FETCH rcontract.tenant rct " +
            "WHERE i.status IN :statuses " +
            "AND i.dueDate < :overdueThresholdDate",
            countQuery = "SELECT COUNT(i) FROM InvoiceEntity i " +
                    "WHERE i.status IN :statuses " +
                    "AND i.dueDate < :overdueThresholdDate")
    Page<InvoiceEntity> findOverdueInvoicesForAlerts( // Devuelve Entidades
                                                      @Param("overdueThresholdDate") LocalDateTime overdueThresholdDate,
                                                      @Param("statuses") List<InvoiceStatus> statuses,
                                                      Pageable pageable
    );



}
