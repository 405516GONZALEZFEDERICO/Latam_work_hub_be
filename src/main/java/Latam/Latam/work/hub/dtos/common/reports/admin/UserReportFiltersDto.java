package Latam.Latam.work.hub.dtos.common.reports.admin;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class UserReportFiltersDto {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate; // For registration date range
    private String role; // e.g., "CLIENTE", "PROVEEDOR"
    private String status; // e.g., "Activo", "Inactivo" (for user account status)
}
