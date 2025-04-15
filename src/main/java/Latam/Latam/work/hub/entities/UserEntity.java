package Latam.Latam.work.hub.entities;

import Latam.Latam.work.hub.enums.DocumentType;
import Latam.Latam.work.hub.enums.ProviderType;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "USUARIOS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true,nullable = false)
    private String email;

    private String password;


    @Column(name = "photo_url")
    private String photoUrl;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "firebase_uid")
    private String firebaseUid;

    @Column(name = "full_name")
    private String name;

    @Column(name = "registration_date")
    private LocalDateTime registrationDate;

    @Column(name = "last_access")
    private LocalDateTime lastAccess;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type")
    private DocumentType documentType;

    @Column(name = "document_number")
    private String documentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type")
    private ProviderType providerType;

    @ManyToOne
    @JoinColumn(name = "address_id")
    private AddressEntity address;


    @Column(name = "job_tittle")
    private String jobTitle;

    private String department;


    @ManyToOne
    @JoinColumn(name = "company_id")
    private CompanyEntity company;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private RoleEntity role;

    public boolean hasRole(String roleName) {
        return role != null && role.getName().equals(roleName);
    }

}

