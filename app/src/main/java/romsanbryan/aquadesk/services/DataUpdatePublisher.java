package romsanbryan.aquadesk.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

@Component
public class DataUpdatePublisher {
    private static final Logger log = LoggerFactory.getLogger(DataUpdatePublisher.class);
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final Supplier<SseEmitter> emitterFactory;

    public DataUpdatePublisher() {
        this(() -> new SseEmitter(0L));
    }

    DataUpdatePublisher(Supplier<SseEmitter> emitterFactory) {
        this.emitterFactory = emitterFactory;
    }

    public SseEmitter subscribe() {
        log.debug("Registering SSE subscriber");
        SseEmitter emitter = emitterFactory.get();
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            emitters.remove(emitter);
            log.warn("Unable to send SSE connection event", e);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    public void publish(String type) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishImmediately(type);
                }
            });
            return;
        }
        publishImmediately(type);
    }

    private void publishImmediately(String type) {
        log.debug("Publishing data update type={} subscribers={}", type, emitters.size());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("data-updated").data(Map.of("type", type)));
            } catch (IOException e) {
                emitters.remove(emitter);
                log.warn("Removing disconnected SSE subscriber after send failure", e);
                emitter.completeWithError(e);
            }
        }
    }
}
