package Latam.Latam.work.hub.enums;

public enum ContractStatus {
    DRAFT,              // Initial state when contract is being prepared
    PENDING, // Contract created but initial payment not made
    CONFIRMED,
    ACTIVE,             // Contract is active and in force
    TERMINATED,         // Contract terminated before natural end (e.g., breach)
    EXPIRED,            // Contract reached its natural end date
    CANCELLED,          // Contract canceled (before becoming active)
    RENEWAL     // Optional: Contract in process of being renewed
}