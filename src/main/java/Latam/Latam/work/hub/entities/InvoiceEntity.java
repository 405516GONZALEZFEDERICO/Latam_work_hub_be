package Latam.Latam.work.hub.entities;

import Latam.Latam.work.hub.enums.InvoiceStatus;
import Latam.Latam.work.hub.enums.InvoiceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "FACTURAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "invoice_type")
    @Enumerated(EnumType.STRING)
    private InvoiceType type;

    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;        

  
    @OneToOne
    @JoinColumn(name = "booking_id")
    private BookingEntity booking;

    @OneToOne
    @JoinColumn(name = "rental_contract_id")
    private RentalContractEntity rentalContract; 


}

