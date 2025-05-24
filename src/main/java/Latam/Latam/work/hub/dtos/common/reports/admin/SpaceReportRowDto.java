    package Latam.Latam.work.hub.dtos.common.reports.admin;

    import lombok.AllArgsConstructor;
    import lombok.Builder;
    import lombok.Data;
    import lombok.NoArgsConstructor;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class SpaceReportRowDto {
        private Long spaceId;
        private String name;
        private String owner;
        private Long bookingCount; // Se llenará en el servicio
        private Long rentalCount;
        private Double revenueGenerated; // Se llenará en el servicio (y se calculará)
        private String status; // Se determinará en el servicio
    }