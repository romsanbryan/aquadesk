package romsanbryan.aquadesk.services;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;

class DataUpdatePublisherTest {

    @AfterEach
    void clearTransactionState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void createsEmitterAndPublishesWithoutSubscribers() {
        DataUpdatePublisher publisher = new DataUpdatePublisher();

        assertNotNull(publisher.subscribe());
        assertDoesNotThrow(() -> publisher.publish("event"));
    }

    @Test
    void defersPublishingUntilAnActiveTransactionCommits() {
        DataUpdatePublisher publisher = new DataUpdatePublisher();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        publisher.publish("event");

        var synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());
        assertInstanceOf(TransactionSynchronization.class, synchronizations.get(0));
        assertDoesNotThrow(() -> synchronizations.get(0).afterCommit());
    }

    @Test
    void publishesImmediatelyWithoutActiveTransactionSynchronization() {
        DataUpdatePublisher publisher = new DataUpdatePublisher();

        publisher.publish("event");

        assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
        assertDoesNotThrow(() -> publisher.publish("another-event"));
    }

    @Test
    void removesSubscriberWhenInitialConnectedEventCannotBeSent() {
        IOException failure = new IOException("client disconnected during connect");
        FailingEmitter emitter = new FailingEmitter(failure, 1);
        DataUpdatePublisher publisher = new DataUpdatePublisher(() -> emitter);

        assertSame(emitter, publisher.subscribe());
        assertSame(failure, emitter.completedWithError);
        publisher.publish("event");

        assertEquals(1, emitter.sendAttempts.get());
    }

    @Test
    void removesDisconnectedSubscriberWhenPublishingAnUpdateFails() {
        IOException failure = new IOException("client disconnected during update");
        FailingEmitter emitter = new FailingEmitter(failure, 2);
        DataUpdatePublisher publisher = new DataUpdatePublisher(() -> emitter);
        publisher.subscribe();

        publisher.publish("event");
        publisher.publish("later-event");

        assertSame(failure, emitter.completedWithError);
        assertEquals(2, emitter.sendAttempts.get());
    }

    @Test
    void removesSubscribersWhenTheEmitterCompletesTimesOutOrErrors() {
        for (String callbackType : new String[]{"completion", "timeout", "error"}) {
            CallbackEmitter emitter = new CallbackEmitter();
            DataUpdatePublisher publisher = new DataUpdatePublisher(() -> emitter);
            publisher.subscribe();
            int initialAttempts = emitter.sendAttempts.get();

            switch (callbackType) {
                case "completion" -> emitter.completion.run();
                case "timeout" -> emitter.timeout.run();
                case "error" -> emitter.error.accept(new IOException("client disconnected"));
                default -> throw new AssertionError(callbackType);
            }
            publisher.publish("event");

            assertEquals(initialAttempts, emitter.sendAttempts.get(), callbackType);
        }
    }

    private static final class FailingEmitter extends SseEmitter {
        private final IOException failure;
        private final int failOnAttempt;
        private final AtomicInteger sendAttempts = new AtomicInteger();
        private Throwable completedWithError;

        private FailingEmitter(IOException failure, int failOnAttempt) {
            super(0L);
            this.failure = failure;
            this.failOnAttempt = failOnAttempt;
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (sendAttempts.incrementAndGet() == failOnAttempt) {
                throw failure;
            }
        }

        @Override
        public void completeWithError(Throwable failure) {
            completedWithError = failure;
            super.completeWithError(failure);
        }
    }

    private static final class CallbackEmitter extends SseEmitter {
        private Runnable completion;
        private Runnable timeout;
        private Consumer<Throwable> error;
        private final AtomicInteger sendAttempts = new AtomicInteger();

        private CallbackEmitter() {
            super(0L);
        }

        @Override
        public void onCompletion(Runnable callback) {
            completion = callback;
        }

        @Override
        public void onTimeout(Runnable callback) {
            timeout = callback;
        }

        @Override
        public void onError(Consumer<Throwable> callback) {
            error = callback;
        }

        @Override
        public void send(SseEventBuilder builder) {
            sendAttempts.incrementAndGet();
        }
    }
}
