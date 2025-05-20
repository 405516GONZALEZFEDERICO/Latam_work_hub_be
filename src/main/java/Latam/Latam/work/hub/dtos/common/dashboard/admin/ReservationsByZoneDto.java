package Latam.Latam.work.hub.dtos.common.dashboard.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationsByZoneDto {
    private String zoneName;
    private Long reservationCount;
}
