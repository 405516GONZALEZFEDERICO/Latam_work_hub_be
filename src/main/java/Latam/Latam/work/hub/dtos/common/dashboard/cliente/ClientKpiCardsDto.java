package Latam.Latam.work.hub.dtos.common.dashboard.cliente;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientKpiCardsDto {
    private long totalBookings;
    private long activeContracts;
    private long completedBookings;
    
    // Gastos diferenciados
    private Double totalGrossSpentLast30Days;    // Gastos brutos (sin descontar reembolsos)
    private Double totalNetSpentLast30Days;      // Gastos netos (descontando reembolsos)
    private Double totalRefundsLast30Days;       // Total de reembolsos recibidos
    
    // Para compatibilidad hacia atrás - será igual a totalNetSpentLast30Days
    @Deprecated
    private Double totalSpentLast30Days;
    
    private long upcomingBookings;
} 