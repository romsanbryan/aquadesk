package romsanbryan.aquadesk.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.MessageSource;
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
import romsanbryan.data.repositories.AquariumRepository;
import romsanbryan.data.repositories.Co2Repository;
import romsanbryan.data.repositories.EventRepository;
import romsanbryan.data.repositories.FiltreRepository;
import romsanbryan.data.repositories.InfoRepository;
import romsanbryan.data.repositories.MaintenanceRepository;
import romsanbryan.data.repositories.ParametersRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HistoryExportService {

    private final AquariumRepository aquariumRepository;
    private final Co2Repository co2Repository;
    private final EventRepository eventRepository;
    private final FiltreRepository filtreRepository;
    private final InfoRepository infoRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final ParametersRepository parametersRepository;
    private final MessageSource messageSource;
    public HistoryExportService(AquariumRepository aquariumRepository,
                                Co2Repository co2Repository,
                                EventRepository eventRepository,
                                FiltreRepository filtreRepository,
                                InfoRepository infoRepository,
                                MaintenanceRepository maintenanceRepository,
                                ParametersRepository parametersRepository,
                                MessageSource messageSource) {
        this.aquariumRepository = aquariumRepository;
        this.co2Repository = co2Repository;
        this.eventRepository = eventRepository;
        this.filtreRepository = filtreRepository;
        this.infoRepository = infoRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.parametersRepository = parametersRepository;
        this.messageSource = messageSource;
    }

    @Transactional(readOnly = true)
    public byte[] export(Integer aquariumId, Format format, LocalDate startDate, LocalDate endDate,
                        boolean allHistory, List<String> requestedSections) {
        validateRequest(format, startDate, endDate, allHistory, requestedSections);
        Set<Section> sections = requestedSections.stream()
                .map(Section::fromCode)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Section.class)));

        Aquarium aquarium = aquariumRepository.findById(aquariumId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aquarium was not found with id " + aquariumId));
        List<HistoryEntry> entries = new ArrayList<>();

        addEventHistory(entries, aquariumId, sections, startDate, endDate, allHistory);

        entries.sort(Comparator.comparing(HistoryEntry::date, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(entry -> entry.section().label)
                .thenComparing(HistoryEntry::item));
        return format == Format.CSV ? toCsv(entries, aquarium.getName()) : toMarkdown(entries, aquarium.getName());
    }

    private void validateRequest(Format format, LocalDate startDate, LocalDate endDate,
                                 boolean allHistory, List<String> requestedSections) {
        if (format == null) {
            throw new ValidationException("Export format is required.");
        }
        if (!allHistory && (startDate == null || endDate == null || endDate.isBefore(startDate))) {
            throw new ValidationException("A valid start and end date are required.");
        }
        if (requestedSections == null || requestedSections.isEmpty()) {
            throw new ValidationException("Select at least one history category.");
        }
        requestedSections.forEach(Section::fromCode);
    }

    private void addEventHistory(List<HistoryEntry> entries, Integer aquariumId, Set<Section> sections,
                                 LocalDate start, LocalDate end, boolean allHistory) {
        List<Event> events = eventRepository.findByAquariumIdOrderByStartDateAsc(aquariumId);
        List<Event> periodEvents = events.stream()
                .filter(event -> matchesPeriod(event.getStartDate(), start, end, allHistory))
                .filter(event -> !"photo".equalsIgnoreCase(event.getType()))
                .toList();
        Set<Integer> periodEventIds = periodEvents.stream().map(Event::getId).collect(Collectors.toSet());

        if (sections.contains(Section.EVENTS)) {
            for (Event event : periodEvents) {
                add(entries, event.getId(), event.getStartDate(), Section.EVENTS, event.getTitle(),
                        "Título", event.getTitle(),
                        "Tipo", event.getType(),
                        "Fecha", event.getStartDate(),
                        "Interactivo", event.getClickeable());
            }
        }

        if (sections.contains(Section.PARAMETERS)) {
            for (Parameters parameters : parametersRepository.findAllByAquariumId(aquariumId)) {
                Event event = findEvent(events, parameters.getIdEvent());
                if (event == null || !periodEventIds.contains(event.getId())) continue;
                add(entries, event.getId(), event.getStartDate(), Section.PARAMETERS, event.getTitle(),
                        "NH3", parameters.getNh3(),
                        "NO2", parameters.getNo2(),
                        "NO3", parameters.getNo3(),
                        "pH", parameters.getPh(),
                        "KH", parameters.getKh(),
                        "GH", parameters.getGh(),
                        "Temperatura", parameters.getTemp());
            }
        }

        if (sections.contains(Section.INFORMATION)) {
            for (Info info : infoRepository.findAllByAquariumId(aquariumId)) {
                Event event = findEvent(events, info.getIdEvent());
                if (event == null || !periodEventIds.contains(event.getId())) continue;
                add(entries, event.getId(), event.getStartDate(), Section.INFORMATION, event.getTitle(),
                        "Tipo de información", info.getType(),
                        "Estado", info.getStatus(),
                        "Observación", info.getObservation());
            }
        }

        if (sections.contains(Section.MAINTENANCE)) {
            List<Maintenance> maintenances =
                    maintenanceRepository.findAllByEventAquariumIdOrderByEventStartDateAsc(aquariumId);
            List<Maintenance> periodMaintenances = maintenances.stream()
                    .filter(maintenance -> periodEventIds.contains(maintenance.getEvent().getId()))
                    .toList();
            List<Integer> maintenanceIds = periodMaintenances.stream().map(Maintenance::getId).toList();
            Map<Integer, List<Filtre>> filtersByMaintenance = maintenanceIds.isEmpty()
                    ? Map.of()
                    : filtreRepository.findByMaintenanceIdIn(maintenanceIds).stream()
                    .collect(Collectors.groupingBy(Filtre::getMaintenanceId));
            for (Maintenance maintenance : periodMaintenances) {
                Event event = maintenance.getEvent();
                List<Filtre> filters = filtersByMaintenance.getOrDefault(maintenance.getId(), List.of())
                        .stream().filter(this::isCleanedFilter).toList();
                String filterNames = filters.stream().map(Filtre::getFilterName)
                        .filter(Objects::nonNull).collect(Collectors.joining(" / "));
                String cleaningDetails = filters.stream()
                        .map(filter -> describeCleaning(filter, filters.size() > 1))
                        .collect(Collectors.joining(" / "));
                String filterDetails = filters.stream().map(this::describeMaintenanceFilter)
                        .collect(Collectors.joining("; "));
                add(entries, event.getId(), event.getStartDate(), Section.MAINTENANCE, event.getTitle(),
                        "Agua cambiada (L)", maintenance.getWater(),
                        "Prime", maintenance.getPrimeDose(),
                        "Poda", maintenance.getPruning(),
                        "Observaciones", maintenance.getObservations(),
                        "Cristales", maintenance.getGlass(),
                        "Filtros", filterNames,
                        "Limpieza", cleaningDetails,
                        "Detalle de filtros", filterDetails);
            }
        }

        if (sections.contains(Section.CO2)) {
            for (Co2 treatment : co2Repository.findAllByEventAquariumIdOrderByEventStartDateAsc(aquariumId)) {
                Event event = treatment.getEvent();
                if (event == null || !periodEventIds.contains(event.getId())) continue;
                add(entries, event.getId(), event.getStartDate(), Section.CO2, event.getTitle(),
                        "Producto", treatment.getProduct(),
                        "Cantidad", treatment.getValue());
            }
        }

    }

    private boolean isCleanedFilter(Filtre filter) {
        return Boolean.TRUE.equals(filter.getSponge()) || Boolean.TRUE.equals(filter.getPerlon())
                || Boolean.TRUE.equals(filter.getMech()) || Boolean.TRUE.equals(filter.getBio())
                || Boolean.TRUE.equals(filter.getBioMech());
    }

    private String describeMaintenanceFilter(Filtre filter) {
        return value(filter.getFilterName()) + " (perlón: " + value(filter.getPerlon())
                + ", esponja: " + value(filter.getSponge()) + ", mecánico: " + value(filter.getMech())
                + ", biológico: " + value(filter.getBio()) + ", bio-mecánico: " + value(filter.getBioMech()) + ")";
    }

    private String describeCleaning(Filtre filter, boolean includeFilterName) {
        List<String> materials = new ArrayList<>();
        if (Boolean.TRUE.equals(filter.getSponge())) materials.add(text("historyExport.output.sponge"));
        if (Boolean.TRUE.equals(filter.getPerlon())) materials.add(text("historyExport.output.perlon"));
        if (Boolean.TRUE.equals(filter.getMech())) materials.add(text("historyExport.output.mechanical"));
        if (Boolean.TRUE.equals(filter.getBio())) materials.add(text("historyExport.output.biological"));
        if (Boolean.TRUE.equals(filter.getBioMech())) materials.add(text("historyExport.output.bioMechanical"));
        if (materials.isEmpty()) return "";
        return (includeFilterName ? value(filter.getFilterName()) + ": " : "") + String.join("/", materials);
    }

    private Event findEvent(List<Event> events, Integer eventId) {
        return events.stream().filter(event -> Objects.equals(event.getId(), eventId)).findFirst().orElse(null);
    }

    private void add(List<HistoryEntry> entries, Integer eventId, LocalDate date, Section section, String item,
                     Object... fields) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int index = 0; index < fields.length; index += 2) {
            values.put((String) fields[index], value(fields[index + 1]));
        }
        entries.add(new HistoryEntry(eventId, date, section, item, values));
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private boolean matchesPeriod(LocalDate date, LocalDate start, LocalDate end, boolean allHistory) {
        return matchesPeriod(date, null, start, end, allHistory);
    }

    private boolean matchesPeriod(LocalDate firstDate, LocalDate secondDate, LocalDate start, LocalDate end,
                                  boolean allHistory) {
        if (allHistory) return true;
        if (firstDate == null && secondDate == null) return true;
        return (firstDate != null && !firstDate.isBefore(start) && !firstDate.isAfter(end))
                || (secondDate != null && !secondDate.isBefore(start) && !secondDate.isAfter(end));
    }

    private byte[] toCsv(List<HistoryEntry> entries, String aquariumName) {
        String[] headers = outputLabels();
        Map<EventKey, List<HistoryEntry>> events = new TreeMap<>(Comparator
                .comparing(EventKey::date, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(EventKey::id, Comparator.nullsLast(Comparator.naturalOrder())));
        for (HistoryEntry entry : entries) {
            events.computeIfAbsent(new EventKey(entry.date(), entry.eventId()), ignored -> new ArrayList<>())
                    .add(entry);
        }

        StringBuilder csv = new StringBuilder("\uFEFF");
        appendCsvRow(csv, headers);
        for (List<HistoryEntry> eventEntries : events.values()) {
            boolean hasDetails = eventEntries.stream().anyMatch(entry -> entry.section() != Section.EVENTS);
            for (HistoryEntry entry : eventEntries) {
                if (entry.section() == Section.EVENTS) {
                    if (!hasDetails) appendCsvRow(csv, prependAquarium(eventRow(entry), aquariumName));
                    continue;
                }
                for (String[] row : detailRows(entry)) {
                    appendCsvRow(csv, prependAquarium(row, aquariumName));
                }
            }
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String[] eventRow(HistoryEntry entry) {
        String type = field(entry, "Tipo").toUpperCase(Locale.ROOT);
        String normalizedType = switch (type) {
            case "INFO", "LIVE" -> "INFO";
            case "LIFE-FISH" -> "LIFE_FISH";
            case "LIFE-PLANT" -> "LIFE_PLANT";
            case "CO2" -> "TRATAMIENTO";
            case "MANTEIMENT" -> "MANTEIMENT";
            case "PARAMETERS", "NH3", "NO2", "NO3", "PH", "KH", "GH", "TEMP" -> "PARAMETRO";
            default -> type;
        };
        normalizedType = translateType(normalizedType);
        return csvRow(entry.date(), normalizedType, field(entry, "Título"), "", "", "", "", "", "");
    }

    private List<String[]> detailRows(HistoryEntry entry) {
        return switch (entry.section()) {
            case PARAMETERS -> parameterRows(entry);
            case INFORMATION -> List.<String[]>of(csvRow(entry.date(), "INFO", field(entry, "Observación"),
                    "", "", "", "", "", ""));
            case MAINTENANCE -> maintenanceRows(entry);
            case CO2 -> List.<String[]>of(csvRow(entry.date(), translateType("TRATAMIENTO"), "",
                    field(entry, "Producto"), field(entry, "Cantidad"), "", "", "", ""));
            case EVENTS -> List.<String[]>of(eventRow(entry));
        };
    }

    private List<String[]> parameterRows(HistoryEntry entry) {
        List<String[]> rows = new ArrayList<>();
        for (Map.Entry<String, String> parameter : entry.fields().entrySet()) {
            if (parameter.getValue().isBlank()) continue;
            rows.add(csvRow(entry.date(), translateType("PARAMETRO"), "", parameterName(parameter.getKey()),
                    parameter.getValue(), "", "", "", ""));
        }
        if (rows.isEmpty()) rows.add(csvRow(entry.date(), translateType("PARAMETRO"), "", "", "", "", "", "", ""));
        return rows;
    }

    private String parameterName(String fieldName) {
        return switch (fieldName) {
            case "NH3", "NO2", "NO3", "pH", "KH", "GH" -> fieldName.toLowerCase(Locale.ROOT);
            case "Temperatura" -> "temp";
            default -> fieldName.toLowerCase(Locale.ROOT);
        };
    }

    private List<String[]> maintenanceRows(HistoryEntry entry) {
        List<String[]> rows = new ArrayList<>();
        String observation = field(entry, "Observaciones");
        String water = field(entry, "Agua cambiada (L)");
        String pruning = field(entry, "Poda");
        String filter = field(entry, "Filtros");
        String cleaning = field(entry, "Limpieza");
        String glass = glassLabel(field(entry, "Cristales"));
        rows.add(csvRow(entry.date(), translateType("MANTEIMENT"), observation,
                water.isBlank() ? "" : text("historyExport.output.water"), water, pruning, filter, cleaning, glass));
        addMaintenanceMeasurement(rows, entry, "Prime", observation);
        return rows;
    }

    private void addMaintenanceMeasurement(List<String[]> rows, HistoryEntry entry, String name,
                                           String observation) {
        String amount = field(entry, name);
        if (!amount.isBlank()) {
            rows.add(csvRow(entry.date(), translateType("MANTEIMENT"), observation, name.toLowerCase(Locale.ROOT),
                    amount, "", "", "", ""));
        }
    }

    private String field(HistoryEntry entry, String name) {
        return entry.fields().getOrDefault(name, "");
    }

    private String glassLabel(String value) {
        if ("true".equalsIgnoreCase(value)) return text("historyExport.output.yes");
        if ("false".equalsIgnoreCase(value)) return text("historyExport.output.no");
        return "";
    }

    private String[] csvRow(LocalDate date, String type, String observations, String parameter, String amount,
                            String pruning, String filter, String cleaning, String glass) {
        return new String[]{date == null ? "" : date.toString(), type, observations, parameter, amount,
                pruning, filter, cleaning, glass};
    }

    private void appendCsvRow(StringBuilder csv, String[] values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) csv.append(',');
            csv.append(csvCell(values[index]));
        }
        csv.append("\r\n");
    }

    private String[] prependAquarium(String[] values, String aquariumName) {
        String[] row = new String[values.length + 1];
        row[0] = value(aquariumName);
        System.arraycopy(values, 0, row, 1, values.length);
        return row;
    }

    private String csvCell(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private byte[] toMarkdown(List<HistoryEntry> entries, String aquariumName) {
        Map<EventKey, List<HistoryEntry>> events = new TreeMap<>(Comparator
                .comparing(EventKey::date, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(EventKey::id, Comparator.nullsLast(Comparator.naturalOrder())));
        for (HistoryEntry entry : entries) {
            events.computeIfAbsent(new EventKey(entry.date(), entry.eventId()), ignored -> new ArrayList<>())
                    .add(entry);
        }

        Map<LocalDate, List<String[]>> rowsByDate = new TreeMap<>(Comparator.nullsLast(Comparator.naturalOrder()));
        for (List<HistoryEntry> eventEntries : events.values()) {
            boolean hasDetails = eventEntries.stream().anyMatch(entry -> entry.section() != Section.EVENTS);
            for (HistoryEntry entry : eventEntries) {
                if (entry.section() == Section.EVENTS) {
                    if (!hasDetails) {
                        addMarkdownRow(rowsByDate, prependAquarium(eventRow(entry), aquariumName));
                    }
                    continue;
                }
                for (String[] row : detailRows(entry)) {
                    addMarkdownRow(rowsByDate, prependAquarium(row, aquariumName));
                }
            }
        }

        StringBuilder markdown = new StringBuilder();
        if (rowsByDate.isEmpty()) {
            markdown.append('_').append(text("historyExport.output.noRecords")).append("_\n");
        }
        for (Map.Entry<LocalDate, List<String[]>> dateEntry : rowsByDate.entrySet()) {
            markdown.append("# ").append(value(dateEntry.getKey())).append("\n\n");
            for (String[] row : dateEntry.getValue()) {
                appendMarkdownRow(markdown, row);
            }
            markdown.append('\n');
        }
        return markdown.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void addMarkdownRow(Map<LocalDate, List<String[]>> rowsByDate, String[] row) {
        LocalDate date = row[1].isBlank() ? null : LocalDate.parse(row[1]);
        rowsByDate.computeIfAbsent(date, ignored -> new ArrayList<>()).add(row);
    }

    private void appendMarkdownRow(StringBuilder markdown, String[] row) {
        String[] labels = outputLabels();
        boolean firstField = true;
        markdown.append("- ");
        for (int index = 0; index < row.length; index++) {
            if (index == 1 || row[index].isBlank()) continue;
            if (!firstField) markdown.append(" · ");
            markdown.append("**").append(markdownText(labels[index])).append(":** ")
                    .append(markdownText(row[index]));
            firstField = false;
        }
        markdown.append('\n');
    }

    private String markdownText(String value) {
        return value.replace("\\", "\\\\").replace("|", "\\|")
                .replace("`", "\\`").replace("\r\n", "<br>").replace("\n", "<br>").replace("\r", "<br>");
    }

    private String translateType(String type) {
        return switch (type) {
            case "TRATAMIENTO", "TREATMENT" -> text("historyExport.output.treatment");
            case "PARAMETRO", "PARAMETER" -> text("historyExport.output.parameter");
            case "MANTEIMENT", "MAINTENANCE" -> text("historyExport.output.maintenance");
            case "LIFE_FISH" -> text("historyExport.output.lifeFish");
            case "LIFE_PLANT" -> text("historyExport.output.lifePlant");
            default -> type;
        };
    }

    private String[] outputLabels() {
        return new String[]{
                text("historyExport.output.aquarium"),
                text("historyExport.output.startDate"),
                text("historyExport.output.type"),
                text("historyExport.output.observations"),
                text("historyExport.output.treatmentParameter"),
                text("historyExport.output.value"),
                text("historyExport.output.pruning"),
                text("historyExport.output.filter"),
                text("historyExport.output.cleaning"),
                text("historyExport.output.glass")
        };
    }

    private String text(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }

    public enum Format {
        CSV, MD
    }

    private enum Section {
        EVENTS("events", "Eventos"),
        PARAMETERS("parameters", "Parámetros"),
        INFORMATION("information", "Información"),
        MAINTENANCE("maintenance", "Mantenimientos y filtros"),
        CO2("co2", "Tratamientos CO2");

        private final String code;
        private final String label;

        Section(String code, String label) {
            this.code = code;
            this.label = label;
        }

        private static Section fromCode(String code) {
            return Arrays.stream(values())
                    .filter(section -> section.code.equalsIgnoreCase(code))
                    .findFirst()
                    .orElseThrow(() -> new ValidationException("Unknown history category."));
        }

    }

    private record EventKey(LocalDate date, Integer id) {
    }

    private record HistoryEntry(Integer eventId, LocalDate date, Section section, String item,
                                Map<String, String> fields) {
    }
}
