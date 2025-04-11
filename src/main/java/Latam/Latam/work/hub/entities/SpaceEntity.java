package Latam.Latam.work.hub.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "ESPACIOS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SpaceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private Integer capacity;
    private Double area;         
    @Column(name = "price_per_hour")
    private Double pricePerHour;
    @Column(name = "price_per_day")
    private Double pricePerDay;
    @Column(name = "price_per_month")
    private Double pricePerMonth;
    private Boolean active;
    private Boolean available;
    @Column(name = "url_img")
    private String urlImg;
    @Lob
    private String amenities;     

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity owner;  
    @ManyToOne
    @JoinColumn(name = "type_id")
    private SpaceTypeEntity type;
    @ManyToOne
    @JoinColumn(name = "address_id")
    private AddressEntity address;

}


