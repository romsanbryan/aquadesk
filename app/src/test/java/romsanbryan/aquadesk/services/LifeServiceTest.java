package romsanbryan.aquadesk.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import romsanbryan.aquadesk.dto.request.LifeQuantityRequest;
import romsanbryan.aquadesk.dto.request.LifeRequest;
import romsanbryan.aquadesk.dto.response.LifeResponse;
import romsanbryan.aquadesk.exception.ResourceNotFoundException;
import romsanbryan.aquadesk.exception.ServiceException;
import romsanbryan.aquadesk.exception.ValidationException;
import romsanbryan.data.entities.Aquarium;
import romsanbryan.data.entities.AquariumLife;
import romsanbryan.data.repositories.AquariumLifeRepository;
import romsanbryan.data.repositories.AquariumRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LifeServiceTest {

    @Mock private AquariumLifeRepository lifeRepository;
    @Mock private AquariumRepository aquariumRepository;
    @Mock private EventServices eventServices;
    @Mock private DataUpdatePublisher updatePublisher;
    @Mock private MessageSource messageSource;

    private LifeService service;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        service = new LifeService(lifeRepository, aquariumRepository, eventServices, updatePublisher, messageSource);
    }

    @AfterEach
    void clearLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void listsLifeForAquariumAndMapsEveryResponseField() {
        Aquarium aquarium = aquarium();
        AquariumLife fish = life(4, aquarium, "fish", "Tetra", "Paracheirodon innesi", 8,
                "pH 6.5", "https://example.test/tetra", "Community fish");
        when(aquariumRepository.findById(7)).thenReturn(Optional.of(aquarium));
        when(lifeRepository.findByAquariumIdOrderByCategoryAscCommonNameAsc(7)).thenReturn(List.of(fish));

        assertEquals(List.of(new LifeResponse(4, "fish", "Tetra", "Paracheirodon innesi", 8,
                "pH 6.5", "https://example.test/tetra", "Community fish")), service.getLife(7));
        verify(lifeRepository).findByAquariumIdOrderByCategoryAscCommonNameAsc(7);
    }

    @Test
    void preservesNotFoundErrorsAndWrapsUnexpectedReadFailures() {
        when(aquariumRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getLife(99));

        when(aquariumRepository.findById(7)).thenReturn(Optional.of(aquarium()));
        IllegalStateException failure = new IllegalStateException("repository failure");
        when(lifeRepository.findByAquariumIdOrderByCategoryAscCommonNameAsc(7)).thenThrow(failure);
        ServiceException wrapped = assertThrows(ServiceException.class, () -> service.getLife(7));
        assertSame(failure, wrapped.getCause());
    }

    @Test
    void rejectsMissingOrInvalidLifeDetailsBeforePersistence() {
        assertThrows(ValidationException.class, () -> service.addLife(7, null));
        assertThrows(ValidationException.class, () -> service.addLife(7,
                new LifeRequest(null, "fish", null, 1, null, null, null, null)));
        assertThrows(ValidationException.class, () -> service.addLife(7,
                new LifeRequest("  ", "fish", null, 1, null, null, null, null)));
        assertThrows(ValidationException.class, () -> service.addLife(7,
                new LifeRequest("Tetra", "fish", null, null, null, null, null, null)));
        assertThrows(ValidationException.class, () -> service.addLife(7,
                new LifeRequest("Tetra", "fish", null, 0, null, null, null, null)));

        when(aquariumRepository.findById(7)).thenReturn(Optional.of(aquarium()));
        assertThrows(ValidationException.class, () -> service.addLife(7,
                new LifeRequest("Tetra", null, null, 1, null, null, null, null)));
        assertThrows(ValidationException.class, () -> service.addLife(7,
                new LifeRequest("Tetra", "snail", null, 1, null, null, null, null)));
        verifyNoInteractions(lifeRepository, eventServices, updatePublisher);
    }

    @Test
    void rejectsNonHttpOrMalformedTechnicalSheetUrls() {
        when(aquariumRepository.findById(7)).thenReturn(Optional.of(aquarium()));
        assertThrows(ValidationException.class, () -> service.addLife(7, request("file:///guide", LocalDate.now())));
        assertThrows(ValidationException.class, () -> service.addLife(7, request("http:/guide", LocalDate.now())));
        ValidationException malformed = assertThrows(ValidationException.class,
                () -> service.addLife(7, request("http://bad host", LocalDate.now())));
        assertNotNull(malformed.getCause());
        verifyNoInteractions(lifeRepository, eventServices, updatePublisher);
    }

    @Test
    void addsFishAndPlantsWithNormalizedValuesAndPublishesChangeEvents() {
        Aquarium aquarium = aquarium();
        when(aquariumRepository.findById(7)).thenReturn(Optional.of(aquarium));
        when(lifeRepository.save(any(AquariumLife.class))).thenAnswer(invocation -> {
            AquariumLife saved = invocation.getArgument(0);
            saved.setId(12);
            return saved;
        });
        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Fish added").thenReturn("x".repeat(120));

        LocalDate chosenDate = LocalDate.of(2026, 8, 11);
        LifeResponse fish = service.addLife(7, new LifeRequest("  Tetra ", " FISH ", " Latin name ",
                4, " pH 6.5 ", " https://example.test/fish ", " Notes ", chosenDate));
        LifeResponse plant = service.addLife(7,
                new LifeRequest(" Fern ", " Plant ", " ", 2, null, null, null, null));

        assertEquals(new LifeResponse(12, "fish", "Tetra", "Latin name", 4,
                "pH 6.5", "https://example.test/fish", "Notes"), fish);
        assertEquals("plant", plant.category());
        assertEquals("Fern", plant.commonName());
        ArgumentCaptor<AquariumLife> lives = ArgumentCaptor.forClass(AquariumLife.class);
        verify(lifeRepository, times(2)).save(lives.capture());
        assertSame(aquarium, lives.getAllValues().get(0).getAquarium());
        assertNull(lives.getAllValues().get(1).getScientificName());

        ArgumentCaptor<String> titles = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDate> dates = ArgumentCaptor.forClass(LocalDate.class);
        verify(eventServices, times(2)).createEvent(eq(7), titles.capture(), dates.capture(), anyString());
        assertEquals("Fish added", titles.getAllValues().get(0));
        assertEquals("x".repeat(100), titles.getAllValues().get(1));
        assertEquals(List.of(chosenDate, LocalDate.now()), dates.getAllValues());
        verify(updatePublisher, times(2)).publish("event");
    }

    @Test
    void wrapsUnexpectedWriteFailuresAndPreservesMissingAquariumErrors() {
        when(aquariumRepository.findById(7)).thenReturn(Optional.of(aquarium()));
        IllegalStateException failure = new IllegalStateException("save failed");
        when(lifeRepository.save(any(AquariumLife.class))).thenThrow(failure);
        ServiceException wrapped = assertThrows(ServiceException.class,
                () -> service.addLife(7, request(null, LocalDate.now())));
        assertSame(failure, wrapped.getCause());

        when(aquariumRepository.findById(8)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.addLife(8, request(null, LocalDate.now())));
        verifyNoInteractions(eventServices, updatePublisher);
    }

    @Test
    void validatesQuantityChangesAndProtectsIntegerBounds() {
        AquariumLife fish = life(4, aquarium(), "fish", "Tetra", null, 3, null, null, null);
        when(lifeRepository.findByIdAndAquariumId(4, 7)).thenReturn(Optional.of(fish));
        when(lifeRepository.findByIdAndAquariumId(5, 7)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.changeQuantity(7, 5,
                new LifeQuantityRequest(1, LocalDate.now())));
        assertThrows(ValidationException.class, () -> service.changeQuantity(7, 4, null));
        assertThrows(ValidationException.class, () -> service.changeQuantity(7, 4,
                new LifeQuantityRequest(null, LocalDate.now())));
        assertThrows(ValidationException.class, () -> service.changeQuantity(7, 4,
                new LifeQuantityRequest(0, LocalDate.now())));
        assertThrows(ValidationException.class, () -> service.changeQuantity(7, 4,
                new LifeQuantityRequest(-4, LocalDate.now())));

        fish.setQuantity(Integer.MAX_VALUE);
        assertThrows(ValidationException.class, () -> service.changeQuantity(7, 4,
                new LifeQuantityRequest(1, LocalDate.now())));
        verify(lifeRepository, never()).save(any(AquariumLife.class));
        verifyNoInteractions(eventServices, updatePublisher);
    }

    @Test
    void appliesPositiveAndNegativeQuantityChangesWithExplicitAndDefaultDates() {
        AquariumLife fish = life(4, aquarium(), "fish", "Tetra", null, 3, null, null, null);
        when(lifeRepository.findByIdAndAquariumId(4, 7)).thenReturn(Optional.of(fish));
        when(lifeRepository.save(any(AquariumLife.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("Fish changed");

        LocalDate chosenDate = LocalDate.of(2026, 8, 12);
        assertEquals(5, service.changeQuantity(7, 4, new LifeQuantityRequest(2, chosenDate)).quantity());
        assertEquals(4, service.changeQuantity(7, 4, new LifeQuantityRequest(-1, null)).quantity());

        ArgumentCaptor<String> titles = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDate> dates = ArgumentCaptor.forClass(LocalDate.class);
        verify(eventServices, times(2)).createEvent(eq(7), titles.capture(), dates.capture(), eq("life-fish"));
        assertEquals(List.of("Fish changed", "Fish changed"), titles.getAllValues());
        assertEquals(List.of(chosenDate, LocalDate.now()), dates.getAllValues());
        verify(updatePublisher, times(2)).publish("event");
    }

    @Test
    void wrapsUnexpectedQuantityLookupFailures() {
        IllegalStateException failure = new IllegalStateException("query failed");
        when(lifeRepository.findByIdAndAquariumId(4, 7)).thenThrow(failure);

        ServiceException wrapped = assertThrows(ServiceException.class,
                () -> service.changeQuantity(7, 4, new LifeQuantityRequest(1, LocalDate.now())));

        assertSame(failure, wrapped.getCause());
    }

    private LifeRequest request(String url, LocalDate date) {
        return new LifeRequest("Tetra", "fish", null, 1, null, url, null, date);
    }

    private static Aquarium aquarium() {
        Aquarium aquarium = new Aquarium();
        aquarium.setId(7);
        return aquarium;
    }

    private static AquariumLife life(Integer id, Aquarium aquarium, String category, String commonName,
                                     String scientificName, Integer quantity, String recommendedParameters,
                                     String technicalSheetUrl, String notes) {
        AquariumLife life = new AquariumLife();
        life.setId(id);
        life.setAquarium(aquarium);
        life.setCategory(category);
        life.setCommonName(commonName);
        life.setScientificName(scientificName);
        life.setQuantity(quantity);
        life.setRecommendedParameters(recommendedParameters);
        life.setTechnicalSheetUrl(technicalSheetUrl);
        life.setNotes(notes);
        return life;
    }
}
