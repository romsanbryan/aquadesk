package romsanbryan.aquadesk.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import romsanbryan.aquadesk.dto.request.InfoRequest;
import romsanbryan.aquadesk.dto.response.EventResponse;
import romsanbryan.aquadesk.dto.response.InfoResponse;
import romsanbryan.aquadesk.dto.response.NextEventResponse;
import romsanbryan.aquadesk.exception.ServiceException;
import romsanbryan.aquadesk.exception.ResourceNotFoundException;
import romsanbryan.aquadesk.exception.ValidationException;
import romsanbryan.aquadesk.mapper.EventMapper;
import romsanbryan.data.entities.Co2;
import romsanbryan.data.entities.Event;
import romsanbryan.data.entities.Info;
import romsanbryan.data.entities.Notifications;
import romsanbryan.data.repositories.Co2Repository;
import romsanbryan.data.repositories.EventRepository;
import romsanbryan.data.repositories.InfoRepository;
import romsanbryan.data.repositories.AquariumRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EventServices {

    private static final Logger log = LoggerFactory.getLogger(EventServices.class);
    private final EventRepository eventRepository;
    private final NotificationServices notificationServices;

    private final Co2Repository co2Repository;
    private final InfoRepository infoRepository;


    private final EventMapper eventMapper;
    private final DataUpdatePublisher updatePublisher;
    private final AquariumRepository aquariumRepository;


    @Autowired
    public EventServices(EventRepository eventRepository,
                         NotificationServices notificationRepository,
                         Co2Repository co2Repository,
                         InfoRepository infoRepository,
                         EventMapper eventMapper,
                         DataUpdatePublisher updatePublisher,
                         AquariumRepository aquariumRepository) {

        this.eventRepository = eventRepository;
        this.notificationServices = notificationRepository;
        this.co2Repository = co2Repository;
        this.infoRepository = infoRepository;

        this.eventMapper = eventMapper;
        this.updatePublisher = updatePublisher;
        this.aquariumRepository = aquariumRepository;
    }

    public EventServices(EventRepository eventRepository,
                         NotificationServices notificationRepository,
                         Co2Repository co2Repository,
                         InfoRepository infoRepository,
                         EventMapper eventMapper) {
        this(eventRepository, notificationRepository, co2Repository, infoRepository,
                eventMapper, new DataUpdatePublisher());
    }

    public EventServices(EventRepository eventRepository,
                         NotificationServices notificationRepository,
                         Co2Repository co2Repository,
                         InfoRepository infoRepository,
                         EventMapper eventMapper,
                         DataUpdatePublisher updatePublisher) {
        this(eventRepository, notificationRepository, co2Repository, infoRepository,
                eventMapper, updatePublisher, null);
    }

    public List<NextEventResponse> getUpcomingEvents(Integer aquariumId) {
        log.debug("Calculating upcoming events from active notifications");
        List<Notifications> notifications = notificationServices.findByActiveTrue(aquariumId);

        return notifications.stream()
                .map(notification -> calculateUpcomingEvent(notification, aquariumId))
                .sorted(Comparator.comparingLong(NextEventResponse::remainingDays))
                .toList();
    }

    NextEventResponse calculateUpcomingEvent(Notifications notification, Integer aquariumId) {
        // Support CO2 notifications using an explicit product or a matching database entry.
        String param = notification.getParameter();

        Optional<Event> latestEvent = Optional.empty();


        if (param != null) {
            // Explicit "co2:PRODUCT" format.
            if (param.toLowerCase().startsWith("co2:")) {
                String product = param.substring(4);
                latestEvent = co2Repository
                        .findFirstByProductIgnoreCaseAndEventAquariumIdOrderByEventStartDateDesc(product, aquariumId)
                        .map(Co2::getEvent);
            }

            // Without a prefix, interpret the parameter as a product.
            if (latestEvent.isEmpty()) {
                latestEvent = co2Repository
                        .findFirstByProductIgnoreCaseAndEventAquariumIdOrderByEventStartDateDesc(param, aquariumId)
                        .map(Co2::getEvent);
            }
            // INFO notifications are matched by observation.
            if (latestEvent.isEmpty()) {
                // Explicit "info:OBSERVATION" format.
                if (param.toLowerCase().startsWith("info:")) {
                    String obs = param.substring(5);
                    List<Info> infos = infoRepository.findByObservationAndAquariumId(obs, aquariumId);
                    if (!infos.isEmpty()) {
                        latestEvent = eventRepository.findByIdAndAquariumId(infos.get(0).getIdEvent(), aquariumId);
                    }
                }

                // Without a prefix, interpret the parameter directly as an observation.
                if (latestEvent.isEmpty()) {
                    List<Info> infos2 = infoRepository.findByObservationAndAquariumId(param, aquariumId);
                    if (!infos2.isEmpty()) {
                        latestEvent = eventRepository.findByIdAndAquariumId(infos2.get(0).getIdEvent(), aquariumId);
                    }
                }
            }
        }

        // Fall back to the event type for legacy notifications.
        if (latestEvent.isEmpty()) {
            latestEvent = eventRepository.findFirstByAquariumIdAndTypeOrderByStartDateDesc(aquariumId, param);
        }

        if (latestEvent.isEmpty()) {
            // No event has been registered for this type or product.
            return new NextEventResponse(
                    notification.getParameter(),
                    null,
                    null,
                    -1L // Indicates that no data is available.
            );
        }

        return calculateNextEvent(notification, latestEvent);
    }

    private NextEventResponse calculateNextEvent(Notifications notification, Optional<Event> latestEvent) {
        if (latestEvent.isEmpty()) return new NextEventResponse(notification.getParameter(), null, null, -1L);
        LocalDate lastDate = latestEvent.get().getStartDate();
        LocalDate nextDate;
        LocalDate today = LocalDate.now();

        // Find the next configured weekday when a weekday mask is present.
        int weekdays = notification.getWeekdays();
        if (weekdays == 0) {
            nextDate = lastDate.plusDays(notification.getValue());
        } else {
            nextDate = null;
            for (int i = 1; i <= 7; i++) {
                LocalDate candidate = lastDate.plusDays(i);
                int dayIndex = candidate.getDayOfWeek().getValue() - 1;
                if ((weekdays & (1 << dayIndex)) != 0) {
                    nextDate = candidate;
                    break;
                }
            }
            if (nextDate == null) {
                nextDate = lastDate.plusDays(notification.getValue());
            }
        }

        long remainingDays = ChronoUnit.DAYS.between(today, nextDate);

        return new NextEventResponse(
                notification.getParameter(),
                lastDate,
                nextDate,
                remainingDays
        );
    }

    public List<EventResponse> getEvents(Integer aquariumId, LocalDateTime start, LocalDateTime end) {
        validateDateRange(start, end);
        log.info("Loading events between {} and {}", start, end);
        List<Event> events = eventRepository.findByAquariumIdAndStartDateBetween(aquariumId, start.toLocalDate(), end.toLocalDate());
        events = events.stream()
                .filter(e -> {
                    String t = e.getType();
                    return t != null && !t.isBlank() && !t.equalsIgnoreCase("calendar") && !t.equalsIgnoreCase("evento");
                })
                .collect(Collectors.toList());
        return eventMapper.toGroupedDtoList(events);

    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new ValidationException("Event date range must include a start and end, with end on or after start.");
        }
    }


    public Collection<Co2> getCo2ByDay(Integer aquariumId, LocalDate date) {
        return
                eventRepository.findByAquariumIdAndStartDateAndType(aquariumId, date, "co2")
                        .stream()
                        .map(event -> co2Repository.findByIdEvent(event.getId()))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList()

                        );

    }


    public void addInfo(Integer aquariumId, InfoRequest req) {
        log.info("Creating information event with type {}", req.type());
        LocalDate date = req.date() != null ? req.date() : LocalDate.now();

        String eventType = "INFO".equals(req.type()) ? "info" : "live";


        Event event = new Event();
        event.setTitle("Info");
        event.setStartDate(date);
        event.setType(eventType);  // "info" or "live".
        event.setClickeable(true);
        event.setAquarium(getAquarium(aquariumId));
        event = eventRepository.save(event);

        Info info = new Info();
        info.setIdEvent(event.getId());
        info.setType(req.type());  // "INFO", "FISH", or "plants".
        info.setStatus(req.status());
        info.setObservation(req.observation());
        infoRepository.save(info);
        updatePublisher.publish("event");

    }

    public Collection<InfoResponse> getInfoByDay(Integer aquariumId, LocalDate date) {
        try {

            List<Event> events = new ArrayList<>();
            events.addAll(eventRepository.findByAquariumIdAndStartDateAndType(aquariumId, date, "info"));
            events.addAll(eventRepository.findByAquariumIdAndStartDateAndType(aquariumId, date, "live"));


            return events.stream()
                    .map(event -> infoRepository.findByIdEventAndAquariumId(event.getId(), aquariumId))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(f -> new InfoResponse(
                            f.getId(), f.getIdEvent(), f.getObservation(), f.getType(), f.getStatus()
                    ))
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            throw new ServiceException("Unable to retrieve information.", e);
        }
    }

    public Event createEvent(Integer aquariumId, String name, LocalDate date, String type) {
        log.debug("Creating event with type {} on {}", type, date);
        Event event = eventMapper.mapEvent(name, date, type);
        event.setAquarium(getAquarium(aquariumId));
        return eventRepository.save(event);
    }

    public void deleteEventById(Integer aquariumId, Integer id) {
        log.info("Deleting event with id {}", id);
        Event event = eventRepository.findByIdAndAquariumId(id, aquariumId)
                .orElseThrow(() -> new ResourceNotFoundException("Event was not found with id " + id));
        eventRepository.delete(event);
        updatePublisher.publish("event");
    }

    public Event getEvent(Integer aquariumId, Integer id) {
        return eventRepository.findByIdAndAquariumId(id, aquariumId)
                .orElseThrow(() -> new ResourceNotFoundException("Event was not found with id " + id));
    }

    public Collection<Event> findByStartDateAndType(Integer aquariumId, LocalDate date, String eventType) {
        return eventRepository.findByAquariumIdAndStartDateAndType(aquariumId, date, eventType);
    }

    private romsanbryan.data.entities.Aquarium getAquarium(Integer aquariumId) {
        if (aquariumRepository == null) {
            romsanbryan.data.entities.Aquarium aquarium = new romsanbryan.data.entities.Aquarium();
            aquarium.setId(aquariumId);
            return aquarium;
        }
        return aquariumRepository.findById(aquariumId)
                .orElseThrow(() -> new ResourceNotFoundException("Aquarium was not found with id " + aquariumId));
    }


}