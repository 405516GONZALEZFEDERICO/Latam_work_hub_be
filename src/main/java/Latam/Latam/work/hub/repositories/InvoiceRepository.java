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
    List<InvoiceEntity> findInvoicesExpiringOn(@Param("dueDate")
                                               LocalDate dueDate);
    @Query("SELECT i.rentalContract.space.id, SUM(i.totalAmount) " +
            "FROM InvoiceEntity i " +
            "WHERE i.rentalContract.space.id IN :spaceIds " +
            "AND i.status = :status " +
            "AND i.rentalContract IS NOT NULL " +
            "GROUP BY i.rentalContract.space.id")
    List<Object[]> sumAllRevenueForSpacesFromContracts(
            @Param("spaceIds") List<Long> spaceIds,
            @Param("status") InvoiceStatus status
    );



    /**
     * Busca facturas por ID de contrato y estado
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.rentalContract.id = :contractId AND i.status = :status")
    List<InvoiceEntity> findByRentalContractIdAndStatus(
            @Param("contractId") Long contractId,
            @Param("status") InvoiceStatus status
    );







    @Query("SELECT i.booking.space.id, SUM(i.totalAmount) " +
            "FROM InvoiceEntity i " +
            "WHERE i.booking.space.id IN :spaceIds " +
            "AND i.status = 'PAID' " +
            "AND i.booking IS NOT NULL " +
            "AND (i.booking.status = 'CONFIRMED' OR i.booking.status = 'COMPLETED' OR i.booking.status = 'ACTIVE') " +
            "GROUP BY i.booking.space.id")
    List<Object[]> sumRevenueForActiveBookings(@Param("spaceIds") List<Long> spaceIds);



















    @Query(value = """
    SELECT DISTINCT i
    FROM InvoiceEntity i
    LEFT JOIN FETCH i.booking b 
    LEFT JOIN FETCH b.user bc
    LEFT JOIN FETCH i.rentalContract rcontract 
    LEFT JOIN FETCH rcontract.tenant rct
    WHERE (:filterStartDate IS NULL OR i.issueDate >= :filterStartDate)
    AND (:filterEndDate IS NULL OR i.issueDate <= :filterEndDate)
    AND (:invoiceStatusEnum IS NULL OR i.status = :invoiceStatusEnum)
""",
            countQuery = """
    SELECT COUNT(DISTINCT i)
    FROM InvoiceEntity i
    LEFT JOIN i.booking b 
    LEFT JOIN b.user bc
    LEFT JOIN i.rentalContract rcontract 
    LEFT JOIN rcontract.tenant rct
    WHERE (:filterStartDate IS NULL OR i.issueDate >= :filterStartDate)
    AND (:filterEndDate IS NULL OR i.issueDate <= :filterEndDate)
    AND (:invoiceStatusEnum IS NULL OR i.status = :invoiceStatusEnum)
""")
    Page<InvoiceEntity> findInvoicesForReport(
            @Param("filterStartDate") LocalDateTime filterStartDate,
            @Param("filterEndDate") LocalDateTime filterEndDate,
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

//    @Query("SELECT COALESCE(SUM(i.totalAmount), 0.0) FROM InvoiceEntity i " +
//            "WHERE i.status = :status " +
//            "AND i.issueDate BETWEEN :startDate AND :endDate " +
//            "AND (i.booking IS NOT NULL OR i.rentalContract IS NOT NULL)")
//    Double sumTotalAmountByStatusAndDateRange(
//            @Param("status") InvoiceStatus status,
//            @Param("startDate") LocalDateTime startDate,
//            @Param("endDate") LocalDateTime endDate
//    );
@Query("""
        SELECT COALESCE(SUM(i.totalAmount - COALESCE(i.refundAmount, 0)), 0.0)
        FROM InvoiceEntity i
        WHERE i.rentalContract.space.owner.id = :providerId
        AND i.status = 'PAID'
        AND (:startDate IS NULL OR i.issueDate >= :startDate)
        AND (:endDate IS NULL OR i.issueDate <= :endDate)
    """)
Double sumRevenueByProviderId(
        @Param("providerId") Long providerId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
);

    @Query("""
        SELECT COALESCE(SUM(i.totalAmount - COALESCE(i.refundAmount, 0)), 0.0)
        FROM InvoiceEntity i
        LEFT JOIN i.booking b
        LEFT JOIN i.rentalContract rc
        WHERE (b.user.id = :clientId OR rc.tenant.id = :clientId)
        AND i.status IN ('PAID', 'ISSUED')
        AND (:startDate IS NULL OR i.issueDate >= :startDate)
        AND (:endDate IS NULL OR i.issueDate <= :endDate)
    """)
    Double sumSpendingByClientId(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    // HU1: Ingresos totales (últimos 30 días)
    @Query("SELECT SUM(CASE WHEN inv.status = 'CANCELLED' THEN -1 * COALESCE(inv.refundAmount, 0) " +
           "            ELSE (inv.totalAmount - COALESCE(inv.refundAmount, 0)) END) " +
           "FROM InvoiceEntity inv " +
           "WHERE (inv.status = 'PAID' OR inv.status = 'CANCELLED') " +
           "AND inv.issueDate >= :startDate " +
           "AND inv.issueDate <= :endDate " +
           "AND inv.rentalContract IS NOT NULL " +
           "AND inv.booking IS NULL")
    Double sumTotalAmountByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // HU2: Gráfico de líneas de ingresos por mes.
    // JPQL estándar no tiene funciones de extracción de mes/año de forma portable para GROUP BY.
    // Traemos issueDate y totalAmount, y agrupamos en la capa de servicio.
    @Query("SELECT inv.issueDate, (CASE WHEN inv.status = 'CANCELLED' THEN 0 ELSE (inv.totalAmount - COALESCE(inv.refundAmount, 0)) END) " +
           "FROM InvoiceEntity inv " +
           "WHERE inv.status = :status " +
           "AND inv.status != 'CANCELLED' " +
           "AND inv.issueDate >= :startDate " +
           "AND inv.rentalContract IS NOT NULL " +
           "AND inv.booking IS NULL " +
           "ORDER BY inv.issueDate ASC")
    List<Object[]> findRawMonthlyRevenueData(
            @Param("status") InvoiceStatus status,
            @Param("startDate") LocalDateTime startDate
    );

    // Traer todas las facturas de contratos para calcular ingresos
    @Query("SELECT inv FROM InvoiceEntity inv " +
           "WHERE inv.status IN (:statuses) " +
           "AND inv.issueDate >= :startDate " +
           "AND inv.issueDate <= :endDate " +
           "AND inv.rentalContract IS NOT NULL " +
           "AND inv.booking IS NULL")
    List<InvoiceEntity> findInvoicesForRevenueCalculation(
            @Param("statuses") List<InvoiceStatus> statuses,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Traer facturas para gráfico de ingresos mensuales
    @Query("SELECT inv FROM InvoiceEntity inv " +
           "WHERE inv.status IN (:statuses) " +
           "AND inv.issueDate >= :startDate " +
           "AND inv.rentalContract IS NOT NULL " +
           "AND inv.booking IS NULL " +
           "ORDER BY inv.issueDate ASC")
    List<InvoiceEntity> findInvoicesForMonthlyRevenueData(
            @Param("statuses") List<InvoiceStatus> statuses,
            @Param("startDate") LocalDateTime startDate
    );

    @Query("SELECT FUNCTION('YEAR', inv.issueDate) as year, FUNCTION('MONTH', inv.issueDate) as month, " +
           "SUM(CASE WHEN inv.status = 'CANCELLED' THEN -1 * COALESCE(inv.refundAmount, 0) " +
           "     ELSE (inv.totalAmount - COALESCE(inv.refundAmount, 0)) END) as revenue " +
           "FROM InvoiceEntity inv " +
           "WHERE (inv.status = 'PAID' OR inv.status = 'CANCELLED') " +
           "AND inv.rentalContract IS NOT NULL " +
           "AND inv.booking IS NULL " +
           "AND inv.issueDate >= :startDate " +
           "GROUP BY FUNCTION('YEAR', inv.issueDate), FUNCTION('MONTH', inv.issueDate) " +
           "ORDER BY year ASC, month ASC")
    List<Object[]> findMonthlyRevenue(@Param("startDate") LocalDateTime startDate);

    // ===== MÉTODOS PARA DASHBOARD PROVEEDOR =====
    
    /**
     * Obtiene ingresos mensuales de un proveedor específico
     */
    @Query("SELECT FUNCTION('YEAR', inv.issueDate) as year, FUNCTION('MONTH', inv.issueDate) as month, " +
           "SUM(CASE WHEN inv.status = 'CANCELLED' THEN -1 * COALESCE(inv.refundAmount, 0) " +
           "     ELSE (inv.totalAmount - COALESCE(inv.refundAmount, 0)) END) as revenue " +
           "FROM InvoiceEntity inv " +
           "WHERE inv.rentalContract.space.owner.id = :providerId " +
           "AND (inv.status = 'PAID' OR inv.status = 'CANCELLED') " +
           "AND inv.rentalContract IS NOT NULL " +
           "AND inv.booking IS NULL " +
           "AND inv.issueDate >= :startDate " +
           "GROUP BY FUNCTION('YEAR', inv.issueDate), FUNCTION('MONTH', inv.issueDate) " +
           "ORDER BY year ASC, month ASC")
    List<Object[]> findMonthlyRevenueByProvider(
            @Param("providerId") Long providerId,
            @Param("startDate") LocalDateTime startDate
    );

    /**
     * Obtiene ingresos mensuales BRUTOS de un proveedor específico (para gráfico monthly-revenue)
     * Solo totalAmount sin descontar reembolsos, para coincidir con la lógica del admin
     */
    @Query("SELECT FUNCTION('YEAR', inv.issueDate) as year, FUNCTION('MONTH', inv.issueDate) as month, " +
           "SUM(inv.totalAmount) as revenue " +
           "FROM InvoiceEntity inv " +
           "WHERE inv.rentalContract.space.owner.id = :providerId " +
           "AND inv.status = 'PAID' " +
           "AND inv.rentalContract IS NOT NULL " +
           "AND inv.booking IS NULL " +
           "AND inv.issueDate >= :startDate " +
           "GROUP BY FUNCTION('YEAR', inv.issueDate), FUNCTION('MONTH', inv.issueDate) " +
           "ORDER BY year ASC, month ASC")
    List<Object[]> findMonthlyGrossRevenueByProvider(
            @Param("providerId") Long providerId,
            @Param("startDate") LocalDateTime startDate
    );

    // ===== MÉTODOS PARA DASHBOARD CLIENTE =====
    
    /**
     * Obtiene gastos mensuales de un cliente específico SOLO DE CONTRATOS
     * CORREGIDO: Usa la fecha de inicio del contrato para agrupar por mes
     */
    @Query("SELECT FUNCTION('YEAR', inv.rentalContract.startDate) as year, " +
           "FUNCTION('MONTH', inv.rentalContract.startDate) as month, " +
           "SUM(inv.totalAmount) as spending " +
           "FROM InvoiceEntity inv " +
           "WHERE inv.rentalContract.tenant.id = :clientId " +
           "AND inv.status IN ('PAID', 'ISSUED') " +
           "AND inv.rentalContract.startDate >= FUNCTION('DATE', :startDate) " +
           "AND inv.rentalContract IS NOT NULL " +
           "GROUP BY FUNCTION('YEAR', inv.rentalContract.startDate), " +
           "FUNCTION('MONTH', inv.rentalContract.startDate) " +
           "ORDER BY year ASC, month ASC")
    List<Object[]> findMonthlySpendingByClientContracts(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate
    );

    /**
     * Obtiene gastos mensuales BRUTOS de un cliente específico SOLO DE CONTRATOS (para gráfico monthly-spending)
     * Usa la MISMA lógica que los KPI cards: issueDate y estados PAID + ISSUED
     */
    @Query("SELECT FUNCTION('YEAR', inv.issueDate) as year, " +
           "FUNCTION('MONTH', inv.issueDate) as month, " +
           "SUM(inv.totalAmount) as spending " +
           "FROM InvoiceEntity inv " +
           "WHERE inv.rentalContract.tenant.id = :clientId " +
           "AND inv.status IN ('PAID', 'ISSUED') " +
           "AND inv.issueDate >= :startDate " +
           "AND inv.rentalContract IS NOT NULL " +
           "GROUP BY FUNCTION('YEAR', inv.issueDate), " +
           "FUNCTION('MONTH', inv.issueDate) " +
           "ORDER BY year ASC, month ASC")
    List<Object[]> findMonthlyGrossSpendingByClientContracts(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate
    );

    /**
     * Obtiene gastos mensuales de un cliente específico SOLO DE RESERVAS 
     */
    @Query("SELECT FUNCTION('YEAR', inv.issueDate) as year, FUNCTION('MONTH', inv.issueDate) as month, " +
           "SUM(inv.totalAmount) as spending " +
           "FROM InvoiceEntity inv " +
           "WHERE inv.booking.user.id = :clientId " +
           "AND inv.status IN ('PAID', 'ISSUED') " +
           "AND inv.issueDate >= :startDate " +
           "AND inv.booking IS NOT NULL " +
           "GROUP BY FUNCTION('YEAR', inv.issueDate), FUNCTION('MONTH', inv.issueDate) " +
           "ORDER BY year ASC, month ASC")
    List<Object[]> findMonthlySpendingByClientBookings(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate
    );

    // ===== NUEVOS MÉTODOS PARA DISTINGUIR INGRESOS BRUTOS VS NETOS =====
    
    /**
     * Ingresos BRUTOS totales de CONTRATOS únicamente (sin descontar reembolsos) - ADMIN
     * Solo facturas pagadas de contratos de alquiler, NO de reservas
     */
    @Query("SELECT COALESCE(SUM(inv.totalAmount), 0.0) " +
           "FROM InvoiceEntity inv " +
           "WHERE inv.status = 'PAID' " +
           "AND inv.issueDate >= :startDate " +
           "AND inv.issueDate <= :endDate " +
           "AND inv.rentalContract IS NOT NULL " +
           "AND inv.booking IS NULL")
    Double sumGrossRevenueByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Total de REEMBOLSOS en facturas de CONTRATOS únicamente - ADMIN  
     * Solo reembolsos en facturas de contratos, NO de reservas
     */
    @Query("SELECT COALESCE(SUM(inv.refundAmount), 0.0) " +
           "FROM InvoiceEntity inv " +
           "WHERE inv.refundAmount IS NOT NULL " +
           "AND inv.refundAmount > 0 " +
           "AND inv.issueDate >= :startDate " +
           "AND inv.issueDate <= :endDate " +
           "AND inv.rentalContract IS NOT NULL " +
           "AND inv.booking IS NULL")
    Double sumTotalRefundsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Ingresos BRUTOS por proveedor de CONTRATOS (sin descontar reembolsos) - PROVEEDOR
     */
    @Query("SELECT COALESCE(SUM(inv.totalAmount), 0.0) " +
           "FROM InvoiceEntity inv " +
           "WHERE inv.rentalContract.space.owner.id = :providerId " +
           "AND inv.status = 'PAID' " +
           "AND inv.issueDate >= :startDate " +
           "AND inv.issueDate <= :endDate " +
           "AND inv.rentalContract IS NOT NULL")
    Double sumGrossRevenueFromContractsByProviderId(
            @Param("providerId") Long providerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**  
     * Ingresos BRUTOS por proveedor de RESERVAS (sin descontar reembolsos) - PROVEEDOR
     */
    @Query("SELECT COALESCE(SUM(inv.totalAmount), 0.0) " +
           "FROM InvoiceEntity inv " +
           "WHERE inv.booking.space.owner.id = :providerId " +
           "AND inv.status = 'PAID' " +
           "AND inv.issueDate >= :startDate " +
           "AND inv.issueDate <= :endDate " +
           "AND inv.booking IS NOT NULL")
    Double sumGrossRevenueFromBookingsByProviderId(
            @Param("providerId") Long providerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Total de REEMBOLSOS en facturas de CONTRATOS por proveedor - PROVEEDOR
     */
    @Query("SELECT COALESCE(SUM(inv.refundAmount), 0.0) " +
           "FROM InvoiceEntity inv " +
           "WHERE inv.refundAmount IS NOT NULL " +
           "AND inv.refundAmount > 0 " +
           "AND inv.issueDate >= :startDate " +
           "AND inv.issueDate <= :endDate " +
           "AND inv.rentalContract.space.owner.id = :providerId " +
           "AND inv.rentalContract IS NOT NULL")
    Double sumInvoiceRefundsFromContractsByProviderId(
            @Param("providerId") Long providerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Total de REEMBOLSOS en facturas de RESERVAS por proveedor - PROVEEDOR
     */
    @Query("SELECT COALESCE(SUM(inv.refundAmount), 0.0) " +
           "FROM InvoiceEntity inv " +
           "WHERE inv.refundAmount IS NOT NULL " +
           "AND inv.refundAmount > 0 " +
           "AND inv.issueDate >= :startDate " +
           "AND inv.issueDate <= :endDate " +
           "AND inv.booking.space.owner.id = :providerId " +
           "AND inv.booking IS NOT NULL")
    Double sumInvoiceRefundsFromBookingsByProviderId(
            @Param("providerId") Long providerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Gastos BRUTOS por cliente de CONTRATOS (sin descontar reembolsos) - CLIENTE
     * CORREGIDO: Incluye facturas PAID e ISSUED
     */
    @Query("SELECT COALESCE(SUM(inv.totalAmount), 0.0) " +
           "FROM InvoiceEntity inv " +
           "WHERE inv.rentalContract.tenant.id = :clientId " +
           "AND inv.status IN ('PAID', 'ISSUED') " +
           "AND inv.issueDate >= :startDate " +
           "AND inv.issueDate <= :endDate " +
           "AND inv.rentalContract IS NOT NULL")
    Double sumGrossSpendingFromContractsByClientId(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Gastos BRUTOS por cliente de RESERVAS (sin descontar reembolsos) - CLIENTE
     */
    @Query("SELECT COALESCE(SUM(inv.totalAmount), 0.0) " +
           "FROM InvoiceEntity inv " +
           "WHERE inv.booking.user.id = :clientId " +
           "AND inv.status = 'PAID' " +
           "AND inv.issueDate >= :startDate " +
           "AND inv.issueDate <= :endDate " +
           "AND inv.booking IS NOT NULL")
    Double sumGrossSpendingFromBookingsByClientId(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Total de REEMBOLSOS en facturas de CONTRATOS recibidos por cliente - CLIENTE
     */
    @Query("SELECT COALESCE(SUM(inv.refundAmount), 0.0) " +
           "FROM InvoiceEntity inv " +
           "WHERE inv.refundAmount IS NOT NULL " +
           "AND inv.refundAmount > 0 " +
           "AND inv.issueDate >= :startDate " +
           "AND inv.issueDate <= :endDate " +
           "AND inv.rentalContract.tenant.id = :clientId " +
           "AND inv.rentalContract IS NOT NULL")
    Double sumInvoiceRefundsFromContractsByClientId(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Total de REEMBOLSOS en facturas de RESERVAS recibidos por cliente - CLIENTE
     */
    @Query("SELECT COALESCE(SUM(inv.refundAmount), 0.0) " +
           "FROM InvoiceEntity inv " +
           "WHERE inv.refundAmount IS NOT NULL " +
           "AND inv.refundAmount > 0 " +
           "AND inv.issueDate >= :startDate " +
           "AND inv.issueDate <= :endDate " +
           "AND inv.booking.user.id = :clientId " +
           "AND inv.booking IS NOT NULL")
    Double sumInvoiceRefundsFromBookingsByClientId(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * DEBUG: Busca TODAS las facturas de contratos de un cliente sin filtrar por estado
     */
    @Query("SELECT inv FROM InvoiceEntity inv " +
           "WHERE inv.rentalContract.tenant.id = :clientId " +
           "AND inv.issueDate >= :startDate " +
           "AND inv.issueDate <= :endDate " +
           "AND inv.rentalContract IS NOT NULL " +
           "ORDER BY inv.issueDate DESC")
    List<InvoiceEntity> findAllContractInvoicesByClientId(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * DEBUG: Lista todas las facturas de contratos de un cliente para investigar gastos mensuales
     */
    @Query("SELECT inv.id, inv.invoiceNumber, inv.totalAmount, inv.issueDate, inv.status, inv.rentalContract.startDate " +
           "FROM InvoiceEntity inv " +
           "WHERE inv.rentalContract.tenant.id = :clientId " +
           "AND inv.status IN ('PAID', 'ISSUED') " +
           "AND inv.issueDate >= :startDate " +
           "AND inv.rentalContract IS NOT NULL " +
           "ORDER BY inv.issueDate DESC")
    List<Object[]> findAllContractInvoicesForDebug(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate
    );

    /**
     * Obtiene gastos mensuales de un cliente específico SOLO DE CONTRATOS con LocalDate
     */
    @Query("SELECT FUNCTION('YEAR', inv.rentalContract.startDate) as year, " +
           "FUNCTION('MONTH', inv.rentalContract.startDate) as month, " +
           "SUM(inv.totalAmount) as spending " +
           "FROM InvoiceEntity inv " +
           "WHERE inv.rentalContract.tenant.id = :clientId " +
           "AND inv.status IN ('PAID', 'ISSUED') " +
           "AND inv.rentalContract.startDate >= :startDate " +
           "AND inv.rentalContract IS NOT NULL " +
           "GROUP BY FUNCTION('YEAR', inv.rentalContract.startDate), " +
           "FUNCTION('MONTH', inv.rentalContract.startDate) " +
           "ORDER BY year ASC, month ASC")
    List<Object[]> findMonthlySpendingByClientContractsWithLocalDate(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDate startDate
    );

}
