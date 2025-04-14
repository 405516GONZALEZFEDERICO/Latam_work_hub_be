package Latam.Latam.work.hub.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "EMPRESAS")
public class CompanyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "tax_id")
    private String taxId;

    private String phone;

    private String email;

    @Column(name = "web_site")
    private String website;

    private String description;

    @Column(name = "registration_date")
    private LocalDateTime registrationDate;

    private Boolean active;

    @Column(name = "contact_person")
    private String contactPerson;

    @ManyToOne
    @JoinColumn(name = "country_id")
    private CountryEntity country;


    @ElementCollection
    @CollectionTable(name = "EMPRESA_PAIS_OPERACION",
            joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "country_code")
    private Set<String> operatingCountries;

}
