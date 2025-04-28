package Latam.Latam.work.hub.dtos.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingDto {
    private String uid;
    private Long spaceId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private LocalTime initHour;
    private LocalTime endHour;

    private Integer counterPersons;
    private Double totalAmount;
}
