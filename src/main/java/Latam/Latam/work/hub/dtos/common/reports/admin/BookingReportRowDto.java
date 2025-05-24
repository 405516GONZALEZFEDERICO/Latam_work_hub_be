package Latam.Latam.work.hub.dtos.common.reports.admin;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingReportRowDto {
    private Long bookingId;
    private String spaceName;
    private String providerName; // Propietario del espacio
    private String clientName;   // Usuario que hizo la reserva
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long durationHours;
    private String status;       // Estado de la reserva (e.g., "COMPLETED", "PENDING", "CANCELLED")
    private Double amount;       // Monto total de la reserva

}