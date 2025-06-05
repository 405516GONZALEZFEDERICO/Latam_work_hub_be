package Latam.Latam.work.hub.dtos.common.dashboard.proveedor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderKpiCardsDto {
    private long totalSpaces;
    private long activeContracts;
    private long reservationsThisMonth;
    
    // Ingresos diferenciados
    private Double totalGrossRevenueLast30Days;  // Ingresos brutos (sin descontar reembolsos)
    private Double totalNetRevenueLast30Days;    // Ingresos netos (descontando reembolsos)
    private Double totalRefundsLast30Days;       // Total de reembolsos
    
    // Para compatibilidad hacia atrás - será igual a totalNetRevenueLast30Days
    @Deprecated
    private Double totalRevenueLast30Days;
    
    private long spacesOccupied;
    private long spacesAvailable;
    private Double occupancyRate;
} 