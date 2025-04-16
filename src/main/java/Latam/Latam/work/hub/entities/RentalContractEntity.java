package Latam.Latam.work.hub.entities;

import Latam.Latam.work.hub.enums.ContractStatus;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Table(name = "CONTRATOS_ALQUILER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class RentalContractEntity {
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

    @Column(name = "duration_months")
    private Double durationMonths;

    @Enumerated(EnumType.STRING)
    private ContractStatus status;

    @Column(name = "grace_period")
    private Integer gracePeriod;

    @ManyToOne
    @JoinColumn(name = "space_id")
    private SpaceEntity space;            

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity tenant ;              

}