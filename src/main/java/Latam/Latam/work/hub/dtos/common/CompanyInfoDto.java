package Latam.Latam.work.hub.dtos.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyInfoDto {
    private String legalName;
    private String name;
    private String taxId;
    private String phone;
    private String email;
    private String website;
    private String contactPerson;
    private Integer country;
    private String providerType;
}
