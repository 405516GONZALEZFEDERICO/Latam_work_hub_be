package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.dtos.common.BookingDto;
import Latam.Latam.work.hub.entities.BookingEntity;
import Latam.Latam.work.hub.entities.SpaceEntity;
import Latam.Latam.work.hub.enums.BookingStatus;
import Latam.Latam.work.hub.enums.BookingType;
import Latam.Latam.work.hub.repositories.BookingRepository;
import Latam.Latam.work.hub.repositories.SpaceRepository;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.BookingService;
import Latam.Latam.work.hub.services.InvoiceService;
import Latam.Latam.work.hub.services.MailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final InvoiceService invoiceService;
    private final MailService mailService;



    @Override
    @Transactional
    public String createBooking(BookingDto bookingDto) {
        try {
            // Primero validar que existan las entidades
            var space = spaceRepository.findById(bookingDto.getSpaceId())
                    .orElseThrow(() -> new RuntimeException("Espacio no encontrado"));


            var user = userRepository.findByFirebaseUid(bookingDto.getUid())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Validar disponibilidad del espacio para las fechas solicitadas
            boolean isAvailableForPeriod = isSpaceAvailableForPeriod(
                    space.getId(),
                    bookingDto.getStartDate(),
                    bookingDto.getEndDate(),
                    bookingDto.getInitHour(),
                    bookingDto.getEndHour()
            );
            if (!isAvailableForPeriod) {
                throw new RuntimeException("El espacio no está disponible para el período solicitado");
            }
            // Crear y configurar la entidad
            var bookingEntity = new BookingEntity();
            bookingEntity.setActive(true);
            bookingEntity.setSpace(space);
            bookingEntity.setUser(user);
            bookingEntity.setInitHour(bookingDto.getInitHour());
            bookingEntity.setEndHour(bookingDto.getEndHour());
            bookingEntity.setStartDate(bookingDto.getStartDate());
            bookingEntity.setEndDate(bookingDto.getEndDate());
            bookingEntity.setCounterPersons(bookingDto.getCounterPersons());
            bookingEntity.setTotalAmount(bookingDto.getTotalAmount());

            // Determinar tipo de reserva
            bookingEntity.setBookingType(determineBookingType(bookingDto));

            var savedBooking = bookingRepository.saveAndFlush(bookingEntity);
            SpaceEntity spaceEntity=space;
            spaceEntity.setAvailable(false);
            this.spaceRepository.save(spaceEntity);
            return invoiceService.createInvoice(savedBooking);

        } catch (Exception e) {
            throw new RuntimeException("Error al crear la reserva: " + e.getMessage(), e);
        }
    }

    private BookingType determineBookingType(BookingDto bookingDto) {
        if (bookingDto.getInitHour() != null) {
            return BookingType.PER_HOUR;
        } else if (bookingDto.getStartDate() != null && bookingDto.getEndDate() == null) {
            return BookingType.PER_DAY;
        }
        return BookingType.PER_MONTH;
    }


    /**
     * Verifica si un espacio está disponible para un período específico
     */
    private boolean isSpaceAvailableForPeriod(Long spaceId, LocalDateTime startDate, LocalDateTime endDate,
                                              LocalTime initHour, LocalTime endHour) {
        List<BookingEntity> overlappingBookings = bookingRepository.findOverlappingBookings(
                spaceId,
                startDate,
                endDate,
                initHour,
                endHour);

        return overlappingBookings.isEmpty();
    }

    @Override
    @Transactional
    public void confirmBookingPayment(Long bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        // Actualizar estado de la reserva
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setActive(true);
        bookingRepository.save(booking);

        // Enviar email al dueño del espacio para notificarle de la reserva
        SpaceEntity space = booking.getSpace();
        mailService.sendBookingNotificationToOwner(
                space.getOwner().getEmail(),
                space.getOwner().getName(),
                space.getName(),
                booking.getUser().getName(),
                booking.getStartDate().toString(),
                booking.getEndDate() != null ? booking.getEndDate().toString() : "",
                booking.getInitHour() != null ? booking.getInitHour().toString() : "",
                booking.getEndHour() != null ? booking.getEndHour().toString() : ""
        );
    }

    @Override
    @Transactional
    public void completeBooking(Long bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setActive(false);
        bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        booking.setStatus(BookingStatus.CANCELED);
        booking.setActive(false);
        bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void updateBookingsStatus() {
        LocalDateTime now = LocalDateTime.now();

        // Activar reservas cuya fecha de inicio ha llegado
        List<BookingEntity> upcomingBookings = bookingRepository.findUpcomingBookings(now);
        for (BookingEntity booking : upcomingBookings) {
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                booking.setStatus(BookingStatus.ACTIVE);
                booking.setActive(true);

                // Marcar el espacio como no disponible
                SpaceEntity space = booking.getSpace();
                space.setAvailable(false);
                spaceRepository.save(space);
            }
        }

        // Completar reservas cuya fecha de fin ha pasado
        List<BookingEntity> expiredBookings = bookingRepository.findExpiredBookings(now);
        for (BookingEntity booking : expiredBookings) {
            if (booking.getStatus() == BookingStatus.ACTIVE) {
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setActive(false);

                // Marcar el espacio como disponible nuevamente
                SpaceEntity space = booking.getSpace();
                space.setAvailable(true);
                spaceRepository.save(space);

                // Opcionalmente, enviar email de agradecimiento o solicitud de reseña
                mailService.sendBookingCompletedEmail(
                        booking.getUser().getEmail(),
                        booking.getUser().getName(),
                        booking.getSpace().getName()
                );
            }
        }

        bookingRepository.saveAll(upcomingBookings);
        bookingRepository.saveAll(expiredBookings);
    }
}
