package romsanbryan.aquadesk.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import romsanbryan.aquadesk.dto.request.NotificationRequest;
import romsanbryan.aquadesk.dto.response.NotificationParameterResponse;
import romsanbryan.aquadesk.exception.AquadeskException;
import romsanbryan.aquadesk.exception.ResourceNotFoundException;
import romsanbryan.aquadesk.exception.ServiceException;
import romsanbryan.data.entities.Aquarium;
import romsanbryan.data.entities.Notifications;
import romsanbryan.data.repositories.AquariumRepository;
import romsanbryan.data.repositories.NotificationsParameters;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Locale;

@Service
public class NotificationServices {

    private static final Logger log = LoggerFactory.getLogger(NotificationServices.class);
    @Autowired
    private NotificationsParameters notificationsRepository;
    @Autowired
    private ParameterServices parametersService;
    @Autowired
    private DataUpdatePublisher updatePublisher = new DataUpdatePublisher();
    @Autowired
    private AquariumRepository aquariumRepository;

    public NotificationServices() {
    }


    public List<Notifications> getAllNotificationsActives(Integer aquariumId) {
        return notificationsRepository.findByAquariumIdAndActive(aquariumId, true);
    }

    public List<Notifications> findByActiveTrue(Integer aquariumId) {
        return notificationsRepository.findByAquariumIdAndActive(aquariumId, true);
    }


    public List<Notifications> getAllNotifications(Integer aquariumId) {
        return notificationsRepository.findByAquariumId(aquariumId);
    }

    public void createNotification(Integer aquariumId, NotificationRequest req) {
        try {
            log.info("Creating notification for parameter {}", req.parameter());
            Notifications n = new Notifications();
            n.setParameter(req.parameter());
            n.setValue(req.value());
            n.setActive(req.active() == null || req.active());
            n.setAquarium(getAquarium(aquariumId));

            if (req.weekdays() != null) {
                n.setWeekdays(req.weekdays());
            } else if (req.weekdaysList() != null && !req.weekdaysList().isEmpty()) {
                n.setWeekdays(computeWeekdaysMask(req.weekdaysList()));
            }
            notificationsRepository.save(n);
            updatePublisher.publish("notification");
        } catch (AquadeskException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Failed to create notification for parameter {}", req.parameter(), e);
            throw new ServiceException("Unable to create notification.", e);
        }
    }

    public void updateNotification(Integer aquariumId, Long id, NotificationRequest req) {
        log.info("Updating notification with id {}", id);

        notificationsRepository.findByIdAndAquariumId(id, aquariumId).ifPresentOrElse(existing -> updateExisting(existing, req), () -> {
            throw new ResourceNotFoundException("Notification was not found with id " + id);
        });


    }

    public void deleteNotification(Integer aquariumId, Long id) {
        log.info("Deleting notification with id {}", id);
        notificationsRepository.findByIdAndAquariumId(id, aquariumId)
                .ifPresentOrElse(notificationsRepository::delete, () -> {
                    throw new ResourceNotFoundException("Notification was not found with id " + id);
                });
        updatePublisher.publish("notification");
    }

    public List<NotificationParameterResponse> getNotifications() {
        return aquariumRepository.findAllByOrderByNameAsc().stream()
                .flatMap(aquarium -> parametersService.getNotificationParameters(aquarium.getId()).stream()
                        .map(notification -> withAquariumName(notification, aquarium.getName())))
                .toList();
    }

    private NotificationParameterResponse withAquariumName(NotificationParameterResponse notification,
                                                           String aquariumName) {
        return new NotificationParameterResponse(
                notification.parameter(), notification.value(), aquariumName);
    }

    private int computeWeekdaysMask(List<String> days) {
        int mask = 0;
        for (String s : days) {
            if (s == null) continue;
            DayOfWeek d = parseDay(s.trim());
            if (d != null) {
                int idx = d.getValue() - 1; // Monday=1 -> bit0
                mask |= (1 << idx);
            } else {
                log.warn("Ignoring unsupported weekday value: {}", s);
            }
        }
        return mask;
    }

    private DayOfWeek parseDay(String s) {
        if (s.isBlank()) return null;
        String normalized = s.trim().toLowerCase(Locale.ROOT);
        // Support English names and abbreviations.
        return switch (normalized) {
            case "monday", "mon", "lunes", "lun" -> DayOfWeek.MONDAY;
            case "tuesday", "tue", "martes", "mar" -> DayOfWeek.TUESDAY;
            case "wednesday", "wed", "miercoles", "mie" -> DayOfWeek.WEDNESDAY;
            case "thursday", "thu", "jueves", "jue" -> DayOfWeek.THURSDAY;
            case "friday", "fri", "viernes", "vie" -> DayOfWeek.FRIDAY;
            case "saturday", "sat", "sabado", "sab" -> DayOfWeek.SATURDAY;
            case "sunday", "sun", "domingo", "dom" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    private void updateExisting(Notifications existing, NotificationRequest req) {
        existing.setParameter(req.parameter());
        existing.setValue(req.value());
        if (req.active() != null) existing.setActive(req.active());
        if (req.weekdays() != null) existing.setWeekdays(req.weekdays());
        else if (req.weekdaysList() != null) existing.setWeekdays(computeWeekdaysMask(req.weekdaysList()));
        notificationsRepository.save(existing);
        updatePublisher.publish("notification");
    }

    private Aquarium getAquarium(Integer aquariumId) {
        return aquariumRepository.findById(aquariumId)
                .orElseThrow(() -> new ResourceNotFoundException("Aquarium was not found with id " + aquariumId));
    }


}
