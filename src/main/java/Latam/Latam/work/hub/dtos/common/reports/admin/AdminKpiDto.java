package Latam.Latam.work.hub.dtos.common.reports.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminKpiDto {
    private long totalActiveClients;
    private long totalActiveProviders;
    private long totalAvailableSpaces;
    private long totalBookingsPeriod; // Para un período específico
    private BigDecimal totalRevenuePeriod; // Para un período específico
    // Puedes agregar más KPIs aquí
}
