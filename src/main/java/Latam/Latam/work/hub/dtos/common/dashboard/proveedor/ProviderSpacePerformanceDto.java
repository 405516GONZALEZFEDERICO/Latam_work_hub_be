package Latam.Latam.work.hub.dtos.common.dashboard.proveedor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderSpacePerformanceDto {
    private String spaceName;
    private long totalBookings;
    private long totalContracts;
    private Double totalRevenue;
    private Double occupancyRate;
} 