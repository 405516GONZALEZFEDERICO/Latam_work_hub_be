package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.dtos.common.dashboard.proveedor.ProviderKpiCardsDto;
import Latam.Latam.work.hub.dtos.common.dashboard.proveedor.ProviderMonthlyRevenueDto;
import Latam.Latam.work.hub.dtos.common.dashboard.proveedor.ProviderSpacePerformanceDto;
import Latam.Latam.work.hub.services.DashboardProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.ArrayList;

// Nuevos imports para debug
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.entities.RentalContractEntity;
import Latam.Latam.work.hub.entities.InvoiceEntity;
import Latam.Latam.work.hub.entities.BookingEntity;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.repositories.RentalContractRepository;
import Latam.Latam.work.hub.repositories.InvoiceRepository;
import Latam.Latam.work.hub.repositories.BookingRepository;
import Latam.Latam.work.hub.enums.InvoiceStatus;
import Latam.Latam.work.hub.enums.BookingStatus;

@RestController
@RequestMapping("/api/dashboard-provider")
@PreAuthorize("hasRole('PROVEEDOR')")
@RequiredArgsConstructor
public class DashboardProviderController {
    private final DashboardProviderService dashboardProviderService;
    private final UserRepository userRepository;
    private final RentalContractRepository rentalContractRepository;
    private final InvoiceRepository invoiceRepository;
    private final BookingRepository bookingRepository;

    /**
     * Endpoint para obtener los datos de las Tarjetas de KPI del proveedor.
     */
    @GetMapping("/kpi-cards")
    public ResponseEntity<ProviderKpiCardsDto> getKpiCards(@RequestParam String uid) {
        ProviderKpiCardsDto kpiData = dashboardProviderService.getKpiCardsData(uid);
        return ResponseEntity.ok(kpiData);
    }

    /**
     * Endpoint para el gráfico de líneas de ingresos mensuales del proveedor.
     * @param uid UID del proveedor
     * @param months Número de meses hacia atrás (opcional, defecto: 12).
     */
    @GetMapping("/monthly-revenue")
    public ResponseEntity<List<ProviderMonthlyRevenueDto>> getMonthlyRevenue(
            @RequestParam String uid,
            @RequestParam(name = "months", defaultValue = "12", required = false) int months) {
        if (months <= 0 || months > 60) {
            months = 12;
        }
        List<ProviderMonthlyRevenueDto> revenueData = dashboardProviderService.getMonthlyRevenue(uid, months);
        return ResponseEntity.ok(revenueData);
    }

    /**
     * Endpoint para obtener el rendimiento de los espacios del proveedor.
     */
    @GetMapping("/space-performance")
    public ResponseEntity<List<ProviderSpacePerformanceDto>> getSpacePerformance(@RequestParam String uid) {
        List<ProviderSpacePerformanceDto> data = dashboardProviderService.getSpacePerformance(uid);
        return ResponseEntity.ok(data);
    }
    
    /**
     * Endpoint temporal para debug de cálculos de ingresos brutos vs netos
     */
    @GetMapping("/debug-revenue")
    public ResponseEntity<Map<String, Object>> debugRevenue(@RequestParam String uid) {
        Map<String, Object> debug = new HashMap<>();
        try {
            UserEntity provider = userRepository.findByFirebaseUid(uid)
                    .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
            
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            LocalDateTime now = LocalDateTime.now();
            
            debug.put("providerId", provider.getId());
            debug.put("providerUid", uid);
            debug.put("dateRange", "Desde " + thirtyDaysAgo + " hasta " + now);
            
            // === CONSULTAS DIRECTAS ===
            
            // Obtener todas las facturas pagadas del proveedor en el rango de fechas
            List<InvoiceEntity> allInvoices = invoiceRepository.findAll().stream()
                    .filter(inv -> {
                        boolean isProviderInvoice = false;
                        if (inv.getRentalContract() != null && 
                            inv.getRentalContract().getSpace() != null &&
                            inv.getRentalContract().getSpace().getOwner() != null &&
                            inv.getRentalContract().getSpace().getOwner().getId().equals(provider.getId())) {
                            isProviderInvoice = true;
                        }
                        if (inv.getBooking() != null && 
                            inv.getBooking().getSpace() != null &&
                            inv.getBooking().getSpace().getOwner() != null &&
                            inv.getBooking().getSpace().getOwner().getId().equals(provider.getId())) {
                            isProviderInvoice = true;
                        }
                        
                        return isProviderInvoice && 
                               inv.getStatus() == InvoiceStatus.PAID &&
                               inv.getIssueDate() != null &&
                               inv.getIssueDate().isAfter(thirtyDaysAgo);
                    })
                    .toList();
            
            List<Map<String, Object>> invoiceDetails = new ArrayList<>();
            double totalFromInvoices = 0.0;
            
            for (InvoiceEntity invoice : allInvoices) {
                Map<String, Object> invoiceInfo = new HashMap<>();
                invoiceInfo.put("invoiceId", invoice.getId());
                invoiceInfo.put("totalAmount", invoice.getTotalAmount());
                invoiceInfo.put("refundAmount", invoice.getRefundAmount());
                invoiceInfo.put("status", invoice.getStatus());
                invoiceInfo.put("issueDate", invoice.getIssueDate());
                invoiceInfo.put("description", invoice.getDescription());
                invoiceInfo.put("type", invoice.getType());
                
                // Agregar info del contrato o reserva
                if (invoice.getRentalContract() != null) {
                    RentalContractEntity contract = invoice.getRentalContract();
                    invoiceInfo.put("contractId", contract.getId());
                    invoiceInfo.put("monthlyAmount", contract.getMonthlyAmount());
                    invoiceInfo.put("depositAmount", contract.getDepositAmount());
                    invoiceInfo.put("spaceName", contract.getSpace().getName());
                }
                if (invoice.getBooking() != null) {
                    BookingEntity booking = invoice.getBooking();
                    invoiceInfo.put("bookingId", booking.getId());
                    invoiceInfo.put("spaceName", booking.getSpace().getName());
                }
                
                invoiceDetails.add(invoiceInfo);
                totalFromInvoices += (invoice.getTotalAmount() != null ? invoice.getTotalAmount() : 0.0);
            }
            
            debug.put("invoices", invoiceDetails);
            debug.put("totalFromInvoicesCalculated", totalFromInvoices);
            
            // === COMPARAR CON MÉTODOS REPOSITORY ===
            Double contractGrossFromRepo = invoiceRepository.sumGrossRevenueFromContractsByProviderId(
                    provider.getId(), thirtyDaysAgo, now);
            Double bookingInvoiceGrossFromRepo = invoiceRepository.sumGrossRevenueFromBookingsByProviderId(
                    provider.getId(), thirtyDaysAgo, now);
            
            debug.put("contractGrossFromRepository", contractGrossFromRepo);
            debug.put("bookingInvoiceGrossFromRepository", bookingInvoiceGrossFromRepo);
            debug.put("totalGrossFromRepository", 
                (contractGrossFromRepo != null ? contractGrossFromRepo : 0.0) + 
                (bookingInvoiceGrossFromRepo != null ? bookingInvoiceGrossFromRepo : 0.0));
            debug.put("explanation", "Si las facturas muestran $1,098 pero esperabas $998, " +
                                    "probablemente hay amenities ($100) incluidas en el contrato");
            
            return ResponseEntity.ok(debug);
            
        } catch (Exception e) {
            debug.put("error", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(debug);
        }
    }

    /**
     * Endpoint para debuggear facturas de contratos del cliente
     */
    @GetMapping("/debug-invoices")
    public ResponseEntity<Map<String, Object>> debugInvoices(@RequestParam String uid) {
        Map<String, Object> debugInfo = new HashMap<>();
        
        try {
            UserEntity client = userRepository.findByFirebaseUid(uid)
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
            
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            LocalDateTime now = LocalDateTime.now();
            
            List<InvoiceEntity> allContractInvoices = invoiceRepository.findAllContractInvoicesByClientId(
                    client.getId(), thirtyDaysAgo, now);
            
            Map<String, List<Map<String, Object>>> invoicesByStatus = new HashMap<>();
            double totalByStatus = 0.0;
            
            for (InvoiceEntity invoice : allContractInvoices) {
                String status = invoice.getStatus().name();
                if (!invoicesByStatus.containsKey(status)) {
                    invoicesByStatus.put(status, new ArrayList<>());
                }
                
                Map<String, Object> invoiceInfo = new HashMap<>();
                invoiceInfo.put("id", invoice.getId());
                invoiceInfo.put("invoiceNumber", invoice.getInvoiceNumber());
                invoiceInfo.put("totalAmount", invoice.getTotalAmount());
                invoiceInfo.put("issueDate", invoice.getIssueDate());
                invoiceInfo.put("dueDate", invoice.getDueDate());
                invoiceInfo.put("contractId", invoice.getRentalContract() != null ? invoice.getRentalContract().getId() : null);
                
                invoicesByStatus.get(status).add(invoiceInfo);
                
                if ("PAID".equals(status)) {
                    totalByStatus += invoice.getTotalAmount() != null ? invoice.getTotalAmount() : 0.0;
                }
            }
            
            debugInfo.put("clientId", client.getId());
            debugInfo.put("clientFirebaseUid", uid);
            debugInfo.put("dateRange", Map.of("from", thirtyDaysAgo, "to", now));
            debugInfo.put("totalInvoicesFound", allContractInvoices.size());
            debugInfo.put("invoicesByStatus", invoicesByStatus);
            debugInfo.put("totalPaidAmount", totalByStatus);
            debugInfo.put("status", "success");
            
        } catch (Exception e) {
            debugInfo.put("status", "error");
            debugInfo.put("message", e.getMessage());
        }
        
        return ResponseEntity.ok(debugInfo);
    }

 
} 