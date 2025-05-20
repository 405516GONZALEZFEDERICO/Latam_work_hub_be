package Latam.Latam.work.hub.dtos.common.reports.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ExpiringContractsAlertFiltersDto {
    private Integer daysUntilExpiry; // Default if not provided
}
