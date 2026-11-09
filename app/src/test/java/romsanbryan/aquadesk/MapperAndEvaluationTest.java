package romsanbryan.aquadesk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockMultipartFile;
import romsanbryan.aquadesk.dto.request.MaintenanceRequest;
import romsanbryan.aquadesk.dto.request.ParametersRequest;
import romsanbryan.aquadesk.dto.request.RangeRequest;
import romsanbryan.aquadesk.dto.response.EventResponse;
import romsanbryan.aquadesk.evaluation.DefaultParameterEvaluator;
import romsanbryan.aquadesk.evaluation.ParameterValueExtractor;
import romsanbryan.aquadesk.exception.ValidationException;
import romsanbryan.aquadesk.mapper.EventMapper;
import romsanbryan.aquadesk.mapper.MaintenanceMapper;
import romsanbryan.aquadesk.mapper.ParameterMapper;
import romsanbryan.aquadesk.mapper.PhotoMapper;
import romsanbryan.aquadesk.mapper.TreatmentMapper;
import romsanbryan.data.entities.Co2;
import romsanbryan.data.entities.Event;
import romsanbryan.data.entities.Maintenance;
import romsanbryan.data.entities.Parameters;
import romsanbryan.data.entities.ParametersRange;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class MapperAndEvaluationTest {

    @AfterEach
    void clearLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void mapsAndGroupsEventsIncludingFallbacks() {
        EventMapper mapper = new EventMapper();
        Event co2 = event(1, "CO2", "co2", LocalDate.of(2026, 10, 2));
        Co2 treatment = new Co2();
        treatment.setProduct("Carbon");
        treatment.setValue(new BigDecimal("2.5"));
        co2.setCo2(treatment);
        Event second = event(2, "CO2", "co2", LocalDate.of(2026, 10, 2));
        Event nullType = event(3, "Unknown", null, LocalDate.of(2026, 10, 3));

        var direct = mapper.toDto(co2);
        assertEquals(1L, direct.id());
        assertEquals("<i class='bi bi-wind'></i>", direct.title());
        assertEquals("#3788d8", direct.color());
        assertEquals(LocalDate.of(2026, 10, 3).atStartOfDay(), direct.end());
        assertNull(direct.description());
        assertTrue(direct.allDay());

        var grouped = mapper.toGroupedDtoList(List.of(co2, second, nullType));
        assertEquals(2, grouped.size());
        var co2Group = grouped.stream().filter(item -> "co2".equals(item.type())).findFirst().orElseThrow();
        assertEquals(2, co2Group.count());
        assertEquals(new BigDecimal("2.5"), co2Group.items().get(0).co2Value());
        assertEquals("Carbon", co2Group.items().get(0).co2Product());

        var fallback = mapper.toDto(nullType);
        assertEquals("<i class='bi bi-calendar-event'></i>", fallback.title());
        assertEquals(LocalDate.of(2026, 10, 4).atStartOfDay(), fallback.end());
        assertEquals("#3788d8", fallback.color());
        assertEquals(1, mapper.toDtoList(List.of(co2)).size());

        Event mapped = mapper.mapEvent("Mapped", LocalDate.of(2026, 10, 4), "info");
        assertEquals("Mapped", mapped.getTitle());
        assertTrue(mapped.getClickeable());
    }

    @Test
    void mapsEveryEventIconAndColorFallback() {
        EventMapper mapper = new EventMapper();
        List<Event> events = List.of(
                event(21, "CO2", "fosfo", LocalDate.now()),
                event(22, "CO2", "nitro", LocalDate.now()),
                event(23, "CO2", "profito", LocalDate.now()),
                event(24, "Info", "info", LocalDate.now()),
                event(25, "Maintenance", "manteiment", LocalDate.now()),
                event(26, "Parameters", "parameters", LocalDate.now()),
                event(27, "Live", "live", LocalDate.now()),
                event(28, "Photo", "photo", LocalDate.now()),
                event(29, "Unknown", "unknown", LocalDate.now()),
                event(30, "Holiday", "festivo", LocalDate.now()),
                event(31, "Promotion", "promocion", LocalDate.now()),
                event(32, "Maintenance", "mantenimiento", LocalDate.now()),
                event(33, "Special", "especial", LocalDate.now()),
                event(34, "Null type", null, LocalDate.now()),
                event(35, "Null date", "info", null),
                event(36, "Fish", "life-fish", LocalDate.now()),
                event(37, "Plant", "life-plant", LocalDate.now())
        );

        var responses = mapper.toDtoList(events);
        Map<String, String> icons = Map.ofEntries(
                Map.entry("fosfo", "<i class='bi bi-wind'></i>"),
                Map.entry("nitro", "<i class='bi bi-wind'></i>"),
                Map.entry("profito", "<i class='bi bi-wind'></i>"),
                Map.entry("info", "<i class='bi bi-info-circle'></i>"),
                Map.entry("manteiment", "<i class='bi bi-tools'></i>"),
                Map.entry("parameters", "<i class='bi bi-sliders'></i>"),
                Map.entry("live", "<i class='bi bi-camera-video'></i>"),
                Map.entry("photo", "<i class='bi bi-camera'></i>"),
                Map.entry("life-fish", "<svg aria-hidden='true' viewBox='0 0 24 24' width='1em' height='1em' "
                        + "fill='currentColor'><path d='M3 12c2.2-3.1 5.2-4.7 9-4.7 2.4 0 4.5.8 6.4 2.3L22 7v10l-3.6-2.6"
                        + "c-1.9 1.5-4 2.3-6.4 2.3-3.8 0-6.8-1.6-9-4.7Zm7.1-1.3a1 1 0 1 0 0 2 1 1 0 0 0 0-2Z'/></svg>"),
                Map.entry("life-plant", "<i class='bi bi-flower1' style='color:#fff'></i>"),
                Map.entry("unknown", "<i class='bi bi-calendar-event'></i>")
        );
        for (int index = 0; index < events.size(); index++) {
            String type = events.get(index).getType();
            if (type != null && icons.containsKey(type)) {
                assertEquals(icons.get(type), responses.get(index).title());
            }
        }

        assertEquals("#e74c3c", responses.get(9).color());
        assertEquals("#2ecc71", responses.get(10).color());
        assertEquals("#95a5a6", responses.get(11).color());
        assertEquals("#9b59b6", responses.get(12).color());
        assertEquals("#3788d8", responses.get(8).color());
        assertEquals("#3788d8", responses.get(13).color());
        assertNull(responses.get(14).start());
        assertEquals("#3788d8", responses.get(15).color());
        assertEquals("#3788d8", responses.get(16).color());
    }

    @Test
    void eventResponseReplacesNullAndBlankTitlesButPreservesNonblankTitles() {
        assertEquals("(Untitled)", new EventResponse(1L, null, null, null,
                null, null, true, "info", 1, null).title());
        assertEquals("(Untitled)", new EventResponse(2L, "   ", null, null,
                null, null, true, "info", 1, null).title());
        assertEquals("Reminder", new EventResponse(3L, "Reminder", null, null,
                null, null, true, "reminder", 1, null).title());
    }

    @Test
    void mapsParameterSummaryRangesAndMeasurements() {
        ParameterMapper mapper = new ParameterMapper();
        assertEquals("Healthy", mapper.toParametersSummaryDto(null, "Healthy").estado());
        assertEquals("Issue", mapper.toParametersSummaryDto(null, "Issue").estado());

        Parameters parameters = new Parameters();
        parameters.setNh3(new BigDecimal("0.1"));
        parameters.setPh(new BigDecimal("7.2"));
        var summary = mapper.toParametersSummaryDto(parameters, "Healthy");
        assertEquals(new BigDecimal("0.1"), summary.nh3());
        assertEquals(new BigDecimal("7.2"), summary.ph());
        assertNull(summary.no2());

        RangeRequest request = new RangeRequest("pH", BigDecimal.ONE, BigDecimal.TEN,
                new BigDecimal("7"), null, null, "pH");
        ParametersRange range = new ParametersRange();
        assertSame(range, mapper.mapParametersRange(request, range));
        assertEquals("pH", range.getParameter());
        assertEquals(BigDecimal.TEN, range.getLevel2());

        ParametersRequest measurement = new ParametersRequest(LocalDate.now(), null, null,
                new BigDecimal("15"), new BigDecimal("7.2"), null, null, null);
        Parameters mapped = mapper.mapParameters(measurement, 42);
        assertEquals(42, mapped.getIdEvent());
        assertEquals(new BigDecimal("15"), mapped.getNo3());
        assertEquals(new BigDecimal("7.2"), mapped.getPh());
        assertNull(mapped.getTemp());
    }

    @Test
    void mapsMaintenanceAndFilterDefaultsAndLatestData() {
        MaintenanceMapper mapper = new MaintenanceMapper();
        Event event = event(4, "Maintenance", "manteiment", LocalDate.of(2026, 10, 1));
        MaintenanceRequest request = new MaintenanceRequest(null, 20, BigDecimal.ONE,
                null, "Trimmed plants", null,
                true, false, true, true, false, false, true, false, true, true);
        Maintenance maintenance = mapper.mapMaintenance(request, event);
        assertSame(event, maintenance.getEvent());
        assertEquals(false, maintenance.getGlass());
        assertEquals("", maintenance.getObservations());
        assertEquals("Trimmed plants", maintenance.getPruning());
        var latest = mapper.mapLatestMaintenance(maintenance,
                List.of(mapper.mapFilter("Canister", true, false, true, true, false, 3)));
        assertEquals(LocalDate.of(2026, 10, 1), latest.fecha());
        assertEquals(1, latest.filters().size());
        assertEquals("Canister", latest.filters().get(0).filterName());

        var filter = mapper.mapFilter("Sponge", null, true, false, true, false, 9);
        assertEquals(9, filter.getMaintenanceId());
        assertNull(filter.getSponge());
        assertEquals(true, filter.getPerlon());
        assertEquals("Sponge", mapper.mapFilterToDto(filter).filterName());
    }

    @Test
    void mapsPhotoBytesAndDefaultsMimeType() throws Exception {
        PhotoMapper mapper = new PhotoMapper();
        Event event = event(11, "Photo", "photo", LocalDate.now());
        byte[] imageBytes = {1, 2, 3};
        var photo = mapper.createPhoto(event,
                new MockMultipartFile("foto", "fish.png", "image/png", imageBytes));
        assertEquals(11, photo.getEventId());
        assertArrayEquals(imageBytes, photo.getPhotoData());
        assertEquals("image/png", photo.getMimeType());
        assertNotNull(photo.getCreatedAt());

        assertEquals("image/jpeg", mapper.createPhoto(event,
                new MockMultipartFile("foto", "fish", null, imageBytes)).getMimeType());
    }

    @Test
    void parsesTreatmentValuesAndRejectsInvalidNumbers() {
        TreatmentMapper mapper = new TreatmentMapper();
        Event event = event(15, "CO2", "co2", LocalDate.now());
        assertEquals(new BigDecimal("2.5"), mapper.getCo2("Liquid CO2", "2,5 ml", event).getValue());
        assertEquals(new BigDecimal("12"), mapper.getCo2("Carbon", "$12", event).getValue());
        assertNull(mapper.getCo2("Carbon", "  ", event).getValue());
        assertNull(mapper.getCo2("Carbon", null, event).getValue());
        assertThrows(ValidationException.class, () -> mapper.getCo2("Carbon", "no number", event));
    }

    @Test
    void extractsKnownParameterValuesAndEvaluatesAllRanges() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        Parameters parameters = new Parameters();
        parameters.setNh3(BigDecimal.ZERO);
        parameters.setNo2(BigDecimal.ONE);
        parameters.setPh(new BigDecimal("7"));
        ParameterValueExtractor extractor = new ParameterValueExtractor(parameters);
        assertEquals(BigDecimal.ZERO, extractor.getValue("NH3"));
        assertNull(extractor.getValue("UNKNOWN"));
        assertTrue(extractor.hasParameter("pH"));
        assertFalse(extractor.hasParameter("unknown"));
        assertEquals(6, extractor.getAvailableParameters().size());

        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/aquadesk");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        DefaultParameterEvaluator evaluator = new DefaultParameterEvaluator(messages);
        BigDecimal[] levels = {BigDecimal.ZERO, new BigDecimal("4"), new BigDecimal("5"),
                new BigDecimal("6"), new BigDecimal("10")};
        assertNull(evaluator.evaluate(null, levels[0], levels[1], levels[2], levels[3], levels[4], "pH"));
        assertEquals("below recommended range: 4 / 5",
                evaluator.evaluate(new BigDecimal("4"), levels[0], levels[1], levels[2], levels[3], levels[4], "pH"));
        assertEquals("Very low range: 3 (ideal: 5)",
                evaluator.evaluate(new BigDecimal("3"), levels[0], levels[1], levels[2], levels[3], levels[4], "pH"));
        assertNull(evaluator.evaluate(new BigDecimal("5"), levels[0], levels[1], levels[2], levels[3], levels[4], "pH"));
        assertEquals("Very high range: 10 (ideal: 5)",
                evaluator.evaluate(new BigDecimal("10"), levels[0], levels[1], levels[2], levels[3], levels[4], "pH"));
        assertEquals("Very high range: 11 (ideal: 5)",
                evaluator.evaluate(new BigDecimal("11"), levels[0], levels[1], levels[2], levels[3], levels[4], "pH"));
    }

    private Event event(int id, String title, String type, LocalDate date) {
        Event event = new Event();
        event.setId(id);
        event.setTitle(title);
        event.setType(type);
        event.setStartDate(date);
        return event;
    }
}
