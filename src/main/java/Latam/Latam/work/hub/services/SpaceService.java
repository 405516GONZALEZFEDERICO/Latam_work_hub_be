package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.dtos.common.SpaceDto;
import Latam.Latam.work.hub.dtos.common.FiltersSpaceDto;
import Latam.Latam.work.hub.dtos.common.SpaceResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
@Service
public interface SpaceService {
boolean createSpace(SpaceDto spaceDto,List<MultipartFile> images) throws Exception;
boolean updateSpace(Long spaceId, SpaceDto spaceDto, List<MultipartFile> images) throws Exception;
Page<SpaceResponseDto>findSpacesFiltered(FiltersSpaceDto filters, Pageable pageable);
SpaceResponseDto findSpaceById(Long id);
    Page<SpaceResponseDto> findSpacesByOwnerUid(String uid, FiltersSpaceDto filters, Pageable pageable);

}
