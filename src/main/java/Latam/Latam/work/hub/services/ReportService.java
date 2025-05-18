package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.dtos.common.reports.admin.AdminKpiDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.BookingReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.ContractReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.ExpiringContractAlertDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.InvoiceReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.OverdueInvoiceAlertDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.ReportFiltersDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.SpaceReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.UserReportRowDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface ReportService {
    AdminKpiDto getAdminKpis(ReportFiltersDto filters);

    Page<SpaceReportRowDto> getSpacesReport(ReportFiltersDto filters, Pageable pageable);

    Page<BookingReportRowDto> getBookingsReport(ReportFiltersDto filters, Pageable pageable);

    Page<UserReportRowDto> getUsersReport(ReportFiltersDto filters, Pageable pageable);

    Page<ContractReportRowDto> getContractsReport(ReportFiltersDto filters, Pageable pageable);

    Page<InvoiceReportRowDto> getInvoicesReport(ReportFiltersDto filters, Pageable pageable);

    Page<ExpiringContractAlertDto> getExpiringContractsAlerts(ReportFiltersDto filters, Pageable pageable);

    Page<OverdueInvoiceAlertDto> getOverdueInvoicesAlerts(ReportFiltersDto filters, Pageable pageable);
}
