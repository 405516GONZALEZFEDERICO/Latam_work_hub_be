package Latam.Latam.work.hub.dtos.common.dashboard.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiCardsDto {
    private long activeClients;
    private long activeProviders;
    private long publishedSpaces;
    private long reservationsThisMonth;
    
    // Ingresos diferenciados
    private Double totalGrossRevenueLast30Days;  // Ingresos brutos (sin descontar reembolsos)
    private Double totalNetRevenueLast30Days;    // Ingresos netos (descontando reembolsos)
    private Double totalRefundsLast30Days;       // Total de reembolsos
    
    // Para compatibilidad hacia atrás - será igual a totalNetRevenueLast30Days
    @Deprecated
    private Double totalRevenueLast30Days;

    private long activeContracts;           // Para "Contratos Activos"
    private long contractsExpiringSoon;     // Para "Contratos Próximos a Vencer (ej. 30 días)"
}