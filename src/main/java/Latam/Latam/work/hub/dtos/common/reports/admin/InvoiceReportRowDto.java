package Latam.Latam.work.hub.dtos.common.reports.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceReportRowDto {
    private Long invoiceId;
    private String clientName;
    private LocalDateTime issueDate;
    private LocalDateTime dueDate;
    private Double totalAmount;
    private Double paidAmount;    // Se llenará, o será 0.0 si no existe en la entidad
    private Double pendingAmount; // Se calculará
    private String status;
    private String invoiceType;
}