package romsanbryan.aquadesk.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import romsanbryan.aquadesk.exception.ValidationException;
import romsanbryan.aquadesk.mapper.TreatmentMapper;
import romsanbryan.data.entities.Co2;
import romsanbryan.data.entities.Event;
import romsanbryan.data.repositories.Co2Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceTest {

    @Mock private EventServices eventServices;
    @Mock private Co2Repository co2Repository;
    @Mock private TreatmentMapper treatmentMapper;
    @Mock private DataUpdatePublisher updatePublisher;
    @InjectMocks private TreatmentService service;

    @Test
    void defaultsAquariumTreatmentDateAndUsesRealMapperForNumericValues() {
        when(eventServices.createEvent(eq(12), eq("Carbon"), any(LocalDate.class), eq("co2"))).thenAnswer(invocation -> {
            assertEquals(LocalDate.now(), invocation.getArgument(2));
            return new Event();
        });
        TreatmentService withRealMapper = new TreatmentService(eventServices, co2Repository,
                new TreatmentMapper(), updatePublisher);

        withRealMapper.createTreatment(12, "Carbon", "2,75 ml", null);

        org.mockito.ArgumentCaptor<Co2> saved = org.mockito.ArgumentCaptor.forClass(Co2.class);
        verify(co2Repository).save(saved.capture());
        assertEquals("Carbon", saved.getValue().getProduct());
        assertEquals(new BigDecimal("2.75"), saved.getValue().getValue());
        verify(updatePublisher).publish("treatment");
    }

    @Test
    void createsAquariumTreatmentAndUsesTheRequestedAquariumAndDate() {
        LocalDate date = LocalDate.of(2025, 5, 6);
        Event event = new Event();
        Co2 treatment = new Co2();
        when(eventServices.createEvent(12, "Carbon", date, "co2")).thenReturn(event);
        when(treatmentMapper.getCo2("Carbon", "", event)).thenReturn(treatment);

        service.createTreatment(12, "Carbon", "", date);

        verify(eventServices).createEvent(12, "Carbon", date, "co2");
        verify(treatmentMapper).getCo2("Carbon", "", event);
        verify(co2Repository).save(treatment);
        verify(updatePublisher).publish("treatment");
    }

    @Test
    void defaultsAquariumTreatmentDateWhenMissing() {
        when(eventServices.createEvent(eq(14), eq("Carbon"), any(LocalDate.class), eq("co2")))
                .thenAnswer(invocation -> {
                    assertEquals(LocalDate.now(), invocation.getArgument(2));
                    return new Event();
                });
        when(treatmentMapper.getCo2(eq("Carbon"), isNull(), any(Event.class))).thenReturn(new Co2());

        service.createTreatment(14, "Carbon", null, null);

        verify(co2Repository).save(any(Co2.class));
        verify(updatePublisher).publish("treatment");
    }

    @Test
    void invalidTreatmentValueIsRejectedBeforeSavingOrPublishing() {
        TreatmentService withRealMapper = new TreatmentService(eventServices, co2Repository,
                new TreatmentMapper(), updatePublisher);
        Event event = new Event();
        when(eventServices.createEvent(eq(1), eq("Carbon"), any(LocalDate.class), eq("co2"))).thenReturn(event);

        ValidationException thrown = assertThrows(ValidationException.class,
                () -> withRealMapper.createTreatment(1, "Carbon", "not a number", null));

        assertEquals("Treatment value must contain a valid number.", thrown.getMessage());
        verify(co2Repository, never()).save(any());
        verifyNoInteractions(updatePublisher);
    }

    @Test
    void eventMapperAndRepositoryFailuresPropagateWithoutPublishing() {
        RuntimeException eventFailure = new IllegalStateException("event failed");
        when(eventServices.createEvent(eq(1), eq("Carbon"), any(LocalDate.class), eq("co2")))
                .thenThrow(eventFailure);
        assertSame(eventFailure, assertThrows(IllegalStateException.class,
                () -> service.createTreatment(1, "Carbon", "1", null)));
        verifyNoInteractions(treatmentMapper, co2Repository, updatePublisher);

        Event event = new Event();
        RuntimeException mapperFailure = new IllegalArgumentException("mapping failed");
        doReturn(event).when(eventServices).createEvent(eq(1), eq("Carbon"), any(LocalDate.class), eq("co2"));
        when(treatmentMapper.getCo2("Carbon", "1", event)).thenThrow(mapperFailure);
        assertSame(mapperFailure, assertThrows(IllegalArgumentException.class,
                () -> service.createTreatment(1, "Carbon", "1", null)));
        verifyNoInteractions(co2Repository, updatePublisher);

        Co2 treatment = new Co2();
        RuntimeException saveFailure = new IllegalStateException("storage failed");
        doReturn(treatment).when(treatmentMapper).getCo2("Carbon", "1", event);
        when(co2Repository.save(treatment)).thenThrow(saveFailure);
        assertSame(saveFailure, assertThrows(IllegalStateException.class,
                () -> service.createTreatment(1, "Carbon", "1", null)));
        verify(updatePublisher, never()).publish(anyString());
    }
}
