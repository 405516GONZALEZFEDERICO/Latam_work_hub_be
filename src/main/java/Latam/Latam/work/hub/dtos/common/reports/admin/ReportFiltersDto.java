package Latam.Latam.work.hub.dtos.common.reports.admin;

import lombok.Data;
import lombok.NoArgsConstructor; // Es bueno tenerlo aunque no se use directamente
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
// También podrías necesitar LocalDate para algunos filtros
import java.time.LocalDate;

@Data
@NoArgsConstructor // Lombok puede generar un constructor sin argumentos
public class ReportFiltersDto {

    // Filtros de Fecha Genéricos (pueden aplicar a diferentes campos según el reporte)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) // Para que Spring MVC parsee bien desde el request (ej. YYYY-MM-DDTHH:MM:SS)
    private LocalDateTime startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    // Filtros de Fecha específicos si necesitas diferenciar (ej. para contratos que usan LocalDate)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // Para YYYY-MM-DD
    private LocalDate filterDateStart; // Podrías usarlo para fecha de inicio de contrato

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate filterDateEnd;   // Podrías usarlo para fecha de fin de contrato

    // Filtros por IDs
    private Long clientId;    // Podría ser tenantId en algunos contextos
    private Long providerId;  // Podría ser ownerId en algunos contextos
    private Long spaceId;
    private Long userId;      // Genérico para cualquier usuario

    // Filtros por Estado (String para flexibilidad, se convierte a Enum en el servicio)
    private String status;    // Ej: "COMPLETED", "ACTIVE", "PAID", "Disponible"

    // Filtro por Rol (para reporte de usuarios)
    private String role;      // Ej: "CLIENTE", "PROVEEDOR"

    // Filtros específicos para Alertas (opcional, o manejarlos con los genéricos)
    private Integer daysUntilExpiry; // Para contratos por vencer
    private Integer minDaysOverdue;  // Para facturas vencidas

    // Otros filtros que puedas necesitar según el reporte
    // private String paymentStatus;
    // private String bookingType;

    // Constructor con todos los argumentos podría ser útil para pruebas,
    // pero para el request mapping, los setters son suficientes.
    // Lombok @AllArgsConstructor podría generarlo si quitas @NoArgsConstructor
    // o si defines los dos.

    // Getters y Setters son generados por @Data de Lombok
}