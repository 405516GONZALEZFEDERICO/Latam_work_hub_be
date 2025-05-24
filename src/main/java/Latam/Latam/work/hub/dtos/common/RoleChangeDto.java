    package Latam.Latam.work.hub.dtos.common;

    import lombok.AllArgsConstructor;
    import lombok.Builder;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Data
    public class RoleChangeDto {
        private String uid;
        private String roleName;
    }
