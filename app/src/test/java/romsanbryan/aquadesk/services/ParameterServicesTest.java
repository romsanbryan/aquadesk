package romsanbryan.aquadesk.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import romsanbryan.aquadesk.dto.request.ParametersRequest;
import romsanbryan.aquadesk.dto.request.RangeRequest;
import romsanbryan.aquadesk.dto.response.NotificationParameterResponse;
import romsanbryan.aquadesk.dto.response.ParametersResponse;
import romsanbryan.aquadesk.exception.ResourceNotFoundException;
import romsanbryan.aquadesk.exception.ServiceException;
import romsanbryan.aquadesk.mapper.ParameterMapper;
import romsanbryan.data.entities.Aquarium;
import romsanbryan.data.entities.Event;
import romsanbryan.data.entities.Parameters;
import romsanbryan.data.entities.ParametersRange;
import romsanbryan.data.repositories.AquariumRepository;
import romsanbryan.data.repositories.ParametersRangeRepository;
import romsanbryan.data.repositories.ParametersRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParameterServicesTest {

    @Mock
    private ParametersRepository parametersRepository;
    @Mock
    private ParametersRangeRepository rangeRepository;
    @Mock
    private EventServices eventServices;
    @Mock
    private DataUpdatePublisher updatePublisher;
    @Mock
    private AquariumRepository aquariumRepository;

    private final ParameterMapper parameterMapper = new ParameterMapper();
    private ParameterServices service;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/aquadesk");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        service = new ParameterServices(parametersRepository, rangeRepository, parameterMapper,
                eventServices, updatePublisher, aquariumRepository, messages);
    }

    @org.junit.jupiter.api.AfterEach
    void clearLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void summaryReflectsLatestValuesAndAquariumEvaluatorHealth() {
        Parameters latest = parameters("0.1", "0.2", "5", "7.1", "4", "8");
        when(parametersRepository.findFirstByAquariumIdOrderByIdDesc(9)).thenReturn(latest);
        when(rangeRepository.findByAquariumId(9)).thenReturn(List.of(range("pH", "5", "6", "7", "8", "9")));

        ParametersResponse issue = service.getParametersSummary(9);

        assertAll(
                () -> assertEquals("Issue", issue.estado())
        );
        verify(parametersRepository, times(2)).findFirstByAquariumIdOrderByIdDesc(9);
        verify(rangeRepository).findByAquariumId(9);
    }

    @Test
    void summariesHandleMissingLatestValuesWhenThereAreNoRanges() {
        when(parametersRepository.findFirstByAquariumIdOrderByIdDesc(3)).thenReturn(
                parameters(null, null, null, null, null, null));
        when(rangeRepository.findByAquariumId(3)).thenReturn(List.of());

        assertEquals("Healthy", service.getParametersSummary(3).estado());
    }

    @Test
    void returnsRangesAndParameterPagesFromTheRequestedRepositoryMethods() {
        ParametersRange range = range("pH", "5", "6", "7", "8", "9");
        Parameters item = parameters("0", "0", "10", "7", "4", "8");
        PageRequest pageable = PageRequest.of(1, 4);
        Page<Parameters> page = new PageImpl<>(List.of(item), pageable, 1);
        when(rangeRepository.findByAquariumId(6)).thenReturn(List.of(range));
        when(parametersRepository.findByAquariumId(6, pageable)).thenReturn(page);

        assertEquals(List.of(range), service.getParameterRanges(6));
        assertEquals(List.of(range), service.getAllParameterRanges(6));
        assertSame(page, service.getAllParameters(6, pageable));

        verify(rangeRepository, times(2)).findByAquariumId(6);
        verify(parametersRepository).findByAquariumId(6, pageable);
    }

    @Test
    void createsParameterRangesForAnAquarium() {
        RangeRequest req = rangeRequest("pH");
        Aquarium aquarium = new Aquarium();
        aquarium.setId(12);
        when(aquariumRepository.findById(12)).thenReturn(Optional.of(aquarium));

        service.createParameterRange(12, req);

        ArgumentCaptor<ParametersRange> saved = ArgumentCaptor.forClass(ParametersRange.class);
        verify(rangeRepository).save(saved.capture());
        assertEquals("pH", saved.getValue().getParameter());
        assertSame(aquarium, saved.getValue().getAquarium());
        assertEquals(new BigDecimal("7.00"), saved.getValue().getLevel3());
        verify(updatePublisher).publish("parameter-range");
    }

    @Test
    void wrapsRuntimeFailuresButPreservesAquadeskErrorsWhenCreatingRanges() {
        RangeRequest req = rangeRequest("pH");
        Aquarium aquarium = new Aquarium();
        when(aquariumRepository.findById(1)).thenReturn(Optional.of(aquarium));
        when(rangeRepository.save(any())).thenThrow(new IllegalStateException("database down"));

        ServiceException wrapped = assertThrows(ServiceException.class, () -> service.createParameterRange(1, req));
        assertEquals("Unable to create parameter range.", wrapped.getMessage());
        assertInstanceOf(IllegalStateException.class, wrapped.getCause());
        verify(updatePublisher, never()).publish("parameter-range");

        when(aquariumRepository.findById(2)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.createParameterRange(2, req));
        verify(rangeRepository, times(1)).save(any());
    }

    @Test
    void updatesAndDeletesRangesWithinAquariumScope() {
        ParametersRange scoped = range("old", "1", "2", "3", "4", "5");
        when(rangeRepository.findByIdAndAquariumId(2L, 7)).thenReturn(Optional.of(scoped));
        when(rangeRepository.findByIdAndAquariumId(4L, 7)).thenReturn(Optional.of(scoped));
        service.updateParameterRange(7, 2, rangeRequest("GH"));
        service.deleteParameterRange(7, 4);

        assertEquals("GH", scoped.getParameter());
        verify(rangeRepository).save(scoped);
        verify(rangeRepository).delete(scoped);
        verify(updatePublisher, times(2)).publish("parameter-range");
    }

    @Test
    void missingRangeUpdatesAndDeletesFailWithoutMutatingOrPublishing() {
        when(rangeRepository.findByIdAndAquariumId(11L, 8)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateParameterRange(8, 11, rangeRequest("pH")));
        assertThrows(ResourceNotFoundException.class, () -> service.deleteParameterRange(8, 11));
        verify(rangeRepository, never()).save(any());
        verify(rangeRepository, never()).delete(any());
        verify(updatePublisher, never()).publish(anyString());
    }

    @Test
    void addsSingleAquariumParameterEventsWithProvidedDateAndMapsValues() {
        ParametersRequest req = new ParametersRequest(LocalDate.of(2026, 5, 6), null, null, null,
                new BigDecimal("7.25"), null, null, null);
        Event event = new Event();
        event.setId(24);
        when(eventServices.createEvent(2, "Ph", LocalDate.of(2026, 5, 6), "ph")).thenReturn(event);

        service.addParameters(2, req);

        ArgumentCaptor<Parameters> saved = ArgumentCaptor.forClass(Parameters.class);
        verify(parametersRepository).save(saved.capture());
        assertEquals(24, saved.getValue().getIdEvent());
        assertEquals(new BigDecimal("7.25"), saved.getValue().getPh());
        verify(eventServices).createEvent(2, "Ph", LocalDate.of(2026, 5, 6), "ph");
        verify(updatePublisher).publish("parameters");
    }

    @Test
    void addsMultipleOrEmptyParameterEventsWithFallbackDateAndAquariumScope() {
        Event event = new Event();
        event.setId(25);
        when(eventServices.createEvent(eq(5), eq("Parameters"), any(LocalDate.class), eq("parameters")))
                .thenReturn(event);
        ParametersRequest requestWithSeveralValues = new ParametersRequest(null, BigDecimal.ONE, BigDecimal.ZERO,
                null, null, null, null, null);
        service.addParameters(5, requestWithSeveralValues);

        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(eventServices).createEvent(eq(5), eq("Parameters"), dateCaptor.capture(), eq("parameters"));
        assertEquals(LocalDate.now(), dateCaptor.getValue());
        verify(parametersRepository).save(any(Parameters.class));
        verify(updatePublisher).publish("parameters");
    }

    @Test
    void wrapsRuntimeFailuresAndPreservesDomainErrorsWhenAddingParameters() {
        Event event = new Event();
        event.setId(1);
        when(eventServices.createEvent(eq(3), anyString(), any(LocalDate.class), anyString())).thenReturn(event);
        when(parametersRepository.save(any())).thenThrow(new IllegalStateException("storage failed"));
        ServiceException wrapped = assertThrows(ServiceException.class,
                () -> service.addParameters(3, new ParametersRequest(null, null, null, null, null, null, null, null)));
        assertEquals("Unable to create parameters.", wrapped.getMessage());
        assertInstanceOf(IllegalStateException.class, wrapped.getCause());

        ResourceNotFoundException notFound = new ResourceNotFoundException("missing aquarium");
        when(eventServices.createEvent(eq(3), anyString(), any(LocalDate.class), anyString())).thenThrow(notFound);
        assertSame(notFound, assertThrows(ResourceNotFoundException.class,
                () -> service.addParameters(3, new ParametersRequest(null, null, null, null, null, null, null, null))));
        verify(parametersRepository, times(1)).save(any());
        verify(updatePublisher, never()).publish("parameters");
    }

    @Test
    void retrievesParametersOrRaisesNotFoundWithinAquariumScope() {
        Parameters parameters = parameters("1", "2", "3", "4", "5", "6");
        when(parametersRepository.findByIdEventAndAquariumId(32, 8)).thenReturn(Optional.of(parameters));
        when(parametersRepository.findByIdEventAndAquariumId(34, 8)).thenReturn(Optional.empty());

        assertSame(parameters, service.getParameters(8, 32));
        assertThrows(ResourceNotFoundException.class, () -> service.getParameters(8, 34));
    }

    @Test
    void evaluatesParameterRangeBranchesAndSkipsUnconfiguredOrMissingValues() {
        Parameters sample = parameters(null, null, null, "5", null, null);
        when(parametersRepository.findFirstByAquariumIdOrderByIdDesc(4)).thenReturn(sample);
        when(rangeRepository.findByAquariumId(4)).thenReturn(List.of(
                range("pH", "4", "4.5", "6", "7", "8"),
                range("KH", "1", "2", "4", "6", "8"),
                range("GH", "1", "2", "4", "6", "8"),
                range("unknown", "1", "2", "4", "6", "8")));

        List<NotificationParameterResponse> results = service.getNotificationParameters(4);

        assertEquals(1, results.size());
        assertEquals("pH", results.get(0).parameter());
        assertEquals("below recommended range: 5 / 6", results.get(0).value());
        verify(parametersRepository).findFirstByAquariumIdOrderByIdDesc(4);
        verify(rangeRepository).findByAquariumId(4);
    }

    @Test
    void returnsNoAnomaliesWhenNoParameterReadingExists() {
        when(parametersRepository.findFirstByAquariumIdOrderByIdDesc(4)).thenReturn(null);

        assertTrue(service.getNotificationParameters(4).isEmpty());

        verify(parametersRepository).findFirstByAquariumIdOrderByIdDesc(4);
    }

    @Test
    void evaluatorReportsRecommendedAndExtremeRangesAndNoIssueAtIdeal() {
        Parameters values = parameters("1", "3", "6", "9", "5", null);
        when(parametersRepository.findFirstByAquariumIdOrderByIdDesc(4)).thenReturn(values);
        when(rangeRepository.findByAquariumId(4)).thenReturn(List.of(
                range("NH3", "0", "2", "5", "8", "10"),
                range("NO2", "0", "2", "5", "8", "10"),
                range("NO3", "0", "2", "5", "8", "10"),
                range("pH", "0", "2", "5", "8", "10"),
                range("KH", "0", "2", "5", "8", "10"),
                range("GH", "0", "2", "5", "8", "10"),
                range("temp", "0", "2", "5", "8", "10")));

        List<NotificationParameterResponse> results = service.getNotificationParameters(4);

        assertEquals(List.of("Very low range: 1 (ideal: 5)", "below recommended range: 3 / 5",
                        "above recommended range: 6 / 5", "Very high range: 9 (ideal: 5)"),
                results.stream().map(NotificationParameterResponse::value).toList());
        assertEquals(List.of("NH3", "NO2", "NO3", "pH"),
                results.stream().map(NotificationParameterResponse::parameter).toList());
        verify(parametersRepository).findFirstByAquariumIdOrderByIdDesc(4);
        verify(rangeRepository).findByAquariumId(4);
    }

    private static Parameters parameters(String nh3, String no2, String no3, String ph, String kh, String gh) {
        Parameters parameters = new Parameters();
        parameters.setNh3(decimal(nh3));
        parameters.setNo2(decimal(no2));
        parameters.setNo3(decimal(no3));
        parameters.setPh(decimal(ph));
        parameters.setKh(decimal(kh));
        parameters.setGh(decimal(gh));
        return parameters;
    }

    private static ParametersRange range(String parameter, String level1, String level2, String level3,
                                          String level4, String level5) {
        ParametersRange range = new ParametersRange();
        range.setParameter(parameter);
        range.setLevel1(decimal(level1));
        range.setLevel2(decimal(level2));
        range.setLevel3(decimal(level3));
        range.setLevel4(decimal(level4));
        range.setLevel5(decimal(level5));
        return range;
    }

    private static RangeRequest rangeRequest(String parameter) {
        return new RangeRequest(parameter, new BigDecimal("5.00"), new BigDecimal("6.00"),
                new BigDecimal("7.00"), new BigDecimal("8.00"), new BigDecimal("9.00"), "units");
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
