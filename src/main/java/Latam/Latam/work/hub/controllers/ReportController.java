package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.dtos.common.reports.admin.AdminKpiDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.BookingReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.ContractReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.ExpiringContractAlertDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.InvoiceReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.OverdueInvoiceAlertDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.ReportFiltersDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.SpaceReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.UserReportRowDto;
import Latam.Latam.work.hub.services.ReportService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
    private final ReportService reportService;

    // Endpoint para KPIs generales
    @GetMapping("/kpis")
    public ResponseEntity<AdminKpiDto> getAdminKpis(ReportFiltersDto filters) { // Filtros opcionales
        logger.info("Solicitud recibida para obtener KPIs administrativos con filtros: {}", filters);
        try {
            AdminKpiDto kpis = reportService.getAdminKpis(filters);
            return ResponseEntity.ok(kpis);
        } catch (Exception e) {
            logger.error("Error al obtener KPIs administrativos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build(); // O un DTO de error
        }
    }

    // Endpoint para informe de espacios
    @GetMapping("/spaces")
    public ResponseEntity<Page<SpaceReportRowDto>> getSpacesReport(ReportFiltersDto filters, Pageable pageable) {
        logger.info("Solicitud para informe de espacios con filtros: {} y paginación: {}", filters, pageable);
        try {
            Page<SpaceReportRowDto> reportPage = reportService.getSpacesReport(filters, pageable);
            return ResponseEntity.ok(reportPage);
        } catch (Exception e) {
            logger.error("Error al generar informe de espacios: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoint para informe de reservas
    @GetMapping("/bookings")
    public ResponseEntity<Page<BookingReportRowDto>> getBookingsReport(ReportFiltersDto filters, Pageable pageable) {
        logger.info("Solicitud para informe de reservas con filtros: {} y paginación: {}", filters, pageable);
        try {
            Page<BookingReportRowDto> reportPage = reportService.getBookingsReport(filters, pageable);
            return ResponseEntity.ok(reportPage);
        } catch (Exception e) {
            logger.error("Error al generar informe de reservas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoint para informe de usuarios
    @GetMapping("/users")
    public ResponseEntity<Page<UserReportRowDto>> getUsersReport(ReportFiltersDto filters, Pageable pageable) {
        logger.info("Solicitud para informe de usuarios con filtros: {} y paginación: {}", filters, pageable);
        try {
            Page<UserReportRowDto> reportPage = reportService.getUsersReport(filters, pageable);
            return ResponseEntity.ok(reportPage);
        } catch (Exception e) {
            logger.error("Error al generar informe de usuarios: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoint para informe de contratos
    @GetMapping("/contracts")
    public ResponseEntity<Page<ContractReportRowDto>> getContractsReport(ReportFiltersDto filters, Pageable pageable) {
        logger.info("Solicitud para informe de contratos con filtros: {} y paginación: {}", filters, pageable);
        try {
            Page<ContractReportRowDto> reportPage = reportService.getContractsReport(filters, pageable);
            return ResponseEntity.ok(reportPage);
        } catch (Exception e) {
            logger.error("Error al generar informe de contratos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoint para informe de facturas
    @GetMapping("/invoices")
    public ResponseEntity<Page<InvoiceReportRowDto>> getInvoicesReport(ReportFiltersDto filters, Pageable pageable) {
        logger.info("Solicitud para informe de facturas con filtros: {} y paginación: {}", filters, pageable);
        try {
            Page<InvoiceReportRowDto> reportPage = reportService.getInvoicesReport(filters, pageable);
            return ResponseEntity.ok(reportPage);
        } catch (Exception e) {
            logger.error("Error al generar informe de facturas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoint para alertas de contratos por vencer
    @GetMapping("/alerts/expiring-contracts")
    public ResponseEntity<Page<ExpiringContractAlertDto>> getExpiringContractsAlerts(ReportFiltersDto filters, Pageable pageable) {
        // filters podría tener 'daysUntilExpiry'
        logger.info("Solicitud para alertas de contratos por vencer con filtros: {} y paginación: {}", filters, pageable);
        try {
            Page<ExpiringContractAlertDto> reportPage = reportService.getExpiringContractsAlerts(filters, pageable);
            return ResponseEntity.ok(reportPage);
        } catch (Exception e) {
            logger.error("Error al generar alertas de contratos por vencer: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoint para alertas de facturas vencidas
    @GetMapping("/alerts/overdue-invoices")
    public ResponseEntity<Page<OverdueInvoiceAlertDto>> getOverdueInvoicesAlerts(ReportFiltersDto filters, Pageable pageable) {
        // filters podría tener 'minDaysOverdue'
        logger.info("Solicitud para alertas de facturas vencidas con filtros: {} y paginación: {}", filters, pageable);
        try {
            Page<OverdueInvoiceAlertDto> reportPage = reportService.getOverdueInvoicesAlerts(filters, pageable);
            return ResponseEntity.ok(reportPage);
        } catch (Exception e) {
            logger.error("Error al generar alertas de facturas vencidas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
