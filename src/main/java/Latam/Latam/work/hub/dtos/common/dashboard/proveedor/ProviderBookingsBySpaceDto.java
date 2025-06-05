package Latam.Latam.work.hub.dtos.common.dashboard.proveedor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderBookingsBySpaceDto {
    private Long spaceId;
    private String spaceName;
    private String spaceType;
    private String clientName;
    private String clientEmail;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private Double amount;
} 