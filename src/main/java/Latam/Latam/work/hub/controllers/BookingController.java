package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.dtos.common.BookingDto;
import Latam.Latam.work.hub.dtos.common.BookingResponseDto;
import Latam.Latam.work.hub.enums.BookingStatus;
import Latam.Latam.work.hub.services.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/booking")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<String> createBooking(@RequestBody BookingDto bookingDto) {
        return ResponseEntity.ok(bookingService.createBooking(bookingDto));
    }

    @PostMapping("/refound")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<String> refoundBooking(@RequestParam Long bookingId) {
        bookingService.cancelAndRefoundPayment(bookingId);
        return ResponseEntity.ok("Reserva cancelada exitosamente con la plata devuelta al cliente");
    }


    /**
     * Obtiene todas las reservas de un usuario
     * @param uid UID del usuario en Firebase
     * @param status Estado de las reservas (opcional)
     * @param pageable Paginación
     * @return Lista paginada de reservas
     */
    @GetMapping("/user/{uid}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Page<BookingResponseDto>> getUserBookings(
            @PathVariable String uid,
            @RequestParam(required = false) BookingStatus status,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<BookingResponseDto> bookings = bookingService.getUserBookings(uid, status, pageable);
        return ResponseEntity.ok(bookings);
    }
}
