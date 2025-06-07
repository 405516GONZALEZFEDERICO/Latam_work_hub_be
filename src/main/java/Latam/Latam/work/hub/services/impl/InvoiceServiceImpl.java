package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.entities.BookingEntity;
import Latam.Latam.work.hub.entities.InvoiceEntity;
import Latam.Latam.work.hub.entities.RentalContractEntity;
import Latam.Latam.work.hub.enums.InvoiceStatus;
import Latam.Latam.work.hub.enums.InvoiceType;
import Latam.Latam.work.hub.repositories.InvoiceRepository;
import Latam.Latam.work.hub.services.Billable;
import Latam.Latam.work.hub.services.InvoiceService;
import Latam.Latam.work.hub.services.mp.MercadoPagoService;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final MercadoPagoService mercadoPagoService;
    private static final Logger log = LoggerFactory.getLogger(InvoiceServiceImpl.class);

    @Override
    @Transactional
    public <T extends Billable> String createInvoice(T entity) throws MPException, MPApiException {
        InvoiceEntity invoiceEntity = new InvoiceEntity();
        String title;

        // Determinar tipo y asignar entidad
        if (entity instanceof BookingEntity) {
            BookingEntity booking = (BookingEntity) entity;
            invoiceEntity.setBooking(booking);
            invoiceEntity.setType(InvoiceType.BOOKING);
            title = "Reserva de espacio: " + booking.getSpace().getName();
            log.info("Creando factura para reserva - ID: {}, Monto: ${}", booking.getId(), booking.getAmount());
        } else if (entity instanceof RentalContractEntity) {
            RentalContractEntity contract = (RentalContractEntity) entity;
            invoiceEntity.setRentalContract(contract);
            invoiceEntity.setType(InvoiceType.CONTRACT);
            title = "Contrato de alquiler: " + contract.getSpace().getName();
            log.info("Creando factura para contrato - ID: {}, Monto obtenido: ${}", contract.getId(), contract.getAmount());
        } else {
            throw new IllegalArgumentException("Tipo de entidad no soportado");
        }

        // Configurar campos comunes
        invoiceEntity.setInvoiceNumber(generateInvoiceNumber());
        invoiceEntity.setIssueDate(LocalDateTime.now());
        invoiceEntity.setDueDate(LocalDateTime.now().plusDays(30));
        Double entityAmount = entity.getAmount();
        invoiceEntity.setTotalAmount(entityAmount);
        invoiceEntity.setStatus(InvoiceStatus.DRAFT);

        log.info("Configurando factura - Número: {}, TotalAmount: ${}, Estado: {}", 
                invoiceEntity.getInvoiceNumber(), entityAmount, invoiceEntity.getStatus());

        // Guardar la factura para obtener el ID
        InvoiceEntity savedInvoice = invoiceRepository.save(invoiceEntity);
        log.info("Factura guardada - ID: {}, TotalAmount final: ${}", savedInvoice.getId(), savedInvoice.getTotalAmount());

        // Crear preferencia de pago
        String paymentUrl = mercadoPagoService.createInvoicePaymentPreference(
                savedInvoice.getId(),
                title,
                BigDecimal.valueOf(entityAmount),
                getBuyerEmail(entity),
                getSellerEmail(entity)
        );
        
        log.info("Preferencia de pago creada para factura ID: {}", savedInvoice.getId());
        return paymentUrl;
    }

    @Override
    public InvoiceEntity findByBookingId(Long bookingId) {
        return invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Factura no encontrada para la reserva ID: " + bookingId));
    }

    private String generateInvoiceNumber() {
        LocalDateTime now = LocalDateTime.now();
        String prefix = now.format(DateTimeFormatter.ofPattern("yyyyMM"));

        // Obtener la última factura del mes actual
        String currentMonthPrefix = prefix + "%";
        String lastInvoiceNumber = invoiceRepository.findTopByInvoiceNumberLikeOrderByInvoiceNumberDesc(currentMonthPrefix)
                .map(InvoiceEntity::getInvoiceNumber)
                .orElse(prefix + "0000");

        // Extraer el número secuencial y aumentarlo
        int sequence = Integer.parseInt(lastInvoiceNumber.substring(6)) + 1;

        // Formatear el número secuencial con ceros a la izquierda
        return String.format("%s%04d", prefix, sequence);
    }

    private String getBuyerEmail(Billable entity) {
        if (entity instanceof BookingEntity) {
            return ((BookingEntity) entity).getUser().getEmail();
        } else if (entity instanceof RentalContractEntity) {
            return ((RentalContractEntity) entity).getTenant().getEmail();
        }
        throw new IllegalArgumentException("Tipo de entidad no soportado");
    }

    private String getSellerEmail(Billable entity) {
        if (entity instanceof BookingEntity) {
            return ((BookingEntity) entity).getSpace().getOwner().getEmail();
        } else if (entity instanceof RentalContractEntity) {
            return ((RentalContractEntity) entity).getSpace().getOwner().getEmail();
        }
        throw new IllegalArgumentException("Tipo de entidad no soportado");
    }
}