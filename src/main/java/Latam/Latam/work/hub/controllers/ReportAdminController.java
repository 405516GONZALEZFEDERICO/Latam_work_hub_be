package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.dtos.common.reports.admin.BookingReportFiltersDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.BookingReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.ContractReportFiltersDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.ContractReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.ExpiringContractAlertDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.ExpiringContractsAlertFiltersDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.InvoiceReportFiltersDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.InvoiceReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.OverdueInvoiceAlertDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.OverdueInvoicesAlertFiltersDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.SpaceReportFiltersDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.SpaceReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.UserReportFiltersDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.UserReportRowDto;
import Latam.Latam.work.hub.services.ReportAdminService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/reports-admin")
@RequiredArgsConstructor
public class ReportAdminController {
    private static final Logger logger = LoggerFactory.getLogger(ReportAdminController.class);
    private final ReportAdminService reportAdminService;

    @GetMapping("/spaces")
    public ResponseEntity<Page<SpaceReportRowDto>> getSpacesReport(
            @RequestParam(required = false) String status,
            Pageable pageable
    ) {

        SpaceReportFiltersDto filters = new SpaceReportFiltersDto();
        filters.setStatus(status);

        logger.info("Solicitud para informe de espacios con estado: '{}' y paginación: {}", status, pageable);

        try {
            Page<SpaceReportRowDto> reportPage = reportAdminService.getSpacesReport(filters, pageable);
            logger.info("Informe generado exitosamente. Total elementos: {}, página: {}",
                    reportPage.getTotalElements(), reportPage.getNumber());
            return ResponseEntity.ok(reportPage);
        } catch (Exception e) {
            logger.error("Error al generar informe de espacios: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/bookings")
    public ResponseEntity<Page<BookingReportRowDto>> getBookingsReport(BookingReportFiltersDto filters, Pageable pageable) {
        logger.info("Solicitud para informe de reservas con filtros: {} y paginación: {}", filters, pageable);
        try {
            Page<BookingReportRowDto> reportPage = reportAdminService.getBookingsReport(filters, pageable);
            return ResponseEntity.ok(reportPage);
        } catch (Exception e) {
            logger.error("Error al generar informe de reservas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserReportRowDto>> getUsersReport(UserReportFiltersDto filters, Pageable pageable) {
        logger.info("Solicitud para informe de usuarios con filtros: {} y paginación: {}", filters, pageable);
        try {
            Page<UserReportRowDto> reportPage = reportAdminService.getUsersReport(filters, pageable);
            return ResponseEntity.ok(reportPage);
        } catch (Exception e) {
            logger.error("Error al generar informe de usuarios: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/contracts")
    public ResponseEntity<Page<ContractReportRowDto>> getContractsReport(ContractReportFiltersDto filters, Pageable pageable) {
        logger.info("Solicitud para informe de contratos con filtros: {} y paginación: {}", filters, pageable);
        try {
            Page<ContractReportRowDto> reportPage = reportAdminService.getContractsReport(filters, pageable);
            return ResponseEntity.ok(reportPage);
        } catch (Exception e) {
            logger.error("Error al generar informe de contratos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/invoices")
    public ResponseEntity<Page<InvoiceReportRowDto>> getInvoicesReport(InvoiceReportFiltersDto filters, Pageable pageable) {
        logger.info("Solicitud para informe de facturas con filtros: {} y paginación: {}", filters, pageable);
        try {
            Page<InvoiceReportRowDto> reportPage = reportAdminService.getInvoicesReport(filters, pageable);
            return ResponseEntity.ok(reportPage);
        } catch (Exception e) {
            logger.error("Error al generar informe de facturas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/alerts/expiring-contracts")
    public ResponseEntity<Page<ExpiringContractAlertDto>> getExpiringContractsAlerts(ExpiringContractsAlertFiltersDto filters, Pageable pageable) {
        logger.info("Solicitud para alertas de contratos por vencer con filtros: {} y paginación: {}", filters, pageable);
        try {
            Page<ExpiringContractAlertDto> reportPage = reportAdminService.getExpiringContractsAlerts(filters, pageable);
            return ResponseEntity.ok(reportPage);
        } catch (Exception e) {
            logger.error("Error al generar alertas de contratos por vencer: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/alerts/overdue-invoices")
    // Si OverdueInvoicesAlertFiltersDto está vacío o no lo necesitas, puedes quitarlo
    public ResponseEntity<Page<OverdueInvoiceAlertDto>> getOverdueInvoicesAlerts(OverdueInvoicesAlertFiltersDto filters, Pageable pageable) {
        logger.info("Solicitud para alertas de facturas vencidas con filtros: {} y paginación: {}", filters, pageable);
        try {
            Page<OverdueInvoiceAlertDto> reportPage = reportAdminService.getOverdueInvoicesAlerts(filters, pageable);
            return ResponseEntity.ok(reportPage);
        } catch (Exception e) {
            logger.error("Error al generar alertas de facturas vencidas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}