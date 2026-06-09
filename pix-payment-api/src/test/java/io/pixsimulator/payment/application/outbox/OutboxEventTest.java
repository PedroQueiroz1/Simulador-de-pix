package io.pixsimulator.payment.application.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes do modelo de aplicacao {@link OutboxEvent} (Lote 6).
 *
 * <p>Cobrem a criacao de um evento PENDING valido, as transicoes de publicacao
 * ({@code markPublished}) e de falha ({@code registerFailedAttempt}, incluindo o
 * limite de tentativas) e as validacoes de campos obrigatorios.
 */
class OutboxEventTest {

    private static final UUID EVENT_ID = UUID.fromString("0197a0e8-6e3f-7e2f-b3a5-bce754c21a19");
    private static final UUID AGGREGATE_ID = UUID.fromString("0197a0e8-7000-7e2f-b3a5-bce754c21abc");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 8, 10, 30, 0);
    private static final String TOPIC = "pix.payment.events";
    private static final String PAYLOAD = "{\"eventId\":\"x\"}";

    private OutboxEvent validEvent() {
        return OutboxEvent.create(
                EVENT_ID,
                "PAYMENT",
                AGGREGATE_ID,
                "PAYMENT_CREATED",
                1,
                TOPIC,
                AGGREGATE_ID.toString(),
                PAYLOAD,
                NOW);
    }

    @Test
    @DisplayName("Deve criar evento PENDING valido")
    void shouldCreateValidPendingEvent() {
        OutboxEvent event = validEvent();

        assertEquals(EVENT_ID, event.getId());
        assertEquals("PAYMENT", event.getAggregateType());
        assertEquals(AGGREGATE_ID, event.getAggregateId());
        assertEquals("PAYMENT_CREATED", event.getEventType());
        assertEquals(1, event.getEventVersion());
        assertEquals(TOPIC, event.getTopic());
        assertEquals(AGGREGATE_ID.toString(), event.getPartitionKey());
        assertEquals(PAYLOAD, event.getPayload());
        assertEquals(OutboxEventStatus.PENDING, event.getStatus());
        assertEquals(0, event.getAttempts());
        assertNull(event.getLastError());
        assertEquals(NOW, event.getCreatedAt());
        assertEquals(NOW, event.getAvailableAt());
        assertNull(event.getPublishedAt());
    }

    @Test
    @DisplayName("Deve marcar evento como PUBLISHED")
    void shouldMarkAsPublished() {
        OutboxEvent event = validEvent();
        LocalDateTime publishedAt = NOW.plusSeconds(5);

        event.markPublished(publishedAt);

        assertEquals(OutboxEventStatus.PUBLISHED, event.getStatus());
        assertEquals(publishedAt, event.getPublishedAt());
    }

    @Test
    @DisplayName("Deve registrar falha e incrementar attempts (mantendo PENDING)")
    void shouldRegisterFailureAndIncrementAttempts() {
        OutboxEvent event = validEvent();
        LocalDateTime nextAvailableAt = NOW.plusSeconds(5);

        event.registerFailedAttempt("kafka timeout", nextAvailableAt, 5);

        assertEquals(1, event.getAttempts());
        assertEquals("kafka timeout", event.getLastError());
        assertEquals(nextAvailableAt, event.getAvailableAt());
        // Ainda ha tentativas: permanece PENDING para retry.
        assertEquals(OutboxEventStatus.PENDING, event.getStatus());
    }

    @Test
    @DisplayName("Deve marcar como FAILED ao atingir max attempts")
    void shouldMarkAsFailedWhenMaxAttemptsReached() {
        OutboxEvent event = validEvent();
        int maxAttempts = 3;

        event.registerFailedAttempt("err 1", NOW.plusSeconds(5), maxAttempts);
        assertEquals(OutboxEventStatus.PENDING, event.getStatus());

        event.registerFailedAttempt("err 2", NOW.plusSeconds(10), maxAttempts);
        assertEquals(OutboxEventStatus.PENDING, event.getStatus());

        event.registerFailedAttempt("err 3", NOW.plusSeconds(15), maxAttempts);
        assertEquals(OutboxEventStatus.FAILED, event.getStatus());
        assertEquals(3, event.getAttempts());
        assertEquals("err 3", event.getLastError());
    }

    @Test
    @DisplayName("Deve truncar lastError em 1000 caracteres")
    void shouldTruncateLastError() {
        OutboxEvent event = validEvent();
        String longError = "x".repeat(5000);

        event.registerFailedAttempt(longError, NOW.plusSeconds(5), 5);

        assertEquals(1000, event.getLastError().length());
    }

    @Test
    @DisplayName("Deve rejeitar payload vazio")
    void shouldRejectBlankPayload() {
        assertThrows(IllegalArgumentException.class, () -> OutboxEvent.create(
                EVENT_ID, "PAYMENT", AGGREGATE_ID, "PAYMENT_CREATED", 1,
                TOPIC, AGGREGATE_ID.toString(), "   ", NOW));
    }

    @Test
    @DisplayName("Deve rejeitar topic vazio")
    void shouldRejectBlankTopic() {
        assertThrows(IllegalArgumentException.class, () -> OutboxEvent.create(
                EVENT_ID, "PAYMENT", AGGREGATE_ID, "PAYMENT_CREATED", 1,
                "", AGGREGATE_ID.toString(), PAYLOAD, NOW));
    }

    @Test
    @DisplayName("Deve rejeitar eventType vazio")
    void shouldRejectBlankEventType() {
        assertThrows(IllegalArgumentException.class, () -> OutboxEvent.create(
                EVENT_ID, "PAYMENT", AGGREGATE_ID, "  ", 1,
                TOPIC, AGGREGATE_ID.toString(), PAYLOAD, NOW));
    }
}
