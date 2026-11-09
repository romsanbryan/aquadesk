package romsanbryan.aquadesk.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import romsanbryan.aquadesk.dto.request.InfoRequest;
import romsanbryan.aquadesk.dto.response.EventResponse;
import romsanbryan.aquadesk.dto.response.InfoResponse;
import romsanbryan.aquadesk.dto.response.NextEventResponse;
import romsanbryan.aquadesk.exception.ResourceNotFoundException;
import romsanbryan.aquadesk.exception.ServiceException;
import romsanbryan.aquadesk.exception.ValidationException;
import romsanbryan.aquadesk.mapper.EventMapper;
import romsanbryan.data.entities.Aquarium;
import romsanbryan.data.entities.Co2;
import romsanbryan.data.entities.Event;
import romsanbryan.data.entities.Info;
import romsanbryan.data.entities.Notifications;
import romsanbryan.data.repositories.AquariumRepository;
import romsanbryan.data.repositories.Co2Repository;
import romsanbryan.data.repositories.EventRepository;
import romsanbryan.data.repositories.InfoRepository;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServicesTest {

    @Mock private EventRepository eventRepository;
    @Mock private NotificationServices notificationServices;
    @Mock private Co2Repository co2Repository;
    @Mock private InfoRepository infoRepository;
    @Mock private DataUpdatePublisher updatePublisher;
    @Mock private AquariumRepository aquariumRepository;

    private EventServices service;

    @BeforeEach
    void setUp() {
        service = new EventServices(eventRepository, notificationServices, co2Repository,
                infoRepository, new EventMapper(), updatePublisher, aquariumRepository);
    }

    @Test
    void aquariumUpcomingEventsHandleExplicitCo2AndMissingEvents() {
        Notifications co2Notification = notification("co2:Liquid Carbon", 4, null);
        Notifications missing = notification("unknown", 4, null);
        Notifications nullParameter = notification(null, 2, null);
        when(notificationServices.findByActiveTrue(8)).thenReturn(List.of(co2Notification, missing, nullParameter));
        Event last = event(14, "CO2", "co2", LocalDate.now().minusDays(1));
        Co2 co2 = new Co2();
        co2.setEvent(last);
        when(co2Repository.findFirstByProductIgnoreCaseAndEventAquariumIdOrderByEventStartDateDesc("Liquid Carbon", 8))
                .thenReturn(Optional.of(co2));
        when(co2Repository.findFirstByProductIgnoreCaseAndEventAquariumIdOrderByEventStartDateDesc("unknown", 8))
                .thenReturn(Optional.empty());
        when(eventRepository.findFirstByAquariumIdAndTypeOrderByStartDateDesc(8, "unknown")).thenReturn(Optional.empty());
        when(eventRepository.findFirstByAquariumIdAndTypeOrderByStartDateDesc(8, null)).thenReturn(Optional.empty());

        List<NextEventResponse> result = service.getUpcomingEvents(8);

        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(item -> "unknown".equals(item.type())
                && item.lastDate() == null && item.nextDate() == null && item.remainingDays() == -1));
        assertTrue(result.stream().anyMatch(item -> "co2:Liquid Carbon".equals(item.type())
                && item.lastDate().equals(last.getStartDate())));
        assertTrue(result.stream().anyMatch(item -> item.type() == null && item.remainingDays() == -1));
        verify(co2Repository, never()).findFirstByProductIgnoreCaseAndEventAquariumIdOrderByEventStartDateDesc("co2:Liquid Carbon", 8);
    }

    @Test
    void configuredWeekdaySchedulesTheFirstMatchingDayAfterTheLastEvent() {
        Notifications notification = notification("manteiment", 5, 1 << (DayOfWeek.WEDNESDAY.getValue() - 1));
        Event last = event(90, "Maintenance", "manteiment", LocalDate.of(2026, 3, 2));
        when(notificationServices.findByActiveTrue(15)).thenReturn(List.of(notification));
        when(co2Repository.findFirstByProductIgnoreCaseAndEventAquariumIdOrderByEventStartDateDesc(
                "manteiment", 15)).thenReturn(Optional.empty());
        when(infoRepository.findByObservationAndAquariumId("manteiment", 15)).thenReturn(List.of());
        when(eventRepository.findFirstByAquariumIdAndTypeOrderByStartDateDesc(15, "manteiment")).thenReturn(Optional.of(last));

        NextEventResponse result = service.getUpcomingEvents(15).get(0);

        assertEquals(LocalDate.of(2026, 3, 4), result.nextDate());
    }

    @Test
    void handlesAnEmptyLatestEventWhenTheCalculationHelperIsCalledDirectly() throws Exception {
        Method calculate = EventServices.class.getDeclaredMethod(
                "calculateNextEvent", Notifications.class, Optional.class);
        calculate.setAccessible(true);

        NextEventResponse result = (NextEventResponse) calculate.invoke(
                service, notification("unknown", 3, 0), Optional.empty());

        assertEquals("unknown", result.type());
        assertNull(result.lastDate());
        assertNull(result.nextDate());
        assertEquals(-1, result.remainingDays());
    }

    @Test
    void convenienceConstructorsKeepLegacyAquariumLookupBehavior() {
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        EventServices constructorWithoutPublisher = new EventServices(eventRepository, notificationServices,
                co2Repository, infoRepository, new EventMapper());
        EventServices constructorWithPublisher = new EventServices(eventRepository, notificationServices,
                co2Repository, infoRepository, new EventMapper(), updatePublisher);

        Event first = constructorWithoutPublisher.createEvent(21, "Legacy", LocalDate.of(2026, 3, 1), "info");
        Event second = constructorWithPublisher.createEvent(22, "Legacy", LocalDate.of(2026, 3, 2), "info");

        assertEquals(21, first.getAquarium().getId());
        assertEquals(22, second.getAquarium().getId());
    }

    @Test
    void aquariumUpcomingEventsMatchInfoAndFallBackToEventType() {
        Notifications infoNotification = notification("Feeding", 3, null);
        Notifications legacyNotification = notification("manteiment", 1, null);
        when(notificationServices.findByActiveTrue(5)).thenReturn(List.of(infoNotification, legacyNotification));
        when(co2Repository.findFirstByProductIgnoreCaseAndEventAquariumIdOrderByEventStartDateDesc(anyString(), eq(5)))
                .thenReturn(Optional.empty());
        Info info = new Info();
        info.setIdEvent(72);
        when(infoRepository.findByObservationAndAquariumId("Feeding", 5)).thenReturn(List.of(info));
        Event infoEvent = event(72, "Feed", "info", LocalDate.now());
        when(eventRepository.findByIdAndAquariumId(72, 5)).thenReturn(Optional.of(infoEvent));
        when(infoRepository.findByObservationAndAquariumId("manteiment", 5)).thenReturn(List.of());
        Event legacy = event(73, "Maintenance", "manteiment", LocalDate.now().minusDays(1));
        when(eventRepository.findFirstByAquariumIdAndTypeOrderByStartDateDesc(5, "manteiment")).thenReturn(Optional.of(legacy));

        List<NextEventResponse> results = service.getUpcomingEvents(5);

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(result -> result.type().equals("Feeding")
                && result.lastDate().equals(infoEvent.getStartDate())));
        assertTrue(results.stream().anyMatch(result -> result.type().equals("manteiment")
                && result.nextDate().equals(LocalDate.now())));
    }

    @Test
    void aquariumUpcomingEventsResolveExplicitInfoPrefix() {
        Notifications notification = notification("info:Plants", 2, null);
        when(notificationServices.findByActiveTrue(12)).thenReturn(List.of(notification));
        when(co2Repository.findFirstByProductIgnoreCaseAndEventAquariumIdOrderByEventStartDateDesc("info:Plants", 12))
                .thenReturn(Optional.empty());
        Info info = new Info();
        info.setIdEvent(45);
        when(infoRepository.findByObservationAndAquariumId("Plants", 12)).thenReturn(List.of(info));
        Event latest = event(45, "Plants", "info", LocalDate.now().minusDays(3));
        when(eventRepository.findByIdAndAquariumId(45, 12)).thenReturn(Optional.of(latest));

        NextEventResponse result = service.getUpcomingEvents(12).get(0);

        assertEquals(latest.getStartDate(), result.lastDate());
        assertEquals(latest.getStartDate().plusDays(2), result.nextDate());
        verify(infoRepository, never()).findByObservationAndAquariumId("info:Plants", 12);
    }

    @Test
    void aquariumUpcomingFallsBackWhenExplicitInfoObservationIsNotFoundAndHandlesInvalidWeekdayMask() {
        Notifications missingInfo = notification("info:Missing", 3, null);
        Notifications invalidMask = notification("manteiment", 4, 256);
        Notifications zeroMask = notification("pH", 2, 0);
        when(notificationServices.findByActiveTrue(13)).thenReturn(List.of(missingInfo, invalidMask, zeroMask));
        when(co2Repository.findFirstByProductIgnoreCaseAndEventAquariumIdOrderByEventStartDateDesc(
                anyString(), eq(13))).thenReturn(Optional.empty());
        when(infoRepository.findByObservationAndAquariumId(anyString(), eq(13))).thenReturn(List.of());
        Event last = event(88, "Maintenance", "manteiment", LocalDate.now().minusDays(2));
        Event phLast = event(89, "pH", "parameters", LocalDate.now().minusDays(1));
        when(eventRepository.findFirstByAquariumIdAndTypeOrderByStartDateDesc(13, "manteiment")).thenReturn(Optional.of(last));
        when(eventRepository.findFirstByAquariumIdAndTypeOrderByStartDateDesc(13, "pH")).thenReturn(Optional.of(phLast));
        when(eventRepository.findFirstByAquariumIdAndTypeOrderByStartDateDesc(13, "info:Missing")).thenReturn(Optional.empty());

        List<NextEventResponse> results = service.getUpcomingEvents(13);

        NextEventResponse missing = results.stream().filter(item -> "info:Missing".equals(item.type()))
                .findFirst().orElseThrow();
        assertNull(missing.nextDate());
        NextEventResponse fallback = results.stream().filter(item -> "manteiment".equals(item.type()))
                .findFirst().orElseThrow();
        assertEquals(last.getStartDate().plusDays(4), fallback.nextDate());
        NextEventResponse zeroMaskResult = results.stream().filter(item -> "pH".equals(item.type()))
                .findFirst().orElseThrow();
        assertEquals(phLast.getStartDate().plusDays(2), zeroMaskResult.nextDate());
    }

    @Test
    void listsAquariumDateRangeEventsAndGroupsSameTypeAndDate() {
        LocalDate day = LocalDate.of(2026, 5, 1);
        when(eventRepository.findByAquariumIdAndStartDateBetween(9, day, day)).thenReturn(List.of(
                event(1, "one", "info", day), event(2, "two", "INFO", day),
                event(3, "hidden", "calendar", day), event(4, "hidden", "evento", day),
                event(5, "hidden", " ", day), event(6, "hidden", null, day)));

        List<EventResponse> results = service.getEvents(9, day.atStartOfDay(), day.atTime(12, 0));

        assertEquals(2, results.size());
        assertEquals(2, results.stream().mapToInt(EventResponse::count).sum());
        verify(eventRepository).findByAquariumIdAndStartDateBetween(9, day, day);
    }

    @Test
    void rejectsMissingAndReversedDateRangesBeforeRepositoryAccess() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 2, 0, 0);
        assertThrows(ValidationException.class, () -> service.getEvents(1, null, start));
        assertThrows(ValidationException.class, () -> service.getEvents(1, start, null));
        assertThrows(ValidationException.class, () -> service.getEvents(1, start.plusDays(1), start));
        verifyNoInteractions(eventRepository);
    }

    @Test
    void readsCo2ByDayAndSkipsEventsWithoutCo2Records() {
        LocalDate day = LocalDate.of(2026, 7, 4);
        Event first = event(1, "first", "co2", day);
        Event second = event(2, "second", "co2", day);
        Co2 record = new Co2();
        record.setEvent(first);
        when(eventRepository.findByAquariumIdAndStartDateAndType(4, day, "co2")).thenReturn(List.of(first, second));
        when(co2Repository.findByIdEvent(1)).thenReturn(Optional.of(record));
        when(co2Repository.findByIdEvent(2)).thenReturn(Optional.empty());

        assertEquals(List.of(record), service.getCo2ByDay(4, day).stream().toList());
        verify(eventRepository).findByAquariumIdAndStartDateAndType(4, day, "co2");
    }

    @Test
    void createsInfoAndLiveRecordsWithDefaultAndExplicitDates() {
        LocalDate chosenDate = LocalDate.of(2026, 8, 9);
        Aquarium aquarium = new Aquarium();
        aquarium.setId(6);
        when(aquariumRepository.findById(6)).thenReturn(Optional.of(aquarium));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            saved.setId(91);
            return saved;
        });

        service.addInfo(6, new InfoRequest(chosenDate, "INFO", "healthy", "Clear water"));
        service.addInfo(6, new InfoRequest(null, "FISH", "ok", "Fish active"));

        org.mockito.ArgumentCaptor<Event> events = org.mockito.ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, times(2)).save(events.capture());
        assertEquals(chosenDate, events.getAllValues().get(0).getStartDate());
        assertEquals("info", events.getAllValues().get(0).getType());
        assertTrue(events.getAllValues().get(0).getClickeable());
        assertEquals(LocalDate.now(), events.getAllValues().get(1).getStartDate());
        assertEquals("live", events.getAllValues().get(1).getType());
        assertSame(aquarium, events.getAllValues().get(1).getAquarium());

        org.mockito.ArgumentCaptor<Info> infos = org.mockito.ArgumentCaptor.forClass(Info.class);
        verify(infoRepository, times(2)).save(infos.capture());
        assertEquals(91, infos.getAllValues().get(0).getIdEvent());
        assertEquals("Clear water", infos.getAllValues().get(0).getObservation());
        verify(updatePublisher, times(2)).publish("event");
    }

    @Test
    void addInfoAndCreateEventFailWhenAquariumDoesNotExist() {
        when(aquariumRepository.findById(22)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.addInfo(22, new InfoRequest(LocalDate.now(), "INFO", null, null)));
        assertThrows(ResourceNotFoundException.class,
                () -> service.createEvent(22, "test", LocalDate.now(), "test"));
        verify(eventRepository, never()).save(any());
        verifyNoInteractions(infoRepository, updatePublisher);
    }

    @Test
    void readsAquariumInfoAndWrapsRepositoryFailures() {
        LocalDate day = LocalDate.of(2026, 9, 4);
        Event event = event(14, "Info", "info", day);
        Info info = info(3, "plants", "plants", "growing");
        info.setIdEvent(14);
        when(eventRepository.findByAquariumIdAndStartDateAndType(3, day, "info")).thenReturn(List.of(event));
        when(eventRepository.findByAquariumIdAndStartDateAndType(3, day, "live")).thenReturn(List.of());
        when(infoRepository.findByIdEventAndAquariumId(14, 3)).thenReturn(Optional.of(info));

        assertEquals(List.of(new InfoResponse(3, 14, "plants", "plants", "growing")),
                service.getInfoByDay(3, day).stream().toList());

        when(eventRepository.findByAquariumIdAndStartDateAndType(3, day, "info"))
                .thenThrow(new IllegalArgumentException("query failure"));
        assertThrows(ServiceException.class, () -> service.getInfoByDay(3, day));
    }

    @Test
    void createsScopedEventsAndSupportsLegacyDefaultAquariumConstructorBehavior() {
        Aquarium aquarium = new Aquarium();
        aquarium.setId(4);
        when(aquariumRepository.findById(4)).thenReturn(Optional.of(aquarium));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event created = service.createEvent(4, "Water change", LocalDate.of(2026, 1, 2), "maintenance");

        assertEquals("Water change", created.getTitle());
        assertEquals("maintenance", created.getType());
        assertSame(aquarium, created.getAquarium());
        assertTrue(created.getClickeable());
        verify(eventRepository).save(created);
    }

    @Test
    void deletesEventsAndPublishesUpdatesOnlyAfterSuccessfulScopedDelete() {
        Event found = event(20, "Info", "info", LocalDate.now());
        when(eventRepository.findByIdAndAquariumId(20, 2)).thenReturn(Optional.of(found));

        service.deleteEventById(2, 20);

        verify(eventRepository).delete(found);
        verify(updatePublisher).publish("event");
        when(eventRepository.findByIdAndAquariumId(21, 2)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.deleteEventById(2, 21));
        verify(updatePublisher).publish("event");
    }

    @Test
    void getsScopedEventsAndDateTypeQueries() {
        Event found = event(15, "Info", "info", LocalDate.now());
        when(eventRepository.findByIdAndAquariumId(15, 3)).thenReturn(Optional.of(found));
        assertSame(found, service.getEvent(3, 15));
        when(eventRepository.findByIdAndAquariumId(16, 3)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getEvent(3, 16));

        LocalDate date = LocalDate.of(2026, 10, 1);
        when(eventRepository.findByAquariumIdAndStartDateAndType(3, date, "photo")).thenReturn(List.of(found));
        assertEquals(List.of(found), service.findByStartDateAndType(3, date, "photo"));
    }

    private static Notifications notification(String parameter, int days, Integer weekdays) {
        Notifications notification = new Notifications();
        notification.setParameter(parameter);
        notification.setValue(days);
        notification.setWeekdays(weekdays);
        return notification;
    }

    private static Event event(int id, String title, String type, LocalDate date) {
        Event event = new Event(title, date, type);
        event.setId(id);
        return event;
    }

    private static Info info(int id, String observation, String type, String status) {
        Info info = new Info();
        info.setId(id);
        info.setIdEvent(id == 1 ? 11 : id);
        info.setObservation(observation);
        info.setType(type);
        info.setStatus(status);
        return info;
    }
}
