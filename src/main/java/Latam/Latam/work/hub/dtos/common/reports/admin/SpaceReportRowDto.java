package Latam.Latam.work.hub.dtos.common.reports.admin;

import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class SpaceReportRowDto {
    private Long spaceId;
    private String spaceName;
    private String providerName;
    private Long bookingCount; // Se llenará en el servicio
    private Double revenueGenerated; // Se llenará en el servicio (y se calculará)
    private String status; // Se determinará en el servicio

    public SpaceReportRowDto(Long spaceId, String spaceName, String providerName, String status) {
        this.spaceId = spaceId;
        this.spaceName = spaceName;
        this.providerName = providerName;
        this.status = status;
        this.bookingCount = 0L; // Default
        this.revenueGenerated = 0.0; // Default
    }

    public void setBookingCount(Long bookingCount) {
        this.bookingCount = bookingCount != null ? bookingCount : 0L;
    }

    public void setRevenueGenerated(Double revenueGenerated) {
        this.revenueGenerated = revenueGenerated != null ? revenueGenerated : 0.0;
    }

}