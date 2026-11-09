package romsanbryan.aquadesk.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import romsanbryan.aquadesk.dto.request.MaintenanceRequest;
import romsanbryan.aquadesk.dto.response.LastMaintenanceResponse;
import romsanbryan.aquadesk.dto.response.MaintenanceResponse;
import romsanbryan.aquadesk.exception.ResourceNotFoundException;
import romsanbryan.aquadesk.exception.ValidationException;
import romsanbryan.aquadesk.mapper.MaintenanceMapper;
import romsanbryan.data.entities.Event;
import romsanbryan.data.entities.Filtre;
import romsanbryan.data.entities.Maintenance;
import romsanbryan.data.repositories.FiltreRepository;
import romsanbryan.data.repositories.MaintenanceRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceTest {

    @Mock private MaintenanceRepository maintenanceRepository;
    @Mock private FiltreRepository filtreRepository;
    @Mock private EventServices eventServices;
    @Mock private DataUpdatePublisher updatePublisher;

    private MaintenanceService service;

    @BeforeEach
    void setUp() {
        service = new MaintenanceService(new MaintenanceMapper(), maintenanceRepository,
                filtreRepository, eventServices, updatePublisher);
    }

    @Test
    void latestMaintenanceReturnsNullWhenNoRecordExists() {
        when(maintenanceRepository.findLatestMaintenanceByAquariumId(4)).thenReturn(Optional.empty());

        assertNull(service.getLatestMaintenance(4));

        verifyNoInteractions(filtreRepository);
    }

    @Test
    void mapsLatestMaintenanceAndItsFiltersWithinAquarium() {
        Maintenance maintenance = maintenance(31, LocalDate.of(2026, 8, 5));
        Filtre filter = filter(7, 31, "Filter 1");
        when(maintenanceRepository.findLatestMaintenanceByAquariumId(6)).thenReturn(Optional.of(maintenance));
        when(filtreRepository.findByMaintenanceId(31)).thenReturn(List.of(filter));

        LastMaintenanceResponse scoped = service.getLatestMaintenance(6);

        assertEquals(maintenance.getEvent().getStartDate(), scoped.fecha());
        assertEquals(maintenance.getWater(), scoped.water());
        assertEquals(new BigDecimal("0.25"), scoped.primeDose());
        assertEquals("Trimmed", scoped.pruning());
        assertEquals("Weekly", scoped.observations());
        assertTrue(scoped.glass());
        assertEquals("Filter 1", scoped.filters().get(0).filterName());
        assertEquals(true, scoped.filters().get(0).sponge());
        verify(filtreRepository).findByMaintenanceId(31);
    }

    @Test
    void addsMaintenanceWithExplicitDateMapsBothFiltersAndPublishesOnce() {
        LocalDate date = LocalDate.of(2026, 9, 12);
        Event event = new Event();
        event.setId(55);
        event.setStartDate(date);
        when(eventServices.createEvent(8, "Maintenance", date, "manteiment")).thenReturn(event);
        when(maintenanceRepository.save(any(Maintenance.class))).thenAnswer(invocation -> {
            Maintenance saved = invocation.getArgument(0);
            saved.setId(77);
            return saved;
        });
        MaintenanceRequest request = request(date);

        service.addMaintenance(8, request);

        ArgumentCaptor<Maintenance> maintenanceCaptor = ArgumentCaptor.forClass(Maintenance.class);
        verify(maintenanceRepository).save(maintenanceCaptor.capture());
        Maintenance saved = maintenanceCaptor.getValue();
        assertSame(event, saved.getEvent());
        assertEquals(35, saved.getWater());
        assertEquals(new BigDecimal("0.25"), saved.getPrimeDose());
        assertTrue(saved.getGlass());
        assertEquals("Trim plants", saved.getPruning());
        assertEquals("Routine service", saved.getObservations());

        ArgumentCaptor<Filtre> filters = ArgumentCaptor.forClass(Filtre.class);
        verify(filtreRepository, times(2)).save(filters.capture());
        Filtre first = filters.getAllValues().get(0);
        assertEquals(77, first.getMaintenanceId());
        assertEquals("Filter 1", first.getFilterName());
        assertTrue(first.getSponge());
        assertFalse(first.getPerlon());
        assertTrue(first.getMech());
        assertTrue(first.getBio());
        assertFalse(first.getBioMech());
        Filtre second = filters.getAllValues().get(1);
        assertEquals("Filter 2", second.getFilterName());
        assertFalse(second.getSponge());
        assertTrue(second.getPerlon());
        assertFalse(second.getMech());
        assertTrue(second.getBio());
        assertTrue(second.getBioMech());
        verify(eventServices).createEvent(8, "Maintenance", date, "manteiment");
        verify(updatePublisher).publish("maintenance");
    }

    @Test
    void defaultsMissingDateWhenAddingAquariumMaintenance() {
        Event event = new Event();
        when(eventServices.createEvent(eq(8), eq("Maintenance"), any(LocalDate.class), eq("manteiment")))
                .thenAnswer(invocation -> {
                    assertEquals(LocalDate.now(), invocation.getArgument(2));
                    return event;
                });
        when(maintenanceRepository.save(any(Maintenance.class))).thenAnswer(invocation -> {
            Maintenance saved = invocation.getArgument(0);
            saved.setId(77);
            return saved;
        });

        service.addMaintenance(8, request(null));

        verify(eventServices).createEvent(8, "Maintenance", LocalDate.now(), "manteiment");
        verify(maintenanceRepository).save(any(Maintenance.class));
        verify(filtreRepository, times(2)).save(any(Filtre.class));
        verify(updatePublisher).publish("maintenance");
    }

    @Test
    void retrievesMaintenanceWithFiltersWithinAquarium() {
        Maintenance maintenance = maintenance(44, LocalDate.of(2026, 7, 7));
        maintenance.setId(19);
        Filtre filter = filter(2, 19, "Filter 2");
        when(maintenanceRepository.findByIdEventAndAquariumId(44, 5)).thenReturn(Optional.of(maintenance));
        when(filtreRepository.findByMaintenanceId(19)).thenReturn(List.of(filter));

        MaintenanceResponse scoped = service.getMaintenance(5, 44);

        assertEquals(19, scoped.id());
        assertEquals(40, scoped.water());
        assertEquals(new BigDecimal("0.25"), scoped.primeDose());
        assertTrue(scoped.glass());
        assertEquals("Trimmed", scoped.pruning());
        assertEquals("Weekly", scoped.observations());
        assertEquals("Filter 2", scoped.filters().get(0).filterName());
        verify(filtreRepository).findByMaintenanceId(19);
    }

    @Test
    void throwsNotFoundForMissingAquariumMaintenanceWithoutLoadingFilters() {
        when(maintenanceRepository.findByIdEventAndAquariumId(99, 2)).thenReturn(Optional.empty());

        ResourceNotFoundException scoped = assertThrows(ResourceNotFoundException.class,
                () -> service.getMaintenance(2, 99));

        assertTrue(scoped.getMessage().contains("99"));
        verifyNoInteractions(filtreRepository);
    }

    @Test
    void rejectsNegativeAmountsBeforeCreatingMaintenance() {
        LocalDate date = LocalDate.of(2026, 9, 12);
        MaintenanceRequest request = new MaintenanceRequest(date, -1, new BigDecimal("-0.1"),
                null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertThrows(ValidationException.class, () -> service.addMaintenance(8, request));

        verifyNoInteractions(eventServices, maintenanceRepository, filtreRepository, updatePublisher);
    }

    private static MaintenanceRequest request(LocalDate date) {
        return new MaintenanceRequest(date, 35, new BigDecimal("0.25"),
                true, "Trim plants", "Routine service",
                true, false, true, true, false,
                false, true, false, true, true);
    }

    private static Maintenance maintenance(int eventId, LocalDate date) {
        Maintenance maintenance = new Maintenance();
        maintenance.setEvent(new Event("Maintenance", date, "manteiment"));
        maintenance.getEvent().setId(eventId);
        maintenance.setId(eventId);
        maintenance.setWater(40);
        maintenance.setPrimeDose(new BigDecimal("0.25"));
        maintenance.setGlass(true);
        maintenance.setPruning("Trimmed");
        maintenance.setObservations("Weekly");
        return maintenance;
    }

    private static Filtre filter(int id, int maintenanceId, String name) {
        Filtre filter = new Filtre();
        filter.setId(id);
        filter.setMaintenanceId(maintenanceId);
        filter.setFilterName(name);
        filter.setSponge(true);
        filter.setPerlon(false);
        filter.setMech(true);
        filter.setBio(true);
        filter.setBioMech(false);
        return filter;
    }
}
