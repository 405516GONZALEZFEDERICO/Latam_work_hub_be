package Latam.Latam.work.hub.dtos.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingInvoiceDto {
    private Long id;
    private String description;
    private Double totalAmount;
    private LocalDateTime issueDate;
    private LocalDateTime dueDate;
    private String status;
    private String invoiceNumber;
}