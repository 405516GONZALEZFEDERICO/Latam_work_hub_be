package Latam.Latam.work.hub.dtos.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalContractDto {
    private Long spaceId;
    private String uid;
    private LocalDate startDate;
    private Integer durationMonths;
    private Double monthlyAmount;
    private Double depositAmount;
}
