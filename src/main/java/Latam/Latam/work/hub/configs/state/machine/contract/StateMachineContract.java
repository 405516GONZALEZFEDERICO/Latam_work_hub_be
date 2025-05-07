package Latam.Latam.work.hub.configs.state.machine.contract;

import Latam.Latam.work.hub.entities.RentalContractEntity;
import Latam.Latam.work.hub.enums.ContractStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class StateMachineContract {

    private final Map<ContractStatus, Set<ContractStatus>> validTransitions = new HashMap<>();

    public StateMachineContract() {
        validTransitions.put(ContractStatus.ACTIVE,
                Set.of(ContractStatus.TERMINATED, ContractStatus.EXPIRED, ContractStatus.RENEWAL, ContractStatus.CANCELLED));
        validTransitions.put(ContractStatus.DRAFT,
                Set.of(ContractStatus.PENDING, ContractStatus.CANCELLED));
        validTransitions.put(ContractStatus.PENDING,
                Set.of(ContractStatus.ACTIVE, ContractStatus.CANCELLED));
        validTransitions.put(ContractStatus.RENEWAL,
                Set.of(ContractStatus.ACTIVE, ContractStatus.EXPIRED));
        validTransitions.put(ContractStatus.EXPIRED,
                Collections.emptySet());
        validTransitions.put(ContractStatus.TERMINATED,
                Collections.emptySet());
        validTransitions.put(ContractStatus.CANCELLED,
                Collections.emptySet());
    }

    /**
     * Verifica si una transición de estado es válida
     * @param currentStatus Estado actual
     * @param newStatus Estado propuesto
     * @return true si la transición es válida
     */
    public boolean isValidTransition(ContractStatus currentStatus, ContractStatus newStatus) {
        // Un estado siempre puede permanecer igual
        if (currentStatus == newStatus) {
            return true;
        }

        Set<ContractStatus> allowedTransitions = validTransitions.getOrDefault(currentStatus, Collections.emptySet());
        return allowedTransitions.contains(newStatus);
    }

    /**
     * Intenta realizar una transición de estado
     * @param contract Contrato a actualizar
     * @param newStatus Nuevo estado propuesto
     * @throws IllegalStateException si la transición no es válida
     */
    public void transition(RentalContractEntity contract, ContractStatus newStatus) {
        ContractStatus currentStatus = contract.getContractStatus();

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new IllegalStateException(
                    String.format("Transición de estado inválida: %s → %s", currentStatus, newStatus)
            );
        }

        contract.setContractStatus(newStatus);
    }
}