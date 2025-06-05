package Latam.Latam.work.hub.dtos.common.dashboard.proveedor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderOccupancyDto {
    private String monthYear;
    private Double occupancyPercentage;
    private long totalBookings;
    private long totalContracts;
} 