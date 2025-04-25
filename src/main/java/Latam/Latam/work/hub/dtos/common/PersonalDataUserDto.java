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
public class PersonalDataUserDto {
 private String name;
 private LocalDate birthDate;
 private String documentNumber;
 private DocumentType documentType;
 private String jobTitle;
 private String department;
}
