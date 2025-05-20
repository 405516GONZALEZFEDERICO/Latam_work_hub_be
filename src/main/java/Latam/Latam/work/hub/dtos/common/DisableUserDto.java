package Latam.Latam.work.hub.dtos.common;
import Latam.Latam.work.hub.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisableUserDto {
    private Long id;
    private String name;
    private String email;
    private String firebaseUid;
    private LocalDate birthDay;
    private DocumentType documentType;
    private String documentNumber;
    private String jobTitle;
    private String role;
    private String department;
    private boolean enabled;
}
