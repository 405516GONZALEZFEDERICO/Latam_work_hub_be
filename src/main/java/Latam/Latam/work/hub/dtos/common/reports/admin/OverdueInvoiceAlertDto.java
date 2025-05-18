package Latam.Latam.work.hub.dtos.common.reports.admin;

import lombok.Data;
import lombok.NoArgsConstructor;
// import java.math.BigDecimal;
import java.time.LocalDateTime; // O LocalDate

@Data
@NoArgsConstructor
public class OverdueInvoiceAlertDto {
    private Long invoiceId;
    private String clientName;
    private LocalDateTime dueDate; // O LocalDate
    private Long daysOverdue;
    private Double overdueAmount; // Asumo que es totalAmount si no hay paidAmount
}