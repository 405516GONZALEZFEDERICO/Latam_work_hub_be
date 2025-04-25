package Latam.Latam.work.hub.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@Entity
@Table(name = "SERVICIOS_EXTRA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AmenityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "price", nullable = true)
    private Double price;

    private String name;

    private Boolean active = true;
    @ManyToMany(mappedBy = "amenities")
    private List<SpaceEntity> spaces;
}
