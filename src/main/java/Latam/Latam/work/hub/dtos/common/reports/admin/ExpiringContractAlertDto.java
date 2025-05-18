package Latam.Latam.work.hub.dtos.common.reports.admin;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class ExpiringContractAlertDto {
    private Long contractId;
    private String spaceName;
    private String tenantName;
    private LocalDate expiryDate;
    private Long daysUntilExpiry;
}