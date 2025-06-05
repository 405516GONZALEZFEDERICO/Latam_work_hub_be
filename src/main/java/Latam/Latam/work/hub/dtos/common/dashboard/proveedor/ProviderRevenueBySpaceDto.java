package Latam.Latam.work.hub.dtos.common.dashboard.proveedor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderRevenueBySpaceDto {
    private Long spaceId;
    private String spaceName;
    private String spaceType;
    private Double bookingRevenue;
    private Double contractRevenue;
    private Double totalRevenue;
    private long totalBookings;
    private long totalContracts;
} 