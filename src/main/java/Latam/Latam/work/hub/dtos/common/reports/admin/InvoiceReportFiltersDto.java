package Latam.Latam.work.hub.dtos.common.reports.admin;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class InvoiceReportFiltersDto {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate; // For issue date range
    private Long clientId;
    private String status; // InvoiceStatus as String
}
