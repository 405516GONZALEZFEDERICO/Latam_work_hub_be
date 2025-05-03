package Latam.Latam.work.hub.enums;

/**
 * Estados posibles para una reserva
 */
public enum BookingStatus {
    /**
     * Reserva creada pero pendiente de pago
     */
    PENDING_PAYMENT,

    /**
     * Reserva pagada pero aún no ha llegado la fecha de inicio
     */
    CONFIRMED,

    /**
     * Reserva activa (fecha actual está dentro del período de reserva)
     */
    ACTIVE,

    /**
     * Reserva finalizada (fecha de finalización ha pasado)
     */
    COMPLETED,

    /**
     * Reserva cancelada
     */
    CANCELED
}