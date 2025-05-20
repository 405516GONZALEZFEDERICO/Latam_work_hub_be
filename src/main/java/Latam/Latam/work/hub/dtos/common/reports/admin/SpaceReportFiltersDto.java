package Latam.Latam.work.hub.dtos.common.reports.admin;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class SpaceReportFiltersDto {
    private Long providerId;
    private String status; // e.g., "Disponible", "Ocupado", "Inactivo"
}
