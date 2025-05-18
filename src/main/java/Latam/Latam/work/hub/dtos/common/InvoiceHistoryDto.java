package Latam.Latam.work.hub.dtos.common;

import Latam.Latam.work.hub.enums.InvoiceStatus;
import Latam.Latam.work.hub.enums.InvoiceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceHistoryDto {
    private Long id;
    private String invoiceNumber;
    private BigDecimal totalAmount;
    private LocalDateTime issueDate;
    private LocalDateTime dueDate;
    private InvoiceStatus status;
    private InvoiceType type;
    private String description;
}