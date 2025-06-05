package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.dtos.common.dashboard.proveedor.ProviderKpiCardsDto;
import Latam.Latam.work.hub.dtos.common.dashboard.proveedor.ProviderMonthlyRevenueDto;
import Latam.Latam.work.hub.dtos.common.dashboard.proveedor.ProviderSpacePerformanceDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface DashboardProviderService {
    ProviderKpiCardsDto getKpiCardsData(String providerUid);
    List<ProviderMonthlyRevenueDto> getMonthlyRevenue(String providerUid, int lastNMonths);
    List<ProviderSpacePerformanceDto> getSpacePerformance(String providerUid);
} 