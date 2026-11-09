package romsanbryan.aquadesk.controllers;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import romsanbryan.aquadesk.services.DataUpdatePublisher;

@RestController
public class UpdateStreamController {
    private final DataUpdatePublisher publisher;

    public UpdateStreamController(DataUpdatePublisher publisher) {
        this.publisher = publisher;
    }

    @GetMapping(value = "/api/updates", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return publisher.subscribe();
    }
}
