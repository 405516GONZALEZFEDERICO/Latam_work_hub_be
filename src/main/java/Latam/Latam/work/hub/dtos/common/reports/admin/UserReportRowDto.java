package Latam.Latam.work.hub.dtos.common.reports.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Para el informe de usuarios
@Data
@NoArgsConstructor
public class UserReportRowDto {
    private Long userId;
    private String userName;
    private String email;
    private String role;
    private Long entityCount; // N° de reservas (cliente) o N° de espacios (proveedor)
    private Double incomeGenerated; // Para proveedores
    private LocalDateTime registrationDate;
    private String status; // Activo/Inactivo

    // Constructor para el servicio
    public UserReportRowDto(Long userId, String userName, String email, String role, LocalDateTime registrationDate, String status) {
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.role = role;
        this.registrationDate = registrationDate;
        this.status = status;
        this.entityCount = 0L;
        this.incomeGenerated = 0.0;
    }

    public void setEntityCount(Long entityCount) {
        this.entityCount = entityCount != null ? entityCount : 0L;
    }

    public void setIncomeGenerated(Double incomeGenerated) {
        this.incomeGenerated = incomeGenerated != null ? incomeGenerated : 0.0;
    }
}