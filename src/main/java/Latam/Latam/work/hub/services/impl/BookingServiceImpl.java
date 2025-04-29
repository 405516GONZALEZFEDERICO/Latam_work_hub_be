package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.dtos.common.BookingDto;
import Latam.Latam.work.hub.entities.BookingEntity;
import Latam.Latam.work.hub.entities.SpaceEntity;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.enums.BookingType;
import Latam.Latam.work.hub.repositories.BookingRepository;
import Latam.Latam.work.hub.repositories.SpaceRepository;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.BookingService;
import Latam.Latam.work.hub.services.InvoiceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final ModelMapper modelMapper;
    private final InvoiceService invoiceService;
    @Override
    @Transactional
    public String createBooking(BookingDto bookingDto) {
        try {
            // Primero validar que existan las entidades
            var space = spaceRepository.findById(bookingDto.getSpaceId())
                    .orElseThrow(() -> new RuntimeException("Espacio no encontrado"));


            var user = userRepository.findByFirebaseUid(bookingDto.getUid())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

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
}
