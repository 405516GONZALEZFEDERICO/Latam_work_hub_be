package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.dtos.common.SpaceTypeDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface SpaceTypeService {
    List<SpaceTypeDto>getAllSpacesTypes();
}
