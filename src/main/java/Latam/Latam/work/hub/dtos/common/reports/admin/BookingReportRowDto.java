package Latam.Latam.work.hub.dtos.common.reports.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor // Asume que la query JPQL puede llenar esto directamente
public class BookingReportRowDto {
    private Long bookingId;
    private String spaceName;
    private String clientName;
    private String providerName; // Dueño del espacio
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long durationHours; // Calculado en el servicio después
    private String status;
    private Double amount; // O BigDecimal, según tu entidad BookingEntity.totalAmount
}