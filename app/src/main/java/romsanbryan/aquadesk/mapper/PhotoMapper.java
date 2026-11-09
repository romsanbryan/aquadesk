package romsanbryan.aquadesk.mapper;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import romsanbryan.data.entities.Event;
import romsanbryan.data.entities.Photo;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class PhotoMapper {

    public Photo createPhoto(Event event, MultipartFile photoFile) throws IOException {
        Photo photo = new Photo();
        photo.setEventId(event.getId());
        photo.setPhotoData(photoFile.getBytes());
        photo.setMimeType(photoFile.getContentType() != null ? photoFile.getContentType() : "image/jpeg");
        photo.setCreatedAt(LocalDateTime.now());
        return photo;
    }


}