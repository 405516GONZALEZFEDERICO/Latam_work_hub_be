package Latam.Latam.work.hub.dtos.common.reports.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Para el informe de usuarios
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReportRowDto {
    private Long userId;
    private String email;
    private String name;
    private String role;
    private String status;
    private LocalDateTime registrationDate;
    private LocalDateTime lastLoginDate;

    // Estadísticas para proveedores
    private Long totalSpaces;
    private Long activeContracts;
    private Double totalRevenue;

    // Estadísticas para clientes
    private Long totalBookings;
    private Double totalSpending;
}