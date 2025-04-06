package Latam.Latam.work.hub.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "USERS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String password;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(name = "firebase_uid")
    private String firebaseUid;
    @Column(name = "name")
    private String name;
    @Column(name = "last_password_update")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime lastPasswordUpdateDate;
}
