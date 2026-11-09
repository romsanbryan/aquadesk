package romsanbryan.data.entities;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataEntityBehaviorTest {

    @Test
    void exposesAquariumLifeIdentityAndRelationship() {
        Aquarium aquarium = new Aquarium();
        AquariumLife life = new AquariumLife();

        life.setId(9);
        life.setAquarium(aquarium);

        assertEquals(9, life.getId());
        assertSame(aquarium, life.getAquarium());
    }

    @Test
    void maintainsWeekdayMaskAndNotificationProperties() {
        Notifications notification = new Notifications();
        LocalDateTime created = LocalDateTime.of(2026, 1, 2, 3, 4);
        Aquarium aquarium = new Aquarium();
        notification.setId(8L);
        notification.setAquarium(aquarium);
        notification.setCreationDate(created);
        notification.setParameter("pH");
        notification.setValue(4);
        notification.setActive(true);

        notification.addWeekday(DayOfWeek.MONDAY);
        notification.addWeekday(null);
        notification.addWeekday(DayOfWeek.FRIDAY);
        assertTrue(notification.isActiveOn(DayOfWeek.MONDAY));
        assertTrue(notification.isActiveOn(DayOfWeek.FRIDAY));
        assertFalse(notification.isActiveOn(DayOfWeek.SUNDAY));
        assertFalse(notification.isActiveOn(null));
        assertEquals(List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                notification.getWeekdaysAsSet().stream().toList());

        notification.removeWeekday(null);
        notification.removeWeekday(DayOfWeek.FRIDAY);
        assertEquals(List.of(DayOfWeek.MONDAY), notification.getWeekdaysAsSet().stream().toList());
        notification.setWeekdays(null);
        assertEquals(0, notification.getWeekdays());
        assertTrue(notification.getWeekdaysAsSet().isEmpty());

        assertEquals(8L, notification.getId());
        assertSame(aquarium, notification.getAquarium());
        assertEquals(created, notification.getCreationDate());
        assertEquals("pH", notification.getParameter());
        assertEquals(4, notification.getValue());
        assertTrue(notification.getActive());
    }

    @Test
    void assignsLegacyAquariumToNotificationsAndRangesOnlyWhenMissing() throws Exception {
        Notifications notification = new Notifications();
        invokePrePersist(notification, "assignDefaultAquariumForLegacyWrites");
        assertEquals(1, notification.getAquarium().getId());
        Aquarium existing = new Aquarium();
        existing.setId(17);
        notification.setAquarium(existing);
        invokePrePersist(notification, "assignDefaultAquariumForLegacyWrites");
        assertSame(existing, notification.getAquarium());

        ParametersRange range = new ParametersRange();
        invokePrePersist(range, "assignDefaultAquariumForLegacyWrites");
        assertEquals(1, range.getAquarium().getId());
        range.setAquarium(existing);
        invokePrePersist(range, "assignDefaultAquariumForLegacyWrites");
        assertSame(existing, range.getAquarium());
    }

    @Test
    void exposesAquariumTimestampsAndFilterCollection() {
        Aquarium aquarium = new Aquarium();
        LocalDateTime created = LocalDateTime.of(2026, 2, 3, 4, 5);
        LocalDateTime updated = created.plusDays(1);
        List<AquariumFilter> filters = List.of(new AquariumFilter());

        aquarium.setCreatedAt(created);
        aquarium.setUpdatedAt(updated);
        aquarium.setFilters(filters);

        assertEquals(created, aquarium.getCreatedAt());
        assertEquals(updated, aquarium.getUpdatedAt());
        assertSame(filters, aquarium.getFilters());
    }

    @Test
    void exposesParameterRangeIdentityAndAuditProperties() {
        ParametersRange range = new ParametersRange();
        LocalDateTime created = LocalDateTime.of(2026, 3, 4, 5, 6);
        LocalDateTime updated = created.plusDays(1);

        range.setId(5);
        range.setCreatedAt(created);
        range.setUpdatedAt(updated);

        assertEquals(5, range.getId());
        assertEquals(created, range.getCreatedAt());
        assertEquals(updated, range.getUpdatedAt());
    }

    @Test
    void exposesCo2IdentityAndEventMaintenanceRelationships() {
        Co2 co2 = new Co2();
        Event event = new Event();
        Maintenance maintenance = new Maintenance();
        co2.setId(6);
        event.setMaintenance(maintenance);

        assertEquals(6, co2.getId());
        assertSame(maintenance, event.getMaintenance());
        assertTrue(event.toString().startsWith("Event{"));
    }

    @Test
    void assignsLegacyAquariumToEventsOnlyWhenMissing() throws Exception {
        Event event = new Event();
        invokePrePersist(event, "assignDefaultAquariumForLegacyWrites");
        assertEquals(1, event.getAquarium().getId());

        Aquarium existing = new Aquarium();
        existing.setId(19);
        event.setAquarium(existing);
        invokePrePersist(event, "assignDefaultAquariumForLegacyWrites");
        assertSame(existing, event.getAquarium());
    }

    @Test
    void exposesRemainingEntityIdentityAndAuditProperties() {
        Filtre filter = new Filtre();
        AquariumTask task = new AquariumTask();
        Aquarium aquarium = new Aquarium();
        AquariumFilter aquariumFilter = new AquariumFilter();
        LocalDateTime created = LocalDateTime.of(2026, 4, 5, 6, 7);
        LocalDateTime updated = created.plusDays(1);

        filter.setId(3);
        task.setAquarium(aquarium);
        aquariumFilter.setId(4);
        aquariumFilter.setCreatedAt(created);
        aquariumFilter.setUpdatedAt(updated);

        assertEquals(3, filter.getId());
        assertSame(aquarium, task.getAquarium());
        assertEquals(4, aquariumFilter.getId());
        assertEquals(created, aquariumFilter.getCreatedAt());
        assertEquals(updated, aquariumFilter.getUpdatedAt());
    }

    @Test
    void exposesParameterRangeValuesWithoutChangingPrecision() {
        ParametersRange range = new ParametersRange();
        range.setLevel1(new BigDecimal("0.10"));

        assertEquals(new BigDecimal("0.10"), range.getLevel1());
    }

    private static void invokePrePersist(Object entity, String methodName) throws Exception {
        Method callback = entity.getClass().getDeclaredMethod(methodName);
        callback.setAccessible(true);
        callback.invoke(entity);
    }
}
