package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.dtos.common.dashboard.cliente.ClientKpiCardsDto;
import Latam.Latam.work.hub.dtos.common.dashboard.cliente.ClientMonthlySpendingDto;
import Latam.Latam.work.hub.dtos.common.dashboard.cliente.ClientBookingsByTypeDto;
import Latam.Latam.work.hub.services.DashboardClientService;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.entities.InvoiceEntity;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.repositories.InvoiceRepository;
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

@RestController
@RequestMapping("/api/dashboard-client")
@PreAuthorize("hasRole('CLIENTE')")
@RequiredArgsConstructor
public class DashboardClientController {
    private final DashboardClientService dashboardClientService;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;

    /**
     * Endpoint para obtener los datos de las Tarjetas de KPI del cliente.
     */
    @GetMapping("/kpi-cards")
    public ResponseEntity<ClientKpiCardsDto> getKpiCards(@RequestParam String uid) {
        ClientKpiCardsDto kpiData = dashboardClientService.getKpiCardsData(uid);
        return ResponseEntity.ok(kpiData);
    }

    /**
     * Endpoint para el gráfico de líneas de gastos mensuales del cliente.
     * @param uid UID del cliente
     * @param months Número de meses hacia atrás (opcional, defecto: 12).
     */
    @GetMapping("/monthly-spending")
    public ResponseEntity<List<ClientMonthlySpendingDto>> getMonthlySpending(
            @RequestParam String uid,
            @RequestParam(name = "months", defaultValue = "12", required = false) int months) {
        if (months <= 0 || months > 60) {
            months = 12;
        }
        List<ClientMonthlySpendingDto> spendingData = dashboardClientService.getMonthlySpending(uid, months);
        return ResponseEntity.ok(spendingData);
    }

    /**
     * Endpoint para obtener las reservas por tipo de espacio del cliente.
     */
    @GetMapping("/bookings-by-space-type")
    public ResponseEntity<List<ClientBookingsByTypeDto>> getBookingsBySpaceType(@RequestParam String uid) {
        List<ClientBookingsByTypeDto> data = dashboardClientService.getBookingsBySpaceType(uid);
        return ResponseEntity.ok(data);
    }

    /**
     * Endpoint temporal para debugging de gastos del cliente
     */
    @GetMapping("/debug-spending")
    public ResponseEntity<Map<String, Object>> debugSpending(@RequestParam String uid) {
        Map<String, Object> debugInfo = new HashMap<>();
        
        try {
            // Obtener KPI cards con logs
            ClientKpiCardsDto kpiData = dashboardClientService.getKpiCardsData(uid);
            debugInfo.put("kpiCards", kpiData);
            
            // Obtener gastos mensuales con logs  
            List<ClientMonthlySpendingDto> monthlySpending = dashboardClientService.getMonthlySpending(uid, 12);
            debugInfo.put("monthlySpending", monthlySpending);
            
            debugInfo.put("status", "success");
            debugInfo.put("message", "Revisa los logs del servidor para información detallada");
            
        } catch (Exception e) {
            debugInfo.put("status", "error");
            debugInfo.put("message", e.getMessage());
        }
        
        return ResponseEntity.ok(debugInfo);
    }

    /**
     * Endpoint para debuggear facturas de contratos del cliente
     */
    @GetMapping("/debug-contract-invoices")
    public ResponseEntity<Map<String, Object>> debugContractInvoices(@RequestParam String uid) {
        Map<String, Object> debugInfo = new HashMap<>();
        
        try {
            UserEntity client = userRepository.findByFirebaseUid(uid)
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
            
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            LocalDateTime now = LocalDateTime.now();
            
            // Buscar TODAS las facturas de contratos del cliente (sin filtro de estado)
            List<InvoiceEntity> allContractInvoices = invoiceRepository.findAllContractInvoicesByClientId(
                    client.getId(), thirtyDaysAgo, now);
            
            // Agrupar por estado
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