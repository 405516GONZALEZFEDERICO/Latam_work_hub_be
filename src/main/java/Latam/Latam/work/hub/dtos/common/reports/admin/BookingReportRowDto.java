package Latam.Latam.work.hub.dtos.common.reports.admin;


import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
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

    // Constructor opcional para facilitar la creación en el servicio
    public BookingReportRowDto(Long bookingId, String spaceName, String providerName, String clientName,
                               LocalDateTime startDate, LocalDateTime endDate, Long durationHours,
                               String status, Double amount) {
        this.bookingId = bookingId;
        this.spaceName = spaceName;
        this.providerName = providerName;
        this.clientName = clientName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.durationHours = durationHours;
        this.status = status;
        this.amount = amount;
    }
}