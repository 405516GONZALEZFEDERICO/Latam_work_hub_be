package Latam.Latam.work.hub.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "SERVICIOS_EXTRA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class AmenityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "precio", nullable = true)
    private Double precio;

    private String name;

    @Column(name = "reservation_required")
    private Boolean reservationRequired;

    private Boolean active = true;
}
