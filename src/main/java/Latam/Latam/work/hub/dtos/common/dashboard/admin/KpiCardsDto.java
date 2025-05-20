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
    private Double totalRevenueLast30Days;

    private long activeContracts;           // Para "Contratos Activos"
    private long contractsExpiringSoon;     // Para "Contratos Próximos a Vencer (ej. 30 días)"
}