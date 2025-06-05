package Latam.Latam.work.hub.dtos.common.dashboard.cliente;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientBookingsByTypeDto {
    private String spaceTypeName;
    private Long bookingCount;
} 