package romsanbryan.aquadesk.controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import romsanbryan.aquadesk.exception.ValidationException;
import romsanbryan.aquadesk.services.PhotoService;
import romsanbryan.data.entities.Photo;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/aquariums/{aquariumId}/photos")
public class PhotoController {
    private final PhotoService photoService;
    public PhotoController(PhotoService photoService) { this.photoService = photoService; }

    @PostMapping
    public ResponseEntity<?> uploadPhoto(@RequestParam("fecha") String date, @RequestParam("foto") MultipartFile photoFile,
                                         @PathVariable Integer aquariumId) {
        photoService.createPhotoEvent(aquariumId, parseDate(date), photoFile);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException exception) {
            throw new ValidationException("The photo date must use the ISO format yyyy-MM-dd.", exception);
        }
    }

    @GetMapping("/by-day/{date}")
    public ResponseEntity<?> getPhotosByDay(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                            @PathVariable Integer aquariumId) {
        return ResponseEntity.ok(photoService.getPhotoIdsByDay(aquariumId, date));
    }
    @GetMapping("/{photoId}/image")
    public ResponseEntity<?> getImage(@PathVariable Integer aquariumId, @PathVariable Integer photoId) {
        Photo photo = photoService.getPhoto(aquariumId, photoId);
        return ResponseEntity.ok()
                .header("Content-Type", photo.getMimeType())
                .body(photo.getPhotoData());
    }
    @DeleteMapping("/{photoId}")
    public ResponseEntity<?> deletePhoto(@PathVariable Integer aquariumId, @PathVariable Integer photoId) {
        photoService.deletePhoto(aquariumId, photoId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/last")
    public ResponseEntity<?> getLastPhotos(@RequestParam(required = false, defaultValue = "10") Integer limit,
                                           @PathVariable Integer aquariumId) {
        return ResponseEntity.ok(photoService.getLastPhotos(aquariumId, limit));
    }
}
