package Latam.Latam.work.hub.dtos.common.reports.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// Para el informe de contratos
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractReportRowDto {
    private Long contractId;
    private String spaceName;
    private String tenantName;
    private String ownerName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double amount; // Asumo RentalContractEntity.monthlyAmount es Double
    private String status;
}