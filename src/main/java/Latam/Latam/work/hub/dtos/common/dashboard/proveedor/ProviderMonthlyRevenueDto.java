package Latam.Latam.work.hub.dtos.common.dashboard.proveedor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderMonthlyRevenueDto {
    private String monthYear;
    private Double revenue;
} 