package Latam.Latam.work.hub.entities;

import Latam.Latam.work.hub.enums.ContractStatus;
import Latam.Latam.work.hub.services.Billable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Table(name = "CONTRATOS_ALQUILER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class RentalContractEntity implements Billable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_date")
    private LocalDate startDate;         

    @Column(name = "end_date")
    private LocalDate endDate;            

    @Column(name = "monthly_amount")
    private Double monthlyAmount;         

    @Column(name = "deposit_amount")
    private Double depositAmount;

    @Column(name = "auto_renewal")
    private boolean autoRenewal = false;

    @Column(name = "renewal_months")
    private Integer renewalMonths;

    @Column(name = "duration_months")
    private Double durationMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_status")
    private ContractStatus contractStatus;

    @ManyToOne
    @JoinColumn(name = "space_id")
    private SpaceEntity space;            

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity tenant ;

    @Column(name = "deposit_refounded")
    private boolean depositRefounded = false;

    @Column(name = "deposit_refound_date")
    private LocalDateTime depositRefoundDate;

    @Transient
    private Double amount;

    @Override
    public Double getAmount() {
        return monthlyAmount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}