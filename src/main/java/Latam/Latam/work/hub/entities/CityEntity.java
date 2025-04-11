package Latam.Latam.work.hub.entities;

import Latam.Latam.work.hub.enums.DivistionType;
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


@Entity
@Table(name = "CIUDADES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "division_name")
    private String divisionName;   

    @Enumerated(EnumType.STRING)
    @Column(name = "division_type")
    private DivistionType divisionType;    

    @ManyToOne
    @JoinColumn(name = "country_id")
    private CountryEntity country;
}

