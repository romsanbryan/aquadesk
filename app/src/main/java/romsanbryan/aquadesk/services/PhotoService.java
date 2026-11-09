package romsanbryan.aquadesk.services;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import romsanbryan.aquadesk.mapper.PhotoMapper;
import romsanbryan.aquadesk.exception.ResourceNotFoundException;
import romsanbryan.aquadesk.exception.ServiceException;
import romsanbryan.data.entities.Event;
import romsanbryan.data.entities.Photo;
import romsanbryan.data.repositories.PhotoRepository;

import java.time.LocalDate;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PhotoService {

    private static final Logger log = LoggerFactory.getLogger(PhotoService.class);
    private final EventServices eventServices;

    private final PhotoRepository photoRepository;
    private final PhotoMapper photoMapper;
    private final DataUpdatePublisher updatePublisher;

    @Autowired
    public PhotoService(EventServices eventServices,
                        PhotoRepository photoRepository, PhotoMapper photoMapper,
                        DataUpdatePublisher updatePublisher) {
        this.eventServices = eventServices;
        this.photoRepository = photoRepository;
        this.photoMapper = photoMapper;
        this.updatePublisher = updatePublisher;
    }

    public void createPhotoEvent(Integer aquariumId, LocalDate date, MultipartFile photoFile) {
        log.info("Creating photo event for {}", date);

        Event event = eventServices.createEvent(aquariumId, "Photo", date, "photo");

        try {
            photoRepository.save(photoMapper.createPhoto(event, photoFile));
            updatePublisher.publish("photo");
        } catch (IOException e) {
            throw new ServiceException("Unable to read the uploaded photo.", e);
        }
    }

    public List<Map<String, Object>> getPhotoIdsByDay(Integer aquariumId, LocalDate fecha) {
        log.debug("Loading photo identifiers for {}", fecha);
        return eventServices.findByStartDateAndType(aquariumId, fecha, "photo")
                .stream()
                .flatMap(event -> photoRepository.findByEventId(event.getId()).stream())
                .filter(photo -> photo.getPhotoData() != null && photo.getPhotoData().length > 0)
                .map(photo -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", photo.getId());
                    map.put("eventId", photo.getEventId());
                    map.put("createdAt", photo.getCreatedAt() != null ? photo.getCreatedAt().toString() : "");
                    return map;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getLastPhotos(Integer aquariumId, int limit) {
        return mapPhotos(photoRepository.findAllByAquariumId(aquariumId,
                PageRequest.of(0, Math.max(1, limit), Sort.by(Sort.Direction.DESC, "createdAt"))).getContent());
    }

    private List<Map<String, Object>> mapPhotos(List<Photo> photos) {
        return photos.stream()
                .filter(photo -> photo.getPhotoData() != null && photo.getPhotoData().length > 0)
                .map(photo -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", photo.getId());
                    map.put("eventId", photo.getEventId());
                    map.put("createdAt", photo.getCreatedAt() != null ? photo.getCreatedAt().toString() : "");
                    return map;
                })
                .collect(Collectors.toList());
    }

    public Photo getPhoto(Integer aquariumId, Integer id) {
        log.debug("Loading photo with id {}", id);
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Photo was not found with id " + id));
        try {
            eventServices.getEvent(aquariumId, photo.getEventId());
        } catch (RuntimeException e) {
            throw new ResourceNotFoundException("Photo was not found with id " + id);
        }
        return photo;
    }

    @Transactional
    public void deletePhoto(Integer aquariumId, Integer id) {
        log.info("Deleting photo with id {}", id);
        Photo photo = getPhoto(aquariumId, id);
        photoRepository.deleteById(id);
        if (photoRepository.findByEventId(photo.getEventId()).isEmpty()) {
            eventServices.deleteEventById(aquariumId, photo.getEventId());
        }
        updatePublisher.publish("photo");
    }
}