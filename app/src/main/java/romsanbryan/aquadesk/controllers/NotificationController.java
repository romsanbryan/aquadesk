package romsanbryan.aquadesk.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import romsanbryan.aquadesk.dto.request.NotificationRequest;
import romsanbryan.aquadesk.services.NotificationServices;
import romsanbryan.data.entities.Notifications;
import java.util.List;

@RestController
public class NotificationController {
    private final NotificationServices notificationServices;
    public NotificationController(NotificationServices notificationServices) { this.notificationServices = notificationServices; }

    @GetMapping("/api/aquariums/{aquariumId}/notifications")
    public List<Notifications> getAllNotifications(@PathVariable Integer aquariumId) { return notificationServices.getAllNotifications(aquariumId); }
    @PostMapping("/api/aquariums/{aquariumId}/notifications")
    public ResponseEntity<?> createNotification(@PathVariable Integer aquariumId,
                                                @Valid @RequestBody NotificationRequest req) {
        notificationServices.createNotification(aquariumId, req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    @PutMapping("/api/aquariums/{aquariumId}/notifications/{notificationId}")
    public ResponseEntity<?> updateNotification(@PathVariable Integer aquariumId,
                                                 @PathVariable Long notificationId,
                                                 @Valid @RequestBody NotificationRequest req) {
        notificationServices.updateNotification(aquariumId, notificationId, req);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/api/aquariums/{aquariumId}/notifications/{notificationId}")
    public ResponseEntity<?> deleteNotification(@PathVariable Integer aquariumId,
                                                @PathVariable Long notificationId) {
        notificationServices.deleteNotification(aquariumId, notificationId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/api/aquariums/{aquariumId}/notifications/active")
    public ResponseEntity<?> getNotificationsConfig(@PathVariable Integer aquariumId) {
        return ResponseEntity.ok(notificationServices.getAllNotificationsActives(aquariumId));
    }
    @GetMapping("/api/notifications")
    public ResponseEntity<?> getNotifications() {
        return ResponseEntity.ok(notificationServices.getNotifications());
    }
}
