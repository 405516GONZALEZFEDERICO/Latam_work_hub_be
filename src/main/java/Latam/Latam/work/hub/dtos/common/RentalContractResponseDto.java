package Latam.Latam.work.hub.dtos.common;


import Latam.Latam.work.hub.enums.ContractStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalContractResponseDto {
    private Long id;
    // Datos del espacio
    private Long spaceId;
    private String spaceName;
    private String spaceAddress;
    private String spaceDescription;
    private Double spaceArea;
    private Integer spaceCapacity;
    private Double pricePerHour;
    private Double pricePerDay;
    private Double pricePerMonth;
    private String spaceType;
    private String cityName;
    private String countryName;
    // Datos del contrato
    private String ownerName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double monthlyAmount;
    private Double depositAmount;
    private ContractStatus status;
    private Boolean hasCurrentInvoicePending;
    private String currentInvoiceNumber;
    private LocalDate currentInvoiceDueDate;
}