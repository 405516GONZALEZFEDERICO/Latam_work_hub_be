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
public class CompleteUserDataDto {
    private String photoUrl;
    private String name;
    private LocalDate birthDate;
    private String documentNumber;
    private String email;
    private DocumentType documentType;
    private String jobTitle;
    private String department;
}
