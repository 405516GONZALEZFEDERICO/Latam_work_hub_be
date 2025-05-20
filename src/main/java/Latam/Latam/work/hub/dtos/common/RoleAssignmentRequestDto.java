package Latam.Latam.work.hub.dtos.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleAssignmentRequestDto {
    private String uid;
    private String roleName;
    private String adminKey;
}
