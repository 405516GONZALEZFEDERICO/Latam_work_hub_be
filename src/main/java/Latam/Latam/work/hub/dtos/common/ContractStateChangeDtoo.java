package Latam.Latam.work.hub.dtos.common;

import Latam.Latam.work.hub.enums.ContractStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractStateChangeDtoo {
    private Long id;
    private Long contractId;
    private ContractStatus previousStatus;
    private ContractStatus newStatus;
    private LocalDateTime changeDate;
    private String changeReason;
} 