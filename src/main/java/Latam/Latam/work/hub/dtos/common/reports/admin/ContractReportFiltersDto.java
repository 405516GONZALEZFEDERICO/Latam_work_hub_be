package Latam.Latam.work.hub.dtos.common.reports.admin;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class ContractReportFiltersDto {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // Use LocalDate for contract dates
    private LocalDate contractStartDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate contractEndDate;
    private Long tenantId; // (clientId)
    private Long ownerId;  // (providerId)
    private String status; // ContractStatus as String
}
