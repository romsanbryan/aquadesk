package romsanbryan.aquadesk.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import romsanbryan.aquadesk.exception.ResourceNotFoundException;
import romsanbryan.aquadesk.exception.ValidationException;
import romsanbryan.data.entities.Aquarium;
import romsanbryan.data.entities.Co2;
import romsanbryan.data.entities.Event;
import romsanbryan.data.entities.Filtre;
import romsanbryan.data.entities.Info;
import romsanbryan.data.entities.Maintenance;
import romsanbryan.data.entities.Parameters;
import romsanbryan.data.repositories.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryExportServiceTest {

    @Mock private AquariumRepository aquariumRepository;
    @Mock private Co2Repository co2Repository;
    @Mock private EventRepository eventRepository;
    @Mock private FiltreRepository filtreRepository;
    @Mock private InfoRepository infoRepository;
    @Mock private MaintenanceRepository maintenanceRepository;
    @Mock private ParametersRepository parametersRepository;

    private HistoryExportService service;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/aquadesk");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        service = new HistoryExportService(aquariumRepository, co2Repository, eventRepository, filtreRepository,
                infoRepository, maintenanceRepository, parametersRepository, messages);
    }

    @AfterEach
    void clearLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void exportsFixedReadableCsvRowsGroupedByDateWithoutInternalIds() {
        Aquarium aquarium = new Aquarium();
        aquarium.setId(23);
        aquarium.setName("Acuario de prueba");
        Event event = new Event("Control semanal", LocalDate.of(2026, 10, 1),
                "parameters");
        Event photoEvent = new Event("Photo", LocalDate.of(2026, 10, 1),
                "photo");
        photoEvent.setId(983);
        event.setId(982);
        event.setClickeable(true);
        Parameters parameters = new Parameters();
        parameters.setId(1273L);
        parameters.setIdEvent(982);
        parameters.setNo2(new java.math.BigDecimal("5.0"));
        parameters.setPh(new java.math.BigDecimal("7.20"));
        Info info = new Info();
        info.setId(772);
        info.setIdEvent(982);
        info.setType("FISH");
        info.setObservation("Se observan activos");
        when(aquariumRepository.findById(23)).thenReturn(Optional.of(aquarium));
        when(eventRepository.findByAquariumIdOrderByStartDateAsc(23)).thenReturn(List.of(event, photoEvent));
        when(parametersRepository.findAllByAquariumId(23)).thenReturn(List.of(parameters));
        when(infoRepository.findAllByAquariumId(23)).thenReturn(List.of(info));

        String exported = new String(service.export(23, HistoryExportService.Format.CSV, null, null, true,
                List.of("events", "parameters", "information")), StandardCharsets.UTF_8);

        assertTrue(exported.contains("7.20"));
        assertTrue(exported.contains("Se observan activos"));
        assertFalse(exported.contains("982"));
        assertFalse(exported.contains("1273"));
        assertFalse(exported.contains("772"));
        assertEquals(4, exported.lines().count(),
                "Each parameter value gets its own row while related event records share its date");
        String[] csvRows = exported.replace("\uFEFF", "").strip().split("\\R");
        assertEquals("\"Acuario\",\"Fecha de inicio\",\"Tipo\",\"Observaciones\",\"Tratamiento/parámetro\","
                        + "\"Valor\",\"Poda\",\"Filtro\",\"Limpieza\",\"Cristales\"",
                csvRows[0]);
        assertEquals(10, csvRows[1].split(",", -1).length);
        assertEquals("\"Acuario de prueba\",\"2026-10-01\",\"INFO\",\"Se observan activos\","
                        + "\"\",\"\",\"\",\"\",\"\",\"\"",
                csvRows[1]);
        assertEquals("\"Acuario de prueba\",\"2026-10-01\",\"PARÁMETRO\",\"\",\"no2\","
                        + "\"5.0\",\"\",\"\",\"\",\"\"",
                csvRows[2]);
        assertEquals("\"Acuario de prueba\",\"2026-10-01\",\"PARÁMETRO\",\"\",\"ph\","
                        + "\"7.20\",\"\",\"\",\"\",\"\"",
                csvRows[3]);
        assertFalse(exported.contains("\"FOTO\""));
        verify(eventRepository).findByAquariumIdOrderByStartDateAsc(23);
        verify(parametersRepository).findAllByAquariumId(23);
        verify(infoRepository).findAllByAquariumId(23);
        verifyNoInteractions(co2Repository, filtreRepository, maintenanceRepository);
    }

    @Test
    void exportsTreatmentAndMaintenanceUsingTheRequestedColumns() {
        Aquarium aquarium = new Aquarium();
        aquarium.setName("Acuario jardín");
        Event treatmentEvent = new Event("CO2", LocalDate.of(2025, 6, 28),
                "co2");
        treatmentEvent.setId(12);
        Event maintenanceEvent = new Event("Maintenance", LocalDate.of(2025, 7, 28),
                "manteiment");
        maintenanceEvent.setId(13);
        Maintenance maintenance = new Maintenance();
        maintenance.setId(50);
        maintenance.setEvent(maintenanceEvent);
        maintenance.setWater(50);
        maintenance.setObservations("Mensaje de observaciones");
        maintenance.setPruning("ambulias");
        maintenance.setGlass(true);
        Co2 treatment = new Co2();
        treatment.setEvent(treatmentEvent);
        treatment.setProduct("co2");
        treatment.setValue(new java.math.BigDecimal("5"));
        Filtre filter = new Filtre();
        filter.setMaintenanceId(50);
        filter.setFilterName("A");
        filter.setSponge(true);
        filter.setMech(true);
        filter.setBio(true);
        Filtre untouchedFilter = new Filtre();
        untouchedFilter.setMaintenanceId(50);
        untouchedFilter.setFilterName("B");
        untouchedFilter.setSponge(false);
        untouchedFilter.setMech(false);
        untouchedFilter.setBio(false);

        when(aquariumRepository.findById(3)).thenReturn(Optional.of(aquarium));
        when(eventRepository.findByAquariumIdOrderByStartDateAsc(3))
                .thenReturn(List.of(treatmentEvent, maintenanceEvent));
        when(co2Repository.findAllByEventAquariumIdOrderByEventStartDateAsc(3)).thenReturn(List.of(treatment));
        when(maintenanceRepository.findAllByEventAquariumIdOrderByEventStartDateAsc(3))
                .thenReturn(List.of(maintenance));
        when(filtreRepository.findByMaintenanceIdIn(List.of(50)))
                .thenReturn(List.of(filter, untouchedFilter));

        String csv = new String(service.export(3, HistoryExportService.Format.CSV, null, null, true,
                List.of("co2", "maintenance")), StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"Acuario jardín\",\"2025-06-28\",\"TRATAMIENTO\",\"\",\"co2\","
                + "\"5\",\"\",\"\",\"\",\"\""));
        assertTrue(csv.contains("\"Acuario jardín\",\"2025-07-28\",\"MANTENIMIENTO\","
                + "\"Mensaje de observaciones\","
                + "\"Agua\",\"50\",\"ambulias\",\"A\",\"esponja/mecánico/biológico\",\"Sí\""));
        assertFalse(csv.contains("\"id\""));
        assertFalse(csv.contains("\"B\""), "Filters with no cleaning flag must be omitted");
        assertFalse(csv.contains("data:image"));
    }

    @Test
    void markdownExportGroupsCsvEquivalentRowsUnderOneDateHeader() {
        Aquarium aquarium = new Aquarium();
        aquarium.setName("Acuario markdown");
        Event event = new Event("Dentro del rango", LocalDate.of(2026, 10, 2),
                "info");
        event.setId(51);
        Event parameterEvent = new Event("Parámetros del día", LocalDate.of(2026, 10, 2),
                "parameters");
        parameterEvent.setId(52);
        Event oldEvent = new Event("Fuera del rango", LocalDate.of(2026, 9, 30),
                "info");
        Parameters parameters = new Parameters();
        parameters.setIdEvent(52);
        parameters.setNo2(new java.math.BigDecimal("5.0"));
        when(aquariumRepository.findById(5)).thenReturn(Optional.of(aquarium));
        when(eventRepository.findByAquariumIdOrderByStartDateAsc(5))
                .thenReturn(List.of(oldEvent, event, parameterEvent));
        when(parametersRepository.findAllByAquariumId(5)).thenReturn(List.of(parameters));

        String exported = new String(service.export(5, HistoryExportService.Format.MD,
                LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 3), false,
                List.of("events", "parameters")),
                StandardCharsets.UTF_8);

        assertTrue(exported.contains("Dentro del rango"));
        assertFalse(exported.contains("Fuera del rango"));
        assertTrue(exported.contains("# 2026-10-02"));
        assertEquals(1, exported.lines().filter("# 2026-10-02"::equals).count());
        assertEquals(2, exported.lines().filter(line -> line.startsWith("- ")).count());
        assertTrue(exported.contains("**Tipo:** INFO"));
        assertTrue(exported.contains("**Acuario:** Acuario markdown"));
        assertTrue(exported.contains("**Tratamiento/parámetro:** no2"));
        assertTrue(exported.contains("**Valor:** 5.0"));
    }

    @Test
    void exportsFixedLabelsInEnglishWhenEnglishLocaleIsActive() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        Aquarium aquarium = new Aquarium();
        aquarium.setName("English aquarium");
        Event event = new Event("Weekly check", LocalDate.of(2026, 10, 2),
                "info");
        event.setId(61);
        when(aquariumRepository.findById(6)).thenReturn(Optional.of(aquarium));
        when(eventRepository.findByAquariumIdOrderByStartDateAsc(6)).thenReturn(List.of(event));

        String csv = new String(service.export(6, HistoryExportService.Format.CSV,
                null, null, true, List.of("events")), StandardCharsets.UTF_8);
        String markdown = new String(service.export(6, HistoryExportService.Format.MD,
                null, null, true, List.of("events")), StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"Aquarium\",\"Start date\",\"Type\",\"Observations\","
                + "\"Treatment/parameter\",\"Value\",\"Pruning\",\"Filter\",\"Cleaning\",\"Glass\""));
        assertTrue(markdown.contains("**Aquarium:** English aquarium"));
        assertTrue(markdown.contains("**Type:** INFO"));
        assertFalse(markdown.contains("No hay registros"));
    }

    @Test
    void rejectsMissingOrReversedDateRangesAndUnknownSections() {
        assertThrows(ValidationException.class, () ->
                service.export(1, null, null, null, true, List.of("events")));
        assertThrows(ValidationException.class, () ->
                service.export(1, HistoryExportService.Format.CSV, null, null, false, List.of("events")));
        assertThrows(ValidationException.class, () ->
                service.export(1, HistoryExportService.Format.CSV,
                        LocalDate.of(2026, 10, 2), null, false, List.of("events")));
        assertThrows(ValidationException.class, () ->
                service.export(1, HistoryExportService.Format.CSV,
                        LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 2), false, List.of("events")));
        assertThrows(ValidationException.class, () ->
                service.export(1, HistoryExportService.Format.CSV, null, null, true, List.of("internal-id")));
        assertThrows(ValidationException.class, () ->
                service.export(1, HistoryExportService.Format.CSV, null, null, true, List.of("notifications")));
        assertThrows(ValidationException.class, () ->
                service.export(1, HistoryExportService.Format.CSV, null, null, true, List.of("tasks")));
        assertThrows(ValidationException.class, () ->
                service.export(1, HistoryExportService.Format.CSV, null, null, true, List.of("photos")));
        assertThrows(ValidationException.class, () ->
                service.export(1, HistoryExportService.Format.CSV, null, null, true, null));
        assertThrows(ValidationException.class, () ->
                service.export(1, HistoryExportService.Format.CSV, null, null, true, List.of()));
        when(aquariumRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                service.export(1, HistoryExportService.Format.CSV, null, null, true, List.of("events")));
        verify(aquariumRepository).findById(1);
        verifyNoInteractions(co2Repository, eventRepository, filtreRepository, infoRepository,
                maintenanceRepository, parametersRepository);
    }

    @Test
    void exportsEmptyMarkdownAndNormalizesEveryEventType() {
        Aquarium aquarium = new Aquarium();
        aquarium.setName("Acuario");
        when(aquariumRepository.findById(8)).thenReturn(Optional.of(aquarium));
        when(eventRepository.findByAquariumIdOrderByStartDateAsc(8)).thenReturn(List.of());

        String emptyMarkdown = new String(service.export(8, HistoryExportService.Format.MD,
                null, null, true, List.of("events")), StandardCharsets.UTF_8);

        assertEquals("_No hay registros para los filtros seleccionados._\n", emptyMarkdown);

        List<Event> events = List.of(
                new Event("info", LocalDate.of(2026, 1, 1), "info"),
                new Event("live", LocalDate.of(2026, 1, 2), "live"),
                new Event("co2", LocalDate.of(2026, 1, 3), "co2"),
                new Event("maintenance", LocalDate.of(2026, 1, 4), "manteiment"),
                new Event("parameters", LocalDate.of(2026, 1, 5), "parameters"),
                new Event("nh3", LocalDate.of(2026, 1, 6), "NH3"),
                new Event("no2", LocalDate.of(2026, 1, 7), "NO2"),
                new Event("no3", LocalDate.of(2026, 1, 8), "NO3"),
                new Event("ph", LocalDate.of(2026, 1, 9), "PH"),
                new Event("kh", LocalDate.of(2026, 1, 10), "KH"),
                new Event("gh", LocalDate.of(2026, 1, 11), "GH"),
                new Event("temp", LocalDate.of(2026, 1, 12), "TEMP"),
                new Event("custom", null, "custom"),
                new Event("unknown date and type", null, null),
                new Event("photo", LocalDate.of(2026, 1, 13), "photo"));
        for (int index = 0; index < events.size(); index++) {
            events.get(index).setId(index + 1);
        }
        when(eventRepository.findByAquariumIdOrderByStartDateAsc(9)).thenReturn(events);
        when(aquariumRepository.findById(9)).thenReturn(Optional.of(aquarium));

        String csv = new String(service.export(9, HistoryExportService.Format.CSV,
                null, null, true, List.of("events")), StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"Acuario\",\"2026-01-02\",\"INFO\""));
        assertTrue(csv.contains("\"Acuario\",\"2026-01-03\",\"TRATAMIENTO\""));
        assertTrue(csv.contains("\"Acuario\",\"2026-01-04\",\"MANTENIMIENTO\""));
        assertTrue(csv.contains("\"Acuario\",\"2026-01-05\",\"PARÁMETRO\""));
        assertTrue(csv.contains("\"Acuario\",\"\",\"CUSTOM\""));
        assertFalse(csv.contains("\"photo\""));

        String markdown = new String(service.export(9, HistoryExportService.Format.MD,
                null, null, true, List.of("events")), StandardCharsets.UTF_8);
        assertTrue(markdown.contains("# \n"));
    }

    @Test
    void exportsEmptyParametersAndCleansAllSupportedFilterMaterials() {
        Aquarium aquarium = new Aquarium();
        aquarium.setName("Variado");
        Event parameterEvent = new Event("Parámetros", LocalDate.of(2026, 4, 1),
                "parameters");
        parameterEvent.setId(101);
        Event firstMaintenanceEvent = new Event("Limpieza uno", LocalDate.of(2026, 4, 2),
                "manteiment");
        firstMaintenanceEvent.setId(102);
        Event secondMaintenanceEvent = new Event("Limpieza dos", LocalDate.of(2026, 4, 3),
                "manteiment");
        secondMaintenanceEvent.setId(103);
        Parameters emptyParameters = new Parameters();
        emptyParameters.setIdEvent(101);
        Parameters temperature = new Parameters();
        temperature.setIdEvent(101);
        temperature.setTemp(new java.math.BigDecimal("24"));
        Maintenance firstMaintenance = new Maintenance();
        firstMaintenance.setId(11);
        firstMaintenance.setEvent(firstMaintenanceEvent);
        firstMaintenance.setGlass(false);
        firstMaintenance.setPrimeDose(new java.math.BigDecimal("2"));
        Maintenance secondMaintenance = new Maintenance();
        secondMaintenance.setId(12);
        secondMaintenance.setEvent(secondMaintenanceEvent);
        Filtre sponge = filter(11, "Esponja");
        sponge.setSponge(true);
        Filtre perlon = filter(11, "Perlón");
        perlon.setPerlon(true);
        Filtre mechanical = filter(11, "Mecánico");
        mechanical.setMech(true);
        Filtre biological = filter(11, "Biológico");
        biological.setBio(true);
        Filtre bioMechanical = filter(11, null);
        bioMechanical.setBioMech(true);

        when(aquariumRepository.findById(10)).thenReturn(Optional.of(aquarium));
        when(eventRepository.findByAquariumIdOrderByStartDateAsc(10))
                .thenReturn(List.of(parameterEvent, firstMaintenanceEvent, secondMaintenanceEvent));
        when(parametersRepository.findAllByAquariumId(10)).thenReturn(List.of(emptyParameters, temperature));
        when(maintenanceRepository.findAllByEventAquariumIdOrderByEventStartDateAsc(10))
                .thenReturn(List.of(firstMaintenance, secondMaintenance));
        when(filtreRepository.findByMaintenanceIdIn(List.of(11, 12)))
                .thenReturn(List.of(sponge, perlon, mechanical, biological, bioMechanical));
        when(co2Repository.findAllByEventAquariumIdOrderByEventStartDateAsc(10))
                .thenReturn(List.of(new Co2()));

        String csv = new String(service.export(10, HistoryExportService.Format.CSV,
                null, null, true, List.of("parameters", "maintenance", "co2")),
                StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"Variado\",\"2026-04-01\",\"PARÁMETRO\",\"\",\"\",\"\",\"\",\"\",\"\",\"\""),
                "An empty set of parameter values still produces a row");
        assertTrue(csv.contains("\"2\""));
        assertTrue(csv.contains("\"temp\",\"24\""));
        assertTrue(csv.contains("esponja"));
        assertTrue(csv.contains("perlón"));
        assertTrue(csv.contains("mecánico"));
        assertTrue(csv.contains("biológico"));
        assertTrue(csv.contains("bio-mecánico"));
        assertTrue(csv.contains("No\""));
        assertFalse(csv.contains("data:image"));
    }

    @Test
    void filtersUnmatchedRelatedRecordsWithoutQueryingFiltersForEmptyMaintenanceSet() {
        Aquarium aquarium = new Aquarium();
        Event outsideEvent = new Event("Fuera", LocalDate.of(2026, 5, 1),
                "info");
        outsideEvent.setId(201);
        Event nullDateEvent = new Event("Sin fecha", null, "info");
        nullDateEvent.setId(202);
        Parameters unmatchedParameters = new Parameters();
        unmatchedParameters.setIdEvent(999);
        Parameters outOfRangeParameters = new Parameters();
        outOfRangeParameters.setIdEvent(201);
        Info unmatchedInfo = new Info();
        unmatchedInfo.setIdEvent(999);
        Info outOfRangeInfo = new Info();
        outOfRangeInfo.setIdEvent(201);
        Maintenance unmatchedMaintenance = new Maintenance();
        unmatchedMaintenance.setId(20);
        unmatchedMaintenance.setEvent(new Event("No incluido", LocalDate.of(2026, 5, 1),
                "manteiment"));
        Co2 nullEventTreatment = new Co2();
        Co2 unmatchedTreatment = new Co2();
        unmatchedTreatment.setEvent(outsideEvent);
        when(aquariumRepository.findById(11)).thenReturn(Optional.of(aquarium));
        when(eventRepository.findByAquariumIdOrderByStartDateAsc(11)).thenReturn(List.of(outsideEvent, nullDateEvent));
        when(parametersRepository.findAllByAquariumId(11))
                .thenReturn(List.of(unmatchedParameters, outOfRangeParameters));
        when(infoRepository.findAllByAquariumId(11)).thenReturn(List.of(unmatchedInfo, outOfRangeInfo));
        when(maintenanceRepository.findAllByEventAquariumIdOrderByEventStartDateAsc(11))
                .thenReturn(List.of(unmatchedMaintenance));
        when(co2Repository.findAllByEventAquariumIdOrderByEventStartDateAsc(11))
                .thenReturn(List.of(nullEventTreatment, unmatchedTreatment));

        String csv = new String(service.export(11, HistoryExportService.Format.CSV,
                LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 3), false,
                List.of("events", "parameters", "information", "maintenance", "co2")),
                StandardCharsets.UTF_8);

        assertTrue(csv.contains("Sin fecha"));
        assertFalse(csv.contains("Fuera"));
        verifyNoInteractions(filtreRepository);
    }

    @Test
    void coversDefensiveHelperBranchesThatCannotBeReachedThroughAnExport() throws Exception {
        LocalDate start = LocalDate.of(2026, 6, 10);
        LocalDate end = LocalDate.of(2026, 6, 20);
        assertTrue(matchesPeriod(null, null, start, end, false));
        assertTrue(matchesPeriod(null, start, start, end, false));
        assertFalse(matchesPeriod(null, start.minusDays(1), start, end, false));
        assertFalse(matchesPeriod(null, end.plusDays(1), start, end, false));
        assertTrue(matchesPeriod(start, null, start, end, false));
        assertFalse(matchesPeriod(start.minusDays(1), null, start, end, false));
        assertFalse(matchesPeriod(end.plusDays(1), null, start, end, false));
        assertTrue(matchesPeriod(start.minusDays(1), end, start, end, false));
        assertTrue(matchesPeriod(end.plusDays(1), start, start, end, false));
        assertTrue(matchesPeriod(null, null, null, null, true));

        Filtre uncleanedFilter = filter(30, "Sin limpiar");
        assertEquals("", invokePrivate("describeCleaning",
                new Class<?>[]{Filtre.class, boolean.class}, uncleanedFilter, false));
        assertEquals("temp", invokePrivate("parameterName",
                new Class<?>[]{String.class}, "Temperatura"));
        assertEquals("custom", invokePrivate("parameterName",
                new Class<?>[]{String.class}, "Custom"));

        Class<?> sectionType = Class.forName(HistoryExportService.class.getName() + "$Section");
        Object eventSection = java.util.Arrays.stream(sectionType.getEnumConstants())
                .filter(section -> section.toString().equals("EVENTS"))
                .findFirst().orElseThrow();
        Class<?> entryType = Class.forName(HistoryExportService.class.getName() + "$HistoryEntry");
        Constructor<?> entryConstructor = entryType.getDeclaredConstructor(
                Integer.class, LocalDate.class, sectionType, String.class, Map.class);
        entryConstructor.setAccessible(true);
        Object eventEntry = entryConstructor.newInstance(31, start, eventSection, "Evento",
                Map.of("Tipo", "LIVE", "Título", "Evento"));
        List<?> eventRows = (List<?>) invokePrivate("detailRows",
                new Class<?>[]{entryType}, eventEntry);
        assertEquals("INFO", ((String[]) eventRows.get(0))[1]);

        for (String[] type : new String[][]{
                {"LIFE-FISH", "LIFE_FISH"},
                {"LIFE-PLANT", "LIFE_PLANT"},
                {"TREATMENT", "TRATAMIENTO"},
                {"PARAMETER", "PARAMETRO"}
        }) {
            Object aliasEntry = entryConstructor.newInstance(32, start, eventSection, "Evento",
                    Map.of("Tipo", type[0], "Título", "Evento"));
            String[] row = (String[]) invokePrivate("eventRow", new Class<?>[]{entryType}, aliasEntry);
            assertEquals(invokePrivate("translateType", new Class<?>[]{String.class}, type[1]), row[1]);
        }
    }

    private boolean matchesPeriod(LocalDate firstDate, LocalDate secondDate,
                                  LocalDate start, LocalDate end, boolean allHistory) throws Exception {
        return (boolean) invokePrivate("matchesPeriod",
                new Class<?>[]{LocalDate.class, LocalDate.class, LocalDate.class, LocalDate.class, boolean.class},
                firstDate, secondDate, start, end, allHistory);
    }

    private Object invokePrivate(String methodName, Class<?>[] parameterTypes, Object... arguments)
            throws Exception {
        Method method = HistoryExportService.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(service, arguments);
    }

    private Filtre filter(Integer maintenanceId, String name) {
        Filtre filter = new Filtre();
        filter.setMaintenanceId(maintenanceId);
        filter.setFilterName(name);
        filter.setSponge(false);
        filter.setPerlon(false);
        filter.setMech(false);
        filter.setBio(false);
        filter.setBioMech(false);
        return filter;
    }
}
