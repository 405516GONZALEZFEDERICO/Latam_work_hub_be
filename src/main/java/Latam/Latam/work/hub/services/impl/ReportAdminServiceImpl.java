package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.dtos.common.reports.admin.BookingReportFiltersDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.ContractReportFiltersDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.ExpiringContractsAlertFiltersDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.InvoiceReportFiltersDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.OverdueInvoicesAlertFiltersDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.SpaceReportFiltersDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.UserReportFiltersDto;

import Latam.Latam.work.hub.dtos.common.reports.admin.BookingReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.ContractReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.ExpiringContractAlertDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.InvoiceReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.OverdueInvoiceAlertDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.SpaceReportRowDto;
import Latam.Latam.work.hub.dtos.common.reports.admin.UserReportRowDto;

import Latam.Latam.work.hub.entities.BookingEntity;
import Latam.Latam.work.hub.entities.InvoiceEntity;
import Latam.Latam.work.hub.entities.RentalContractEntity;
import Latam.Latam.work.hub.entities.SpaceEntity;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.enums.BookingStatus;
import Latam.Latam.work.hub.enums.ContractStatus;
import Latam.Latam.work.hub.enums.InvoiceStatus;
import Latam.Latam.work.hub.repositories.BookingRepository;
import Latam.Latam.work.hub.repositories.InvoiceRepository;
import Latam.Latam.work.hub.repositories.RentalContractRepository;
import Latam.Latam.work.hub.repositories.SpaceRepository;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.ReportAdminService;
import lombok.AllArgsConstructor; // o @RequiredArgsConstructor si los campos son final
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportAdminServiceImpl implements ReportAdminService {

    private static final Logger logger = LoggerFactory.getLogger(ReportAdminServiceImpl.class);

    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final BookingRepository bookingRepository;
    private final RentalContractRepository rentalContractRepository;
    private final InvoiceRepository invoiceRepository;

    private static final String ROLE_CLIENT_NAME = "CLIENTE";
    private static final String ROLE_PROVIDER_NAME = "PROVEEDOR";
    private static final int CONTRACT_EXPIRING_DEFAULT_DAYS = 30;
    private static final String NOT_AVAILABLE = "N/A";

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
    public Page<SpaceReportRowDto> getSpacesReport(SpaceReportFiltersDto filters, Pageable pageable) {
        logger.info("Obteniendo informe de espacios con filtros: {} y paginación: {}", filters, pageable);

        LocalDateTime filterStartDate = null;
        LocalDateTime filterEndDate = null;
        Long providerId = null;
        String spaceStatusParam = null;

        if (filters != null) {

            providerId = filters.getProviderId();
            spaceStatusParam = filters.getStatus();
        }

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

        Map<Long, Long> bookingCountsMap = Collections.emptyMap();
        if (!spaceIdsOnPage.isEmpty()) {
            bookingCountsMap = bookingRepository.countBookingsForSpacesInPeriod(
                    spaceIdsOnPage, filterStartDate, filterEndDate, BookingStatus.COMPLETED // O el estado que consideres relevante
            ).stream().collect(Collectors.toMap(obj -> (Long) obj[0], obj -> (Long) obj[1]));
        }


        Map<Long, Double> revenueFromBookingsMap = Collections.emptyMap();
        if (!spaceIdsOnPage.isEmpty()) {
            revenueFromBookingsMap = invoiceRepository.sumRevenueForSpacesFromBookingsInPeriod(
                    spaceIdsOnPage, filterStartDate, filterEndDate, InvoiceStatus.PAID
            ).stream().collect(Collectors.toMap(obj -> (Long) obj[0], obj -> obj[1] != null ? ((Number) obj[1]).doubleValue() : 0.0));
        }

        Map<Long, Double> revenueFromContractsMap = Collections.emptyMap();
        if (!spaceIdsOnPage.isEmpty()) {
            revenueFromContractsMap = invoiceRepository.sumRevenueForSpacesFromContractsInPeriod(
                    spaceIdsOnPage, filterStartDate, filterEndDate, InvoiceStatus.PAID
            ).stream().collect(Collectors.toMap(obj -> (Long) obj[0], obj -> obj[1] != null ? ((Number) obj[1]).doubleValue() : 0.0));
        }

        final Map<Long, Long> finalBookingCountsMap = bookingCountsMap;
        final Map<Long, Double> finalRevenueFromBookingsMap = revenueFromBookingsMap;
        final Map<Long, Double> finalRevenueFromContractsMap = revenueFromContractsMap;

        List<SpaceReportRowDto> dtoList = spacesOnPage.stream().map(s -> {
            String statusString;
            if (s.getActive() && s.getAvailable() && !s.isDeleted()) statusString = "Disponible";
            else if (s.getActive() && !s.getAvailable() && !s.isDeleted()) statusString = "Ocupado";
            else if (!s.getActive()) statusString = "Inactivo";
            else statusString = "Eliminado"; // O "Otro" si prefieres

            // Asegúrate de que SpaceReportRowDto tenga un constructor o setters adecuados
            SpaceReportRowDto dto = new SpaceReportRowDto();
            dto.setSpaceId(s.getId());
            dto.setSpaceName(s.getName());
            if (s.getOwner() != null) {
                dto.setProviderName(s.getOwner().getName());
            } else {
                dto.setProviderName("N/A");
            }
            dto.setStatus(statusString);
            dto.setBookingCount(finalBookingCountsMap.getOrDefault(s.getId(), 0L));
            double revBookings = finalRevenueFromBookingsMap.getOrDefault(s.getId(), 0.0);
            double revContracts = finalRevenueFromContractsMap.getOrDefault(s.getId(), 0.0);
            dto.setRevenueGenerated(revBookings + revContracts);
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, spacesPage.getTotalElements());
    }


 @Override
 @Transactional(readOnly = true)
 public Page<BookingReportRowDto> getBookingsReport(BookingReportFiltersDto filters, Pageable pageable) {
     logger.info("Obteniendo informe de bookings con filtros: {} y paginación: {}", filters, pageable);
     LocalDate startDateParam = null;
     LocalDate endDateParam = null;
     Long clientIdParam = null;
     Long providerIdParam = null;
     Long spaceIdParam = null;
     BookingStatus bookingStatusEnum = null;

     if (filters != null) {
         startDateParam = filters.getStartDate(); // Ya es LocalDate, no necesita conversión
         endDateParam = filters.getEndDate();     // Ya es LocalDate, no necesita conversión
         clientIdParam = filters.getClientId();
         providerIdParam = filters.getProviderId();
         spaceIdParam = filters.getSpaceId();
         bookingStatusEnum = safeEnumValueOf(BookingStatus.class, filters.getStatus(), "status de reserva");
     }

     Page<BookingEntity> bookingsPage = bookingRepository.findBookingsForReport(
             startDateParam,
             endDateParam,
             clientIdParam,
             providerIdParam,
             spaceIdParam,
             bookingStatusEnum,
             pageable
     );

     List<BookingReportRowDto> dtoList = bookingsPage.getContent().stream().map(b -> {
         BookingReportRowDto dto = new BookingReportRowDto();
         dto.setBookingId(b.getId());
         if (b.getSpace() != null) {
             dto.setSpaceName(b.getSpace().getName());
             if (b.getSpace().getOwner() != null) {
                 dto.setProviderName(b.getSpace().getOwner().getName());
             } else {
                 dto.setProviderName(NOT_AVAILABLE);
             }
         } else {
             dto.setSpaceName(NOT_AVAILABLE);
             dto.setProviderName(NOT_AVAILABLE);
         }
         if (b.getUser() != null) {
             dto.setClientName(b.getUser().getName());
         } else {
             dto.setClientName(NOT_AVAILABLE);
         }
         dto.setStartDate(b.getStartDate());
         dto.setEndDate(b.getEndDate());
         if (b.getStartDate() != null && b.getEndDate() != null) {
             dto.setDurationHours(ChronoUnit.HOURS.between(b.getStartDate(), b.getEndDate()));
         } else {
             dto.setDurationHours(0L);
         }
         dto.setStatus(b.getStatus() != null ? b.getStatus().toString() : NOT_AVAILABLE);
         dto.setAmount(b.getTotalAmount());
         return dto;
     }).collect(Collectors.toList());

     return new PageImpl<>(dtoList, pageable, bookingsPage.getTotalElements());
 }
    @Override
    @Transactional(readOnly = true)
    public Page<UserReportRowDto> getUsersReport(UserReportFiltersDto filters, Pageable pageable) {
        logger.info("Obteniendo informe de usuarios con filtros: {} y paginación: {}", filters, pageable);

        String roleFilter = null;
        Boolean isEnabledQueryParam = null; // Para el estado del usuario (activo/inactivo)
        LocalDateTime registrationStartDate = null;
        LocalDateTime registrationEndDate = null;

        if (filters != null) {
            roleFilter = StringUtils.hasText(filters.getRole()) ? filters.getRole() : null;

            if (StringUtils.hasText(filters.getStatus())) {
                String statusFilter = filters.getStatus().trim();
                if ("activo".equalsIgnoreCase(statusFilter)) {
                    isEnabledQueryParam = true;
                } else if ("inactivo".equalsIgnoreCase(statusFilter)) {
                    isEnabledQueryParam = false;
                } else {
                    logger.warn("Valor de filtro de estado de usuario no reconocido: '{}'. No se filtrará por estado.", statusFilter);
                    // isEnabledQueryParam permanece null, por lo que la query no filtrará por este criterio si el valor es inválido.
                }
            }
            registrationStartDate = filters.getStartDate(); // Puede ser null
        }

        // Llamada al método del repositorio que ahora acepta Boolean isEnabled
        Page<UserEntity> usersPage = userRepository.findUsersForReport(
                roleFilter,
                isEnabledQueryParam,
                registrationStartDate,
                pageable
        );

        // Mapeo a DTO
        List<UserReportRowDto> dtoList = usersPage.getContent().stream().map(user -> {
            UserReportRowDto dto = new UserReportRowDto();
            dto.setUserId(user.getId());
            dto.setEmail(user.getEmail());
            dto.setName(user.getName());
            dto.setRole(user.getRole() != null ? user.getRole().getName() : NOT_AVAILABLE);
            dto.setStatus(user.isEnabled() ? "Activo" : "Inactivo"); // Se calcula para mostrar, independientemente del filtro
            dto.setRegistrationDate(user.getRegistrationDate());
            dto.setLastLoginDate(user.getLastAccess());

            // Cargar estadísticas específicas del rol
            if (user.getRole() != null) {
                String userRoleName = user.getRole().getName();
                if (ROLE_PROVIDER_NAME.equals(userRoleName)) {
                    dto.setTotalSpaces(spaceRepository.countByOwnerId(user.getId()));
                    dto.setActiveContracts(rentalContractRepository.countActiveContractsByProviderId(user.getId()));
                    // Considera si las fechas de registro son las correctas para el cálculo de revenue/spending.
                    // Si deben ser independientes de los filtros de fecha del reporte de usuarios, pasa null.
                    dto.setTotalRevenue(invoiceRepository.sumRevenueByProviderId(user.getId(), null , null));
                } else if (ROLE_CLIENT_NAME.equals(userRoleName)) {
                    dto.setTotalBookings(bookingRepository.countByUserId(user.getId()));
                    dto.setActiveContracts(rentalContractRepository.countActiveContractsByTenantId(user.getId()));
                    dto.setTotalSpending(invoiceRepository.sumSpendingByClientId(user.getId(), null, null));
                }
            }
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, usersPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContractReportRowDto> getContractsReport(ContractReportFiltersDto filters, Pageable pageable) {
        logger.info("Obteniendo informe de contratos con filtros: {} y paginación: {}", filters, pageable);
        LocalDate filterContractStartDate = null;
        LocalDate filterContractEndDate = null;
        Long tenantId = null;
        Long ownerId = null;
        ContractStatus contractStatusEnum = null;

        if (filters != null) {
            filterContractStartDate = filters.getContractStartDate();
            filterContractEndDate = filters.getContractEndDate();
            tenantId = filters.getTenantId();
            ownerId = filters.getOwnerId();
            contractStatusEnum = safeEnumValueOf(ContractStatus.class, filters.getStatus(), "status de contrato");
        }

        Page<RentalContractEntity> contractsPage = rentalContractRepository.findContractsForReport(
                filterContractStartDate, filterContractEndDate, tenantId, ownerId, contractStatusEnum, pageable
        );

        List<ContractReportRowDto> dtoList = contractsPage.getContent().stream().map(rc -> {
            ContractReportRowDto dto = new ContractReportRowDto(); // Asegúrate de que este DTO exista
            dto.setContractId(rc.getId());
            if (rc.getSpace() != null) {
                dto.setSpaceName(rc.getSpace().getName());
                if (rc.getSpace().getOwner() != null) {
                    dto.setOwnerName(rc.getSpace().getOwner().getName());
                } else {
                    dto.setOwnerName("N/A");
                }
            } else {
                dto.setSpaceName("N/A");
                dto.setOwnerName("N/A");
            }
            if (rc.getTenant() != null) {
                dto.setTenantName(rc.getTenant().getName());
            } else {
                dto.setTenantName("N/A");
            }
            dto.setStartDate(rc.getStartDate());
            dto.setEndDate(rc.getEndDate());
            dto.setAmount(rc.getMonthlyAmount());
            dto.setStatus(rc.getContractStatus() != null ? rc.getContractStatus().toString() : "N/A");
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, contractsPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceReportRowDto> getInvoicesReport(InvoiceReportFiltersDto filters, Pageable pageable) {
        logger.info("Obteniendo informe de facturas con filtros: {} y paginación: {}", filters, pageable);
        LocalDateTime filterIssueStartDate = null;
        LocalDateTime filterIssueEndDate = null;
        Long clientId = null;
        InvoiceStatus invoiceStatusEnum = null;

        if (filters != null) {
            filterIssueStartDate = filters.getStartDate(); // Para fecha de emisión
            clientId = filters.getClientId();
            invoiceStatusEnum = safeEnumValueOf(InvoiceStatus.class, filters.getStatus(), "status de factura");
        }

        Page<InvoiceEntity> invoicesPage = invoiceRepository.findInvoicesForReport(
                filterIssueStartDate, filterIssueEndDate, clientId, invoiceStatusEnum, pageable
        );

        List<InvoiceReportRowDto> dtoList = invoicesPage.getContent().stream().map(i -> {
            InvoiceReportRowDto dto = new InvoiceReportRowDto(); // Asegúrate de que este DTO exista
            dto.setInvoiceId(i.getId());
            String clientNameFound = "N/A";
            if (i.getBooking() != null && i.getBooking().getUser() != null) {
                clientNameFound = i.getBooking().getUser().getName();
            } else if (i.getRentalContract() != null && i.getRentalContract().getTenant() != null) {
                clientNameFound = i.getRentalContract().getTenant().getName();
            }
            dto.setClientName(clientNameFound);
            dto.setInvoiceType(i.getType() != null ? i.getType().name() : "N/A");
            dto.setIssueDate(i.getIssueDate());
            dto.setDueDate(i.getDueDate());
            dto.setTotalAmount(i.getTotalAmount());

            // Asume que no hay 'paidAmount' en InvoiceEntity. Si lo hay, úsalo.
            // dto.setPaidAmount(i.getPaidAmount() != null ? i.getPaidAmount() : 0.0);
            dto.setPaidAmount(0.0); // Placeholder

            double total = i.getTotalAmount() != null ? i.getTotalAmount() : 0.0;
            double paid = dto.getPaidAmount();
            dto.setPendingAmount(total - paid);

            dto.setStatus(i.getStatus() != null ? i.getStatus().toString() : "N/A");
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, invoicesPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpiringContractAlertDto> getExpiringContractsAlerts(ExpiringContractsAlertFiltersDto filters, Pageable pageable) {
        logger.info("Obteniendo alertas de contratos por vencer con filtros: {} y paginación: {}", filters, pageable);
        LocalDate today = LocalDate.now();

        int daysForExpiryAlert = CONTRACT_EXPIRING_DEFAULT_DAYS;
        if (filters != null && filters.getDaysUntilExpiry() != null && filters.getDaysUntilExpiry() > 0) {
            daysForExpiryAlert = filters.getDaysUntilExpiry();
        }
        LocalDate expiryLimitDate = today.plusDays(daysForExpiryAlert);

        Page<RentalContractEntity> contractsPage = rentalContractRepository.findExpiringContractsForAlerts(
                today, expiryLimitDate, ContractStatus.ACTIVE, pageable // Asume que solo quieres contratos ACTIVOS
        );

        List<ExpiringContractAlertDto> dtoList = contractsPage.getContent().stream().map(rc -> {
            ExpiringContractAlertDto dto = new ExpiringContractAlertDto(); // Asegúrate de que este DTO exista
            dto.setContractId(rc.getId());
            if (rc.getSpace() != null) {
                dto.setSpaceName(rc.getSpace().getName());
            } else {
                dto.setSpaceName("N/A");
            }
            if (rc.getTenant() != null) {
                dto.setTenantName(rc.getTenant().getName());
            } else {
                dto.setTenantName("N/A");
            }
            dto.setExpiryDate(rc.getEndDate());
            if (rc.getEndDate() != null) {
                long daysBetween = ChronoUnit.DAYS.between(today, rc.getEndDate());
                dto.setDaysUntilExpiry(daysBetween >= 0 ? daysBetween : 0); // No mostrar días negativos si ya expiró
            } else {
                dto.setDaysUntilExpiry(null);
            }
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, contractsPage.getTotalElements());
    }


    @Override
    @Transactional(readOnly = true)
    public Page<OverdueInvoiceAlertDto> getOverdueInvoicesAlerts(OverdueInvoicesAlertFiltersDto filters, Pageable pageable) {
        // El DTO OverdueInvoicesAlertFiltersDto está actualmente vacío.
        // Si añades filtros (ej. minDaysOverdue, status) a ese DTO, debes usarlos aquí.
        logger.info("Obteniendo alertas de facturas vencidas con filtros: {} y paginación: {}", filters, pageable);
        LocalDateTime overdueThresholdDate = LocalDateTime.now().with(LocalTime.MIN); // Facturas con dueDate ANTES de hoy

        // Por defecto, considera facturas emitidas. Podrías hacerlo configurable.
        List<InvoiceStatus> statusesToConsider = List.of(InvoiceStatus.ISSUED);
        // if (filters != null && StringUtils.hasText(filters.getStatus())) {
        //     InvoiceStatus filteredStatus = safeEnumValueOf(InvoiceStatus.class, filters.getStatus(), "status de factura vencida");
        //     if (filteredStatus != null && (filteredStatus == InvoiceStatus.ISSUED /* || otros estados válidos para mora */)) {
        //         statusesToConsider = List.of(filteredStatus);
        //     }
        // }

        Page<InvoiceEntity> invoicesPage = invoiceRepository.findOverdueInvoicesForAlerts(
                overdueThresholdDate, statusesToConsider, pageable
        );

        List<OverdueInvoiceAlertDto> dtoList = invoicesPage.getContent().stream().map(i -> {
            OverdueInvoiceAlertDto dto = new OverdueInvoiceAlertDto(); // Asegúrate de que este DTO exista
            dto.setInvoiceId(i.getId());
            String clientNameFound = "N/A";
            if (i.getBooking() != null && i.getBooking().getUser() != null) {
                clientNameFound = i.getBooking().getUser().getName();
            } else if (i.getRentalContract() != null && i.getRentalContract().getTenant() != null) {
                clientNameFound = i.getRentalContract().getTenant().getName();
            }
            dto.setClientName(clientNameFound);
            dto.setDueDate(i.getDueDate());
            if (i.getDueDate() != null && i.getDueDate().toLocalDate().isBefore(LocalDate.now())) {
                dto.setDaysOverdue(ChronoUnit.DAYS.between(i.getDueDate().toLocalDate(), LocalDate.now()));
            } else {
                // Si no está vencida (o dueDate es null), días vencidos es 0 o null.
                // La query ya debería traer solo las vencidas, pero es buena una doble verificación.
                dto.setDaysOverdue(0L);
            }
            // Asume que totalAmount es lo vencido si no hay campo paidAmount en InvoiceEntity.
            dto.setOverdueAmount(i.getTotalAmount());
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, invoicesPage.getTotalElements());
    }
}