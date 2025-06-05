package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.dtos.common.dashboard.cliente.ClientKpiCardsDto;
import Latam.Latam.work.hub.dtos.common.dashboard.cliente.ClientMonthlySpendingDto;
import Latam.Latam.work.hub.dtos.common.dashboard.cliente.ClientBookingsByTypeDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface DashboardClientService {
    ClientKpiCardsDto getKpiCardsData(String clientUid);
    List<ClientMonthlySpendingDto> getMonthlySpending(String clientUid, int lastNMonths);
    List<ClientBookingsByTypeDto> getBookingsBySpaceType(String clientUid);
} 