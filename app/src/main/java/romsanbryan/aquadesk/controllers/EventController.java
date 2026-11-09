package romsanbryan.aquadesk.controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import romsanbryan.aquadesk.dto.request.InfoRequest;
import romsanbryan.aquadesk.dto.response.EventResponse;
import romsanbryan.aquadesk.dto.response.NextEventResponse;
import romsanbryan.aquadesk.services.EventServices;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/aquariums/{aquariumId}/events")
public class EventController {
    private final EventServices eventServices;
    public EventController(EventServices eventServices) { this.eventServices = eventServices; }

    @GetMapping("/upcoming")
    public List<NextEventResponse> getUpcomingEvents(@PathVariable Integer aquariumId) { return eventServices.getUpcomingEvents(aquariumId); }
    @GetMapping
    public List<EventResponse> getEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @PathVariable Integer aquariumId) {
        return eventServices.getEvents(aquariumId, start, end);
    }
    @GetMapping("/co2/by-day/{date}")
    public ResponseEntity<?> getCo2ByDay(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                         @PathVariable Integer aquariumId) {
        return ResponseEntity.ok(eventServices.getCo2ByDay(aquariumId, date));
    }
    @DeleteMapping("/{eventId}")
    public ResponseEntity<?> deleteEvent(@PathVariable Integer eventId, @PathVariable Integer aquariumId) {
        eventServices.deleteEventById(aquariumId, eventId);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/information")
    public ResponseEntity<?> addInfo(@RequestBody InfoRequest req, @PathVariable Integer aquariumId) {
        eventServices.addInfo(aquariumId, req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    @GetMapping("/information/by-day/{date}")
    public ResponseEntity<?> getInfoByDay(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                          @PathVariable Integer aquariumId) {
        return ResponseEntity.ok(eventServices.getInfoByDay(aquariumId, date));
    }
}
