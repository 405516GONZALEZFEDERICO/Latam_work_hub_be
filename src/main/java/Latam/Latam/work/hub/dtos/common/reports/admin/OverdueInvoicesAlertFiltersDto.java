package Latam.Latam.work.hub.dtos.common.reports.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OverdueInvoicesAlertFiltersDto {
    private Integer minDaysOverdue; // Opcional si quieres que sea configurable
     private String status; // Para filtrar facturas por estado (e.g. "ISSUED") antes de chequear vencimiento
}
