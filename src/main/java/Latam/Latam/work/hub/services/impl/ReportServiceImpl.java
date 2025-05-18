package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.dtos.common.reports.admin.AdminKpiDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.BookingReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.ContractReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.ExpiringContractAlertDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.InvoiceReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.OverdueInvoiceAlertDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.ReportFiltersDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.SpaceReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.UserReportRowDto;
import Latam.Latam.work.hub.entities.BookingEntity;
import Latam.Latam.work.hub.entities.InvoiceEntity;
import Latam.Latam.work.hub.entities.RentalContractEntity;
import Latam.Latam.work.hub.entities.SpaceEntity;
import Latam.Latam.work.hub.enums.BookingStatus;
import Latam.Latam.work.hub.enums.ContractStatus;
import Latam.Latam.work.hub.enums.InvoiceStatus;
import Latam.Latam.work.hub.repositories.BookingRepository;
import Latam.Latam.work.hub.repositories.InvoiceRepository;
import Latam.Latam.work.hub.repositories.RentalContractRepository;
import Latam.Latam.work.hub.repositories.SpaceRepository;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.ReportService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportServiceImpl.class);

    // Campos 'final' para que @RequiredArgsConstructor los inyecte
    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final BookingRepository bookingRepository;
    private final RentalContractRepository rentalContractRepository;
    private final InvoiceRepository invoiceRepository;

    // Estas constantes deben coincidir con los nombres en tu RoleEntity
    private static final String ROLE_CLIENT_NAME = "CLIENTE"; // AJUSTA SI ES DIFERENTE
    private static final String ROLE_PROVIDER_NAME = "PROVEEDOR"; // AJUSTA SI ES DIFERENTE
    private static final int CONTRACT_EXPIRING_DEFAULT_DAYS = 30;

    private <T extends Enum<T>> T safeEnumValueOf(Class<T> enumClass, String name, String fieldName) {
        if (StringUtils.hasText(name)) {
            try {
                return Enum.valueOf(enumClass, name.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Valor de filtro inválido '{}' para el campo '{}' (enum {}). Se ignorará el filtro.", name, fieldName, enumClass.getSimpleName());
                return null;
            }
        }
        return null;
    }


    @Override
    @Transactional(readOnly = true)
    public AdminKpiDto getAdminKpis(ReportFiltersDto filters) {
        logger.info("Generando KPIs con filtros: {}", filters);
        AdminKpiDto kpis = new AdminKpiDto();

        kpis.setTotalActiveClients(userRepository.countActiveUsersByRole(ROLE_CLIENT_NAME));
        kpis.setTotalActiveProviders(userRepository.countActiveUsersByRole(ROLE_PROVIDER_NAME));
        kpis.setTotalAvailableSpaces(spaceRepository.countByActiveTrueAndAvailableTrueAndDeletedFalse());

        LocalDateTime startDate = (filters != null && filters.getStartDate() != null) ? filters.getStartDate() : YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime endDate = (filters != null && filters.getEndDate() != null) ? filters.getEndDate() : YearMonth.now().atEndOfMonth().atTime(LocalTime.MAX);

        // Estos métodos ya existen en los repositorios que me proporcionaste
        kpis.setTotalBookingsPeriod(bookingRepository.countByStartDateBetweenAndStatus(startDate, endDate, BookingStatus.COMPLETED));
        Double totalRevenueDouble = invoiceRepository.sumTotalAmountByStatusAndDateRange(InvoiceStatus.PAID, startDate, endDate);
        kpis.setTotalRevenuePeriod(totalRevenueDouble != null ? BigDecimal.valueOf(totalRevenueDouble) : BigDecimal.ZERO);

        return kpis;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SpaceReportRowDto> getSpacesReport(ReportFiltersDto filters, Pageable pageable) {
        logger.info("Obteniendo informe de espacios con filtros (enfoque servicio): {} y paginación: {}", filters, pageable);

        LocalDateTime filterStartDate = (filters != null) ? filters.getStartDate() : null;
        LocalDateTime filterEndDate = (filters != null) ? filters.getEndDate() : null;
        Long providerId = (filters != null) ? filters.getProviderId() : null;
        String spaceStatusParam = (filters != null) ? filters.getStatus() : null;

        Page<SpaceEntity> spacesPage = spaceRepository.findSpacesForReportPage(
                providerId, spaceStatusParam, pageable
        );

        List<SpaceEntity> spacesOnPage = spacesPage.getContent();
        if (spacesOnPage.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<Long> spaceIdsOnPage = spacesOnPage.stream()
                .map(SpaceEntity::getId)
                .collect(Collectors.toList());

        Map<Long, Long> bookingCountsMap = bookingRepository.countBookingsForSpacesInPeriod(
                spaceIdsOnPage, filterStartDate, filterEndDate, BookingStatus.COMPLETED
        ).stream().collect(Collectors.toMap(obj -> (Long) obj[0], obj -> (Long) obj[1]));

        Map<Long, Double> revenueFromBookingsMap = invoiceRepository.sumRevenueForSpacesFromBookingsInPeriod(
                spaceIdsOnPage, filterStartDate, filterEndDate, InvoiceStatus.PAID
        ).stream().collect(Collectors.toMap(obj -> (Long) obj[0], obj -> obj[1] != null ? ((Number) obj[1]).doubleValue() : 0.0));

        Map<Long, Double> revenueFromContractsMap = invoiceRepository.sumRevenueForSpacesFromContractsInPeriod(
                spaceIdsOnPage, filterStartDate, filterEndDate, InvoiceStatus.PAID
        ).stream().collect(Collectors.toMap(obj -> (Long) obj[0], obj -> obj[1] != null ? ((Number) obj[1]).doubleValue() : 0.0));

        List<SpaceReportRowDto> dtoList = spacesOnPage.stream().map(s -> {
            String statusString;
            if (s.getActive() && s.getAvailable() && !s.isDeleted()) statusString = "Disponible";
            else if (s.getActive() && !s.getAvailable() && !s.isDeleted()) statusString = "Ocupado";
            else if (!s.getActive()) statusString = "Inactivo";
            else statusString = "Otro"; // Asume s.deleted() ya fue filtrado por la query del repo

            // Usa el constructor que definimos para el servicio
            SpaceReportRowDto dto = new SpaceReportRowDto(s.getId(), s.getName(), s.getOwner().getName(), statusString);
            dto.setBookingCount(bookingCountsMap.getOrDefault(s.getId(), 0L));
            double revBookings = revenueFromBookingsMap.getOrDefault(s.getId(), 0.0);
            double revContracts = revenueFromContractsMap.getOrDefault(s.getId(), 0.0);
            dto.setRevenueGenerated(revBookings + revContracts);
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, spacesPage.getTotalElements());
    }


    @Override
    @Transactional(readOnly = true)
    public Page<BookingReportRowDto> getBookingsReport(ReportFiltersDto filters, Pageable pageable) {
        logger.info("Obteniendo informe de bookings (enfoque servicio): {} y paginación: {}", filters, pageable);
        LocalDateTime startDateParam = (filters != null) ? filters.getStartDate() : null;
        LocalDateTime endDateParam = (filters != null) ? filters.getEndDate() : null;
        Long clientIdParam = (filters != null) ? filters.getClientId() : null;
        Long providerIdParam = (filters != null) ? filters.getProviderId() : null;
        Long spaceIdParam = (filters != null) ? filters.getSpaceId() : null;
        BookingStatus bookingStatusEnum = (filters != null) ? safeEnumValueOf(BookingStatus.class, filters.getStatus(), "status de reserva") : null;

        Page<BookingEntity> bookingsPage = bookingRepository.findBookingsForReport( // Usando el método que devuelve entidades
                startDateParam, endDateParam, clientIdParam, providerIdParam,
                spaceIdParam, bookingStatusEnum, pageable
        );

        List<BookingReportRowDto> dtoList = bookingsPage.getContent().stream().map(b -> {
            BookingReportRowDto dto = new BookingReportRowDto();
            dto.setBookingId(b.getId());
            // Los JOIN FETCH en el repo deberían asegurar que estas relaciones están cargadas
            if (b.getSpace() != null) {
                dto.setSpaceName(b.getSpace().getName());
                if (b.getSpace().getOwner() != null) {
                    dto.setProviderName(b.getSpace().getOwner().getName());
                }
            }
            if (b.getUser() != null) { // user es el cliente en BookingEntity
                dto.setClientName(b.getUser().getName());
            }
            dto.setStartDate(b.getStartDate());
            dto.setEndDate(b.getEndDate());
            if (b.getStartDate() != null && b.getEndDate() != null) {
                dto.setDurationHours(ChronoUnit.HOURS.between(b.getStartDate(), b.getEndDate()));
            } else {
                dto.setDurationHours(0L);
            }
            dto.setStatus(b.getStatus().toString());
            dto.setAmount(b.getTotalAmount()); // Asume que BookingEntity.totalAmount es Double
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, bookingsPage.getTotalElements());
    }

    @Override
    public Page<UserReportRowDto> getUsersReport(ReportFiltersDto filters, Pageable pageable) {
        return null;
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ContractReportRowDto> getContractsReport(ReportFiltersDto filters, Pageable pageable) {
        logger.info("Obteniendo informe de contratos (enfoque servicio): {} y paginación: {}", filters, pageable);
        LocalDate filterStartDate = (filters != null && filters.getStartDate() != null) ? filters.getStartDate().toLocalDate() : null;
        LocalDate filterEndDate = (filters != null && filters.getEndDate() != null) ? filters.getEndDate().toLocalDate() : null;
        Long tenantId = (filters != null) ? filters.getClientId() : null;
        Long ownerId = (filters != null) ? filters.getProviderId() : null;
        ContractStatus contractStatusEnum = (filters != null) ? safeEnumValueOf(ContractStatus.class, filters.getStatus(), "status de contrato") : null;

        Page<RentalContractEntity> contractsPage = rentalContractRepository.findContractsForReport( // Usando el método que devuelve entidades
                filterStartDate, filterEndDate, tenantId, ownerId, contractStatusEnum, pageable
        );

        List<ContractReportRowDto> dtoList = contractsPage.getContent().stream().map(rc -> {
            ContractReportRowDto dto = new ContractReportRowDto();
            dto.setContractId(rc.getId());
            // Los JOIN FETCH en el repo deberían asegurar que estas relaciones están cargadas
            if (rc.getSpace() != null) {
                dto.setSpaceName(rc.getSpace().getName());
                if (rc.getSpace().getOwner() != null) {
                    dto.setOwnerName(rc.getSpace().getOwner().getName());
                }
            }
            if (rc.getTenant() != null) {
                dto.setTenantName(rc.getTenant().getName());
            }
            dto.setStartDate(rc.getStartDate());
            dto.setEndDate(rc.getEndDate());
            dto.setAmount(rc.getMonthlyAmount()); // Asume que RentalContractEntity.monthlyAmount es Double
            dto.setStatus(rc.getContractStatus().toString());
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, contractsPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceReportRowDto> getInvoicesReport(ReportFiltersDto filters, Pageable pageable) {
        logger.info("Obteniendo informe de facturas (enfoque servicio): {} y paginación: {}", filters, pageable);
        LocalDateTime filterStartDate = (filters != null) ? filters.getStartDate() : null;
        LocalDateTime filterEndDate = (filters != null) ? filters.getEndDate() : null;
        Long clientId = (filters != null) ? filters.getClientId() : null;
        InvoiceStatus invoiceStatusEnum = (filters != null) ? safeEnumValueOf(InvoiceStatus.class, filters.getStatus(), "status de factura") : null;

        Page<InvoiceEntity> invoicesPage = invoiceRepository.findInvoicesForReport( // Usando el método que devuelve entidades
                filterStartDate, filterEndDate, clientId, invoiceStatusEnum, pageable
        );

        List<InvoiceReportRowDto> dtoList = invoicesPage.getContent().stream().map(i -> {
            InvoiceReportRowDto dto = new InvoiceReportRowDto();
            dto.setInvoiceId(i.getId());

            String clientNameFound = "N/A";
            // Los JOIN FETCH en el repo deberían ayudar aquí
            if (i.getBooking() != null && i.getBooking().getUser() != null) {
                clientNameFound = i.getBooking().getUser().getName();
            } else if (i.getRentalContract() != null && i.getRentalContract().getTenant() != null) {
                clientNameFound = i.getRentalContract().getTenant().getName();
            }
            dto.setClientName(clientNameFound);

            dto.setIssueDate(i.getIssueDate());
            dto.setDueDate(i.getDueDate());
            dto.setTotalAmount(i.getTotalAmount());

            // Asumimos que InvoiceEntity NO tiene un campo 'paidAmount'.
            // Si lo tuviera, lo asignarías aquí: dto.setPaidAmount(i.getPaidAmount());
            dto.setPaidAmount(0.0); // Placeholder si no existe en la entidad

            // Calcular pendingAmount
            double total = i.getTotalAmount() != null ? i.getTotalAmount() : 0.0;
            double paid = dto.getPaidAmount(); // Usar el valor seteado (actualmente 0.0)
            dto.setPendingAmount(total - paid);

            dto.setStatus(i.getStatus().toString());
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, invoicesPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpiringContractAlertDto> getExpiringContractsAlerts(ReportFiltersDto filters, Pageable pageable) {
        logger.info("Obteniendo alertas de contratos por vencer (enfoque servicio): {} y paginación: {}", filters, pageable);
        LocalDate today = LocalDate.now();
        // Podrías tomar daysUntilExpiry del DTO de filtros si lo añades allí
        int daysForExpiryAlert = (filters != null && filters.getDaysUntilExpiry() != null) ? filters.getDaysUntilExpiry() : CONTRACT_EXPIRING_DEFAULT_DAYS;
        LocalDate expiryLimitDate = today.plusDays(daysForExpiryAlert);

        Page<RentalContractEntity> contractsPage = rentalContractRepository.findExpiringContractsForAlerts( // Usando el método que devuelve entidades
                today, expiryLimitDate, ContractStatus.ACTIVE, pageable // Asegúrate que ContractStatus.ACTIVE es correcto
        );

        List<ExpiringContractAlertDto> dtoList = contractsPage.getContent().stream().map(rc -> {
            ExpiringContractAlertDto dto = new ExpiringContractAlertDto();
            dto.setContractId(rc.getId());
            // Los JOIN FETCH en el repo deberían asegurar que estas relaciones están cargadas
            if (rc.getSpace() != null) {
                dto.setSpaceName(rc.getSpace().getName());
            }
            if (rc.getTenant() != null) {
                dto.setTenantName(rc.getTenant().getName());
            }
            dto.setExpiryDate(rc.getEndDate());
            if (rc.getEndDate() != null) {
                dto.setDaysUntilExpiry(ChronoUnit.DAYS.between(today, rc.getEndDate()));
            } else {
                dto.setDaysUntilExpiry(null); // O un valor grande si prefieres
            }
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, contractsPage.getTotalElements());
    }


    @Override
    @Transactional(readOnly = true)
    public Page<OverdueInvoiceAlertDto> getOverdueInvoicesAlerts(ReportFiltersDto filters, Pageable pageable) {
        logger.info("Obteniendo alertas de facturas vencidas (enfoque servicio): {} y paginación: {}", filters, pageable);
        LocalDateTime overdueThresholdDate = LocalDateTime.now().with(LocalTime.MIN); // Facturas vencidas ANTES de hoy

        // Por defecto, busca facturas emitidas (ISSUED) que estén vencidas.
        // Puedes ajustar esto o hacerlo más configurable si es necesario.
        List<InvoiceStatus> statusesToConsider = List.of(InvoiceStatus.ISSUED);
        // Si el filtro de estado viene y es válido, podrías usarlo.
        // if (filters != null && StringUtils.hasText(filters.getStatus())) {
        //     InvoiceStatus filteredStatus = safeEnumValueOf(InvoiceStatus.class, filters.getStatus(), "status de factura vencida");
        //     if (filteredStatus != null && (filteredStatus == InvoiceStatus.ISSUED /* || otros estados válidos para mora */)) {
        //         statusesToConsider = List.of(filteredStatus);
        //     }
        // }


        Page<InvoiceEntity> invoicesPage = invoiceRepository.findOverdueInvoicesForAlerts( // Usando el método que devuelve entidades
                overdueThresholdDate, statusesToConsider, pageable
        );

        List<OverdueInvoiceAlertDto> dtoList = invoicesPage.getContent().stream().map(i -> {
            OverdueInvoiceAlertDto dto = new OverdueInvoiceAlertDto();
            dto.setInvoiceId(i.getId());
            String clientNameFound = "N/A";
            // Los JOIN FETCH en el repo deberían ayudar
            if (i.getBooking() != null && i.getBooking().getUser() != null) {
                clientNameFound = i.getBooking().getUser().getName();
            } else if (i.getRentalContract() != null && i.getRentalContract().getTenant() != null) {
                clientNameFound = i.getRentalContract().getTenant().getName();
            }
            dto.setClientName(clientNameFound);
            dto.setDueDate(i.getDueDate());
            if (i.getDueDate() != null) {
                dto.setDaysOverdue(ChronoUnit.DAYS.between(i.getDueDate().toLocalDate(), LocalDate.now()));
            } else {
                dto.setDaysOverdue(null);
            }
            // Asumimos que totalAmount es lo vencido si no hay campo paidAmount en InvoiceEntity
            dto.setOverdueAmount(i.getTotalAmount());
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, invoicesPage.getTotalElements());
    }
}
