package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.dtos.common.AmenityDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface AmenityService {
    List<AmenityDto>getAllAmenities();
}
