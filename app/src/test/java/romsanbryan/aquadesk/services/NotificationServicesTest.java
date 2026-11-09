package romsanbryan.aquadesk.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import romsanbryan.aquadesk.dto.request.NotificationRequest;
import romsanbryan.aquadesk.dto.response.NotificationParameterResponse;
import romsanbryan.aquadesk.exception.ResourceNotFoundException;
import romsanbryan.aquadesk.exception.ServiceException;
import romsanbryan.data.entities.Aquarium;
import romsanbryan.data.entities.Notifications;
import romsanbryan.data.repositories.AquariumRepository;
import romsanbryan.data.repositories.NotificationsParameters;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServicesTest {

    @Mock
    private NotificationsParameters notificationsRepository;
    @Mock
    private ParameterServices parametersService;
    @Mock
    private DataUpdatePublisher updatePublisher;
    @Mock
    private AquariumRepository aquariumRepository;

    @InjectMocks
    private NotificationServices service;

    @Test
    void delegatesNotificationListQueriesToTheMatchingRepositoryMethods() {
        List<Notifications> notifications = List.of(new Notifications());
        when(notificationsRepository.findByAquariumIdAndActive(4, true)).thenReturn(notifications);
        when(notificationsRepository.findByAquariumId(4)).thenReturn(notifications);

        assertSame(notifications, service.getAllNotificationsActives(4));
        assertSame(notifications, service.findByActiveTrue(4));
        assertSame(notifications, service.getAllNotifications(4));

        verify(notificationsRepository, times(2)).findByAquariumIdAndActive(4, true);
        verify(notificationsRepository).findByAquariumId(4);
    }

    @Test
    void createsAquariumNotificationWithDefaultsAndNormalizesWeekdayAliases() {
        NotificationRequest req = new NotificationRequest("pH", 3, null,
                java.util.Arrays.asList(" Monday ", "TUE", "miercoles", "jue", "viernes", "sab", "Domingo",
                        null, "", "weekday-ish"), null);
        Aquarium aquarium = new Aquarium();
        when(aquariumRepository.findById(7)).thenReturn(Optional.of(aquarium));

        service.createNotification(7, req);

        ArgumentCaptor<Notifications> saved = ArgumentCaptor.forClass(Notifications.class);
        verify(notificationsRepository).save(saved.capture());
        Notifications notification = saved.getValue();
        assertEquals("pH", notification.getParameter());
        assertEquals(3, notification.getValue());
        assertTrue(notification.getActive());
        assertEquals(127, notification.getWeekdays());
        assertSame(aquarium, notification.getAquarium());
        verify(updatePublisher).publish("notification");
    }

    @Test
    void ignoresNullBlankAndUnknownWeekdayValues() {
        Aquarium aquarium = new Aquarium();
        when(aquariumRepository.findById(7)).thenReturn(Optional.of(aquarium));
        service.createNotification(7, new NotificationRequest("pH", 3, true,
                java.util.Arrays.asList(null, "", "   ", "not-a-weekday"), null));

        ArgumentCaptor<Notifications> saved = ArgumentCaptor.forClass(Notifications.class);
        verify(notificationsRepository).save(saved.capture());
        assertEquals(0, saved.getValue().getWeekdays());
        verify(updatePublisher).publish("notification");
    }

    @Test
    void coversEverySupportedEnglishAndSpanishWeekdayAlias() {
        Aquarium aquarium = new Aquarium();
        when(aquariumRepository.findById(7)).thenReturn(Optional.of(aquarium));
        service.createNotification(7, new NotificationRequest("pH", 3, true, List.of(
                "monday", "mon", "lunes", "lun",
                "tuesday", "tue", "martes", "mar",
                "wednesday", "wed", "miercoles", "mie",
                "thursday", "thu", "jueves", "jue",
                "friday", "fri", "viernes", "vie",
                "saturday", "sat", "sabado", "sab",
                "sunday", "sun", "domingo", "dom"), null));

        ArgumentCaptor<Notifications> saved = ArgumentCaptor.forClass(Notifications.class);
        verify(notificationsRepository).save(saved.capture());
        assertEquals(127, saved.getValue().getWeekdays());
    }

    @Test
    void explicitWeekdayMaskTakesPrecedenceAndScopedCreateUsesAquariumAndActiveValue() {
        Aquarium aquarium = new Aquarium();
        aquarium.setId(7);
        when(aquariumRepository.findById(7)).thenReturn(Optional.of(aquarium));
        NotificationRequest req = new NotificationRequest("KH", 5, false,
                List.of("monday", "sunday"), 65);

        service.createNotification(7, req);

        ArgumentCaptor<Notifications> saved = ArgumentCaptor.forClass(Notifications.class);
        verify(notificationsRepository).save(saved.capture());
        assertSame(aquarium, saved.getValue().getAquarium());
        assertFalse(saved.getValue().getActive());
        assertEquals(65, saved.getValue().getWeekdays());
        verify(aquariumRepository).findById(7);
        verify(updatePublisher).publish("notification");
    }

    @Test
    void scopedCreateWithNullMaskAndNoWeekdayListLeavesDefaultMaskAndMissingAquariumFails() {
        Aquarium aquarium = new Aquarium();
        aquarium.setId(2);
        when(aquariumRepository.findById(2)).thenReturn(Optional.of(aquarium));
        service.createNotification(2, new NotificationRequest("GH", 1, true, List.of(), null));

        ArgumentCaptor<Notifications> saved = ArgumentCaptor.forClass(Notifications.class);
        verify(notificationsRepository).save(saved.capture());
        assertEquals(0, saved.getValue().getWeekdays());

        when(aquariumRepository.findById(3)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                service.createNotification(3, new NotificationRequest("GH", 1, null, null, null)));
        verify(notificationsRepository, times(1)).save(any());
        verify(updatePublisher, times(1)).publish("notification");
    }

    @Test
    void wrapsRepositoryFailuresAndPreservesDomainExceptionsDuringCreate() {
        Aquarium aquarium = new Aquarium();
        when(aquariumRepository.findById(9)).thenReturn(Optional.of(aquarium));
        when(notificationsRepository.save(any())).thenThrow(new IllegalStateException("storage unavailable"));
        ServiceException wrapped = assertThrows(ServiceException.class,
                () -> service.createNotification(9, new NotificationRequest("pH", 2, true, null, null)));
        assertEquals("Unable to create notification.", wrapped.getMessage());
        assertInstanceOf(IllegalStateException.class, wrapped.getCause());

        ResourceNotFoundException notFound = new ResourceNotFoundException("missing aquarium");
        when(aquariumRepository.findById(10)).thenThrow(notFound);
        assertSame(notFound, assertThrows(ResourceNotFoundException.class, () ->
                service.createNotification(10, new NotificationRequest("pH", 2, true, null, null))));
        verify(updatePublisher, never()).publish("notification");
    }

    @Test
    void updatesScopedNotificationAndPreservesOptionalFieldsWhenOmitted() {
        Notifications existing = notification("old", 4, true, 9);
        when(notificationsRepository.findByIdAndAquariumId(5L, 3)).thenReturn(Optional.of(existing));

        service.updateNotification(3, 5L, new NotificationRequest("new", 8, null, null, null));

        assertEquals("new", existing.getParameter());
        assertEquals(8, existing.getValue());
        assertTrue(existing.getActive());
        assertEquals(9, existing.getWeekdays());
        verify(notificationsRepository).save(existing);
        verify(updatePublisher).publish("notification");
    }

    @Test
    void updatesScopedNotificationAndAppliesWeekdayListOrExplicitMask() {
        Notifications fromList = notification("old", 1, true, 0);
        Notifications fromMask = notification("old", 1, true, 0);
        when(notificationsRepository.findByIdAndAquariumId(6L, 8)).thenReturn(Optional.of(fromList));
        when(notificationsRepository.findByIdAndAquariumId(7L, 8)).thenReturn(Optional.of(fromMask));

        service.updateNotification(8, 6L,
                new NotificationRequest("temp", 2, false, List.of("mon", "FRI", "noday"), null));
        service.updateNotification(8, 7L,
                new NotificationRequest("temp", 3, true, List.of("monday"), 32));

        assertEquals(17, fromList.getWeekdays());
        assertFalse(fromList.getActive());
        assertEquals(32, fromMask.getWeekdays());
        assertTrue(fromMask.getActive());
        verify(notificationsRepository).save(fromList);
        verify(notificationsRepository).save(fromMask);
        verify(updatePublisher, times(2)).publish("notification");
    }

    @Test
    void missingNotificationUpdateOrDeleteRaisesNotFoundWithoutPublishing() {
        when(notificationsRepository.findByIdAndAquariumId(11L, 4)).thenReturn(Optional.empty());
        when(notificationsRepository.findByIdAndAquariumId(12L, 4)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateNotification(4, 11L,
                new NotificationRequest("pH", 2, null, null, null)));
        assertThrows(ResourceNotFoundException.class, () -> service.updateNotification(4, 12L,
                new NotificationRequest("pH", 2, null, null, null)));
        assertThrows(ResourceNotFoundException.class, () -> service.deleteNotification(4, 11L));
        assertThrows(ResourceNotFoundException.class, () -> service.deleteNotification(4, 12L));

        verify(notificationsRepository, never()).save(any());
        verify(notificationsRepository, never()).delete(any());
        verify(updatePublisher, never()).publish(anyString());
    }

    @Test
    void deletesNotificationInAquariumScopeAndPublishesUpdate() {
        Notifications scoped = notification("KH", 2, false, 3);
        when(notificationsRepository.findByIdAndAquariumId(21L, 6)).thenReturn(Optional.of(scoped));

        service.deleteNotification(6, 21L);

        verify(notificationsRepository).delete(scoped);
        verify(updatePublisher).publish("notification");
    }

    @Test
    void aggregatesNotificationParametersWithAquariumNames() {
        Aquarium first = new Aquarium();
        first.setId(1);
        first.setName("Alpha");
        Aquarium second = new Aquarium();
        second.setId(2);
        second.setName("Beta");
        when(aquariumRepository.findAllByOrderByNameAsc()).thenReturn(List.of(first, second));
        when(parametersService.getNotificationParameters(1)).thenReturn(List.of(
                new NotificationParameterResponse("pH", "low"),
                new NotificationParameterResponse("KH", "high")));
        when(parametersService.getNotificationParameters(2)).thenReturn(List.of(
                new NotificationParameterResponse("NH3", "Very high")));

        List<NotificationParameterResponse> result = service.getNotifications();

        assertEquals(List.of(
                new NotificationParameterResponse("pH", "low", "Alpha"),
                new NotificationParameterResponse("KH", "high", "Alpha"),
                new NotificationParameterResponse("NH3", "Very high", "Beta")), result);
        verify(aquariumRepository).findAllByOrderByNameAsc();
        verify(parametersService).getNotificationParameters(1);
        verify(parametersService).getNotificationParameters(2);
    }

    private static Notifications notification(String parameter, int value, boolean active, int weekdays) {
        Notifications notification = new Notifications();
        notification.setParameter(parameter);
        notification.setValue(value);
        notification.setActive(active);
        notification.setWeekdays(weekdays);
        return notification;
    }
}
