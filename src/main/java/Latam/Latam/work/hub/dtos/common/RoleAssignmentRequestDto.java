package Latam.Latam.work.hub.dtos.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleAssignmentRequestDto {
    private String uid;
    private String roleName;
}
