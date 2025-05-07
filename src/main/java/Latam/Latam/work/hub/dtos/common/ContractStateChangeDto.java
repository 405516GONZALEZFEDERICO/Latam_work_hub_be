package Latam.Latam.work.hub.dtos.common;

import Latam.Latam.work.hub.enums.ContractStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractStateChangeDto {
    private Long id;
    private Long contractId;
    private ContractStatus previousStatus;
    private ContractStatus newStatus;
    private LocalDateTime changeDate;
    private String changeReason;
} 