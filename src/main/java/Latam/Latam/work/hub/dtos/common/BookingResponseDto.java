package Latam.Latam.work.hub.dtos.common;

import Latam.Latam.work.hub.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDto {
    private Long id;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalTime initHour;
    private LocalTime endHour;
    private String bookingType;
    private BookingStatus status;
    private Integer counterPersons;
    private Double totalAmount;
    // Datos del espacio
    private Long spaceId;
    private String spaceName;
    private String spaceAddress;
    private String spaceType;
    private String cityName;
    private String countryName;
}
