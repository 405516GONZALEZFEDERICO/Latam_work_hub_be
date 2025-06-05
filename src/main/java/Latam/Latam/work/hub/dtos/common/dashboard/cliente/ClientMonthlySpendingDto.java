package Latam.Latam.work.hub.dtos.common.dashboard.cliente;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientMonthlySpendingDto {
    private String monthYear;
    private Double spending;
} 