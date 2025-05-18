package Latam.Latam.work.hub.services.impl;

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
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ReportServiceImpl implements ReportService {
    @Override
    public AdminKpiDto getAdminKpis(ReportFiltersDto filters) {
        return null;
    }

    @Override
    public Page<SpaceReportRowDto> getSpacesReport(ReportFiltersDto filters, Pageable pageable) {
        return null;
    }

    @Override
    public Page<BookingReportRowDto> getBookingsReport(ReportFiltersDto filters, Pageable pageable) {
        return null;
    }

    @Override
    public Page<UserReportRowDto> getUsersReport(ReportFiltersDto filters, Pageable pageable) {
        return null;
    }

    @Override
    public Page<ContractReportRowDto> getContractsReport(ReportFiltersDto filters, Pageable pageable) {
        return null;
    }

    @Override
    public Page<InvoiceReportRowDto> getInvoicesReport(ReportFiltersDto filters, Pageable pageable) {
        return null;
    }

    @Override
    public Page<ExpiringContractAlertDto> getExpiringContractsAlerts(ReportFiltersDto filters, Pageable pageable) {
        return null;
    }

    @Override
    public Page<OverdueInvoiceAlertDto> getOverdueInvoicesAlerts(ReportFiltersDto filters, Pageable pageable) {
        return null;
    }
}
