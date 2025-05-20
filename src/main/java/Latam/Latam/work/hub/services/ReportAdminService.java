package Latam.Latam.work.hub.services;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface ReportAdminService {

    Page<SpaceReportRowDto> getSpacesReport(SpaceReportFiltersDto filters, Pageable pageable);

    Page<BookingReportRowDto> getBookingsReport(BookingReportFiltersDto filters, Pageable pageable);

    Page<UserReportRowDto> getUsersReport(UserReportFiltersDto filters, Pageable pageable);

    Page<ContractReportRowDto> getContractsReport(ContractReportFiltersDto filters, Pageable pageable);

    Page<InvoiceReportRowDto> getInvoicesReport(InvoiceReportFiltersDto filters, Pageable pageable);

    Page<ExpiringContractAlertDto> getExpiringContractsAlerts(ExpiringContractsAlertFiltersDto filters, Pageable pageable);

    Page<OverdueInvoiceAlertDto> getOverdueInvoicesAlerts(OverdueInvoicesAlertFiltersDto filters, Pageable pageable);
}
