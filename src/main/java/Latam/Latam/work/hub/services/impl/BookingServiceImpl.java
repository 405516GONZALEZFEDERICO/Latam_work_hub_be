package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.dtos.common.BookingDto;
import Latam.Latam.work.hub.dtos.common.BookingResponseDto;
import Latam.Latam.work.hub.entities.BookingEntity;
import Latam.Latam.work.hub.entities.InvoiceEntity;
import Latam.Latam.work.hub.entities.SpaceEntity;
import Latam.Latam.work.hub.enums.BookingStatus;
import Latam.Latam.work.hub.enums.BookingType;
import Latam.Latam.work.hub.enums.InvoiceStatus;
import Latam.Latam.work.hub.repositories.BookingRepository;
import Latam.Latam.work.hub.repositories.InvoiceRepository;
import Latam.Latam.work.hub.repositories.SpaceRepository;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.BookingService;
import Latam.Latam.work.hub.services.InvoiceService;
import Latam.Latam.work.hub.services.MailService;
import Latam.Latam.work.hub.services.mp.MercadoPagoService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final InvoiceService invoiceService;
    private final MailService mailService;
    private final MercadoPagoService mercadoPagoService;
    private final InvoiceRepository invoiceRepository;

    @Override
    @Transactional
    public String createBooking(BookingDto bookingDto) {
        try {
            var space = spaceRepository.findById(bookingDto.getSpaceId())
                    .orElseThrow(() -> new RuntimeException("Espacio no encontrado"));

            var user = userRepository.findByFirebaseUid(bookingDto.getUid())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

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
            bookingEntity.setStatus(BookingStatus.PENDING_PAYMENT);
            bookingEntity.setBookingType(determineBookingType(bookingDto));

            var savedBooking = bookingRepository.saveAndFlush(bookingEntity);
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

        // Verificar si la reserva ya está confirmada
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return; // No procesar nuevamente
        }

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
    public void cancelAndRefoundPayment(Long bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        // Validar que la cancelación sea con al menos 7 días de anticipación
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = booking.getStartDate();
        long daysUntilBooking = ChronoUnit.DAYS.between(now, startDate);

        if (daysUntilBooking < 7) {
            throw new RuntimeException("Solo se permiten cancelaciones con al menos 7 días de anticipación");
        }

        // Buscar la factura asociada a la reserva
        InvoiceEntity invoice = invoiceService.findByBookingId(bookingId);

        // Manejar el pago según el estado de la factura
        if (invoice != null && invoice.getStatus() == InvoiceStatus.PAID) {
            try {
                boolean refunded = mercadoPagoService.refundPayment(invoice.getId());
                if (!refunded) {
                    throw new RuntimeException("No se pudo procesar el reembolso para la factura asociada");
                }
            } catch (Exception e) {
                throw new RuntimeException("Error al intentar reembolsar el pago: " + e.getMessage(), e);
            }
        } else {
            // Si no hay factura o no está pagada, simplemente cancelamos la reserva
            booking.setStatus(BookingStatus.CANCELED);
            booking.setActive(false);
            bookingRepository.save(booking);

            // Marcar el espacio como disponible nuevamente
            SpaceEntity space = booking.getSpace();
            space.setAvailable(true);
            spaceRepository.save(space);

            // Actualizar el estado de la factura si existe
            if (invoice != null) {
                invoice.setStatus(InvoiceStatus.CANCELLED);
                invoiceRepository.save(invoice);
            }
        }
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

    @Override
    public Page<BookingResponseDto> getUserBookings(String uid, BookingStatus status, Pageable pageable) {
        Page<BookingEntity> bookingsPage = bookingRepository
                .findByUserFirebaseUidAndStatus(uid, status, pageable);

        return bookingsPage.map(booking -> {
            BookingResponseDto dto = new BookingResponseDto();
            // Mapeo de la reserva
            dto.setId(booking.getId());
            dto.setStartDate(booking.getStartDate());
            dto.setEndDate(booking.getEndDate());
            dto.setInitHour(booking.getInitHour());
            dto.setEndHour(booking.getEndHour());
            dto.setBookingType(booking.getBookingType().name());
            dto.setStatus(booking.getStatus());
            dto.setCounterPersons(booking.getCounterPersons());
            dto.setTotalAmount(booking.getTotalAmount());

            // Mapeo del espacio
            dto.setSpaceId(booking.getSpace().getId());
            dto.setSpaceName(booking.getSpace().getName());
            String spaceAddress = String.format("%s %s, %s, %s, %s, %s",
                    booking.getSpace().getAddress().getStreetName(),
                    booking.getSpace().getAddress().getStreetNumber(),
                    booking.getSpace().getAddress().getCity().getName(),
                    booking.getSpace().getAddress().getCity().getDivisionName(),
                    booking.getSpace().getAddress().getCity().getCountry().getName(),
                    booking.getSpace().getAddress().getPostalCode()
            );
            dto.setSpaceAddress(spaceAddress);
            dto.setSpaceType(booking.getSpace().getType().getName());
            dto.setCityName(booking.getSpace().getAddress().getCity().getName());
            dto.setCountryName(booking.getSpace().getAddress().getCity().getCountry().getName());

            return dto;
        });
    }
}
