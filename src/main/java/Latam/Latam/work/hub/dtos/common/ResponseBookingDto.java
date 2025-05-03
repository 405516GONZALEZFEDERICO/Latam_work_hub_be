package Latam.Latam.work.hub.dtos.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseBookingDto {
    private Long id;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalTime initHour;
    private LocalTime endHour;
    private String bookingType;
    private Boolean active;
}
