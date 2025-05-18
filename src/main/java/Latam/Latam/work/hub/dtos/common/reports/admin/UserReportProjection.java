package Latam.Latam.work.hub.dtos.common.reports.admin;

import java.time.LocalDateTime;

public interface UserReportProjection {
    Long getId();
    String getName();
    String getEmail();
    String getRoleName();
    Long getCountValue();
    Double getEarnings();
    LocalDateTime getRegistrationDate();
    String getStatus();
} 