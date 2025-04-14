package Latam.Latam.work.hub.entities;

import Latam.Latam.work.hub.enums.BookingStatus;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "RESERVAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_date")
    private LocalDateTime startDateTime; 

    @Column(name = "end_date")
    private LocalDateTime endDateTime;


    @Enumerated(EnumType.STRING)
    private BookingStatus status;         

    @Column(name = "total_amount")
    private Double totalAmount;           

    @ManyToOne
    @JoinColumn(name = "space_id")
    private SpaceEntity space;          

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;              

}
