package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.dtos.common.SpaceDto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public interface SpaceService {
boolean createSpace(SpaceDto spaceDto,List<MultipartFile> images) throws Exception;
boolean updateSpace(SpaceDto spaceDto);
boolean deleteSpace(Long id);
SpaceDto getSpaceById(Long id);
List<SpaceDto> getAllSpaces();
List<SpaceDto> getSpacesByUserUid(String uid);
}
