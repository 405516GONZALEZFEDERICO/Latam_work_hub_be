package Latam.Latam.work.hub.entities;
import Latam.Latam.work.hub.enums.BookingStatus;
import Latam.Latam.work.hub.enums.BookingType;
import Latam.Latam.work.hub.services.Billable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "RESERVAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingEntity implements Billable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "init_hour")
    private LocalTime initHour;

    @Column(name = "end_hour")
    private LocalTime endHour;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type")
    private BookingType bookingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BookingStatus status;

    private Boolean active;

    @Column(name = "counter_persons")
    private Integer counterPersons;

    @Column(name = "total_amount")
    private Double totalAmount;
    
    @Column(name = "refund_amount")
    private Double refundAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private SpaceEntity space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Override
    public Double getAmount() {
        return this.getTotalAmount();
    }
}
