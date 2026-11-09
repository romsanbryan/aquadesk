package romsanbryan.aquadesk.mapper;

import org.springframework.stereotype.Component;
import romsanbryan.aquadesk.dto.EventDetails;
import romsanbryan.aquadesk.dto.response.EventResponse;
import romsanbryan.data.entities.Event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EventMapper {

    public EventResponse toDto(Event event) {
        return new EventResponse(
                event.getId().longValue(),
                resolveTitle(event.getType(), event.getTitle()),
                toStartOfDay(event.getStartDate()),
                toEndOfDay(event.getStartDate()),
                resolveColor(event.getType()),
                null,                          // The entity has no description.
                true,
                event.getType()// Date-only events are always all-day.
                , 1, null
        );
    }

    public List<EventResponse> toGroupedDtoList(List<Event> events) {
        Map<String, List<Event>> agrupados = events.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getType() + "_" + e.getStartDate().toString()
                ));

        return agrupados.values().stream()
                .map(this::toGroupedDto)
                .toList();
    }

    private EventResponse toGroupedDto(List<Event> grupo) {
        Event primero = grupo.get(0);

        List<EventDetails> items = grupo.stream()
                .map(e -> new EventDetails(
                        e.getId().longValue(),
                        e.getTitle(),
                        toStartOfDay(e.getStartDate()),
                        toEndOfDay(e.getStartDate()),
                        e.getCo2() != null ? e.getCo2().getValue() : null,
                        e.getCo2() != null ? e.getCo2().getProduct() : null
                ))
                .toList();

        return new EventResponse(
                primero.getId().longValue(),
                resolveIcon(primero.getType()),
                toStartOfDay(primero.getStartDate()),
                toEndOfDay(primero.getStartDate()),
                resolveColor(primero.getType()),
                null,
                true,
                primero.getType(),
                grupo.size(),
                items
        );
    }

    private String resolveTitle(String type, String title) {
        String icon = resolveIcon(type);
        return icon;
    }

    private String resolveIcon(String type) {
        if (type == null) return "<i class='bi bi-calendar-event'></i>"; // default
        return switch (type.toLowerCase()) {
            case "co2", "fosfo", "nitro", "profito" -> "<i class='bi bi-wind'></i>";
            case "info" -> "<i class='bi bi-info-circle'></i>";
            case "manteiment" -> "<i class='bi bi-tools'></i>";
            case "parameters" -> "<i class='bi bi-sliders'></i>";
            case "live" -> "<i class='bi bi-camera-video'></i>";
            case "life-fish" -> "<svg aria-hidden='true' viewBox='0 0 24 24' width='1em' height='1em' "
                    + "fill='currentColor'><path d='M3 12c2.2-3.1 5.2-4.7 9-4.7 2.4 0 4.5.8 6.4 2.3L22 7v10l-3.6-2.6"
                    + "c-1.9 1.5-4 2.3-6.4 2.3-3.8 0-6.8-1.6-9-4.7Zm7.1-1.3a1 1 0 1 0 0 2 1 1 0 0 0 0-2Z'/></svg>";
            case "life-plant" -> "<i class='bi bi-flower1' style='color:#fff'></i>";
            case "photo" -> "<i class='bi bi-camera'></i>";
            default -> "<i class='bi bi-calendar-event'></i>"; // default
        };
    }

    public List<EventResponse> toDtoList(List<Event> events) {
        return events.stream()
                .map(this::toDto)
                .toList();
    }


    private LocalDateTime toStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    private LocalDateTime toEndOfDay(LocalDate date) {
        return date != null ? date.plusDays(1).atStartOfDay() : null;
    }

    private String resolveColor(String type) {
        if (type == null) return "#3788d8"; // Default blue.
        return switch (type.toLowerCase()) {
            case "festivo" -> "#e74c3c"; // Red.
            case "promocion" -> "#2ecc71"; // Green.
            case "mantenimiento" -> "#95a5a6"; // Gray.
            case "especial" -> "#9b59b6"; // Purple.
            case "life-fish", "life-plant" -> "#3788d8";
            default -> "#3788d8"; // Blue.
        };
    }


    public Event mapEvent(String title, LocalDate date, String type) {
        Event event = new Event();
        event.setTitle(title);
        event.setStartDate(date);
        event.setType(type);
        event.setClickeable(true);
        return event;
    }
}