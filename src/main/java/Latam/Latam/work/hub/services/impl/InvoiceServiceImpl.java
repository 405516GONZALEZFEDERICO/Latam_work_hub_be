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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final MercadoPagoService mercadoPagoService;

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
        } else if (entity instanceof RentalContractEntity) {
            RentalContractEntity contract = (RentalContractEntity) entity;
            invoiceEntity.setRentalContract(contract);
            invoiceEntity.setType(InvoiceType.CONTRACT);
            title = "Contrato de alquiler: " + contract.getSpace().getName();
        } else {
            throw new IllegalArgumentException("Tipo de entidad no soportado");
        }

        // Configurar campos comunes
        invoiceEntity.setInvoiceNumber(generateInvoiceNumber());
        invoiceEntity.setIssueDate(LocalDateTime.now());
        invoiceEntity.setTotalAmount(entity.getAmount());
        invoiceEntity.setStatus(InvoiceStatus.DRAFT);

        // Guardar la factura para obtener el ID
        InvoiceEntity savedInvoice = invoiceRepository.save(invoiceEntity);

        // Crear preferencia de pago
        return mercadoPagoService.createInvoicePaymentPreference(
                savedInvoice.getId(),
                title,
                BigDecimal.valueOf(entity.getAmount()),
                getBuyerEmail(entity),
                getSellerEmail(entity)
        );
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