package io.pixsimulator.payment.adapter.out.persistence.jpa.outbox.mapper;

import io.pixsimulator.payment.adapter.out.persistence.jpa.outbox.entity.OutboxEventEntity;
import io.pixsimulator.payment.application.outbox.OutboxEvent;
import io.pixsimulator.payment.application.outbox.OutboxEventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Testes do {@link OutboxEventJpaMapper}.
 *
 * <p>Garante a conversao fiel aplicacao &harr; entity nos dois sentidos,
 * incluindo a traducao do enum {@link OutboxEventStatus} &harr; {@code String} e
 * a preservacao de payload, attempts e timestamps.
 */
class OutboxEventJpaMapperTest {

    private static final UUID ID = UUID.fromString("0197a0e8-6e3f-7e2f-b3a5-bce754c21a19");
    private static final UUID AGGREGATE_ID = UUID.fromString("01977a97-3c1f-7b48-a4e6-2b8d4e7b0b41");
    private static final String PAYLOAD = "{\"eventId\":\"0197a0e8-6e3f-7e2f-b3a5-bce754c21a19\",\"eventType\":\"PAYMENT_CREATED\"}";
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 6, 8, 10, 30, 0);
    private static final LocalDateTime AVAILABLE_AT = LocalDateTime.of(2026, 6, 8, 10, 30, 5);
    private static final LocalDateTime PUBLISHED_AT = LocalDateTime.of(2026, 6, 8, 10, 31, 0);

    @Test
    @DisplayName("Deve converter OutboxEvent (aplicacao) para Entity, preservando status, payload, attempts e timestamps")
    void shouldMapDomainToEntity() {
        OutboxEvent event = OutboxEvent.restore(
                ID, "PAYMENT", AGGREGATE_ID, "PAYMENT_APPROVED", 1,
                "pix.payment.events", AGGREGATE_ID.toString(), PAYLOAD,
                OutboxEventStatus.PUBLISHED, 2, "previous error",
                CREATED_AT, PUBLISHED_AT, AVAILABLE_AT);

        OutboxEventEntity entity = OutboxEventJpaMapper.toEntity(event);

        assertEquals(ID, entity.getId());
        assertEquals("PAYMENT", entity.getAggregateType());
        assertEquals(AGGREGATE_ID, entity.getAggregateId());
        assertEquals("PAYMENT_APPROVED", entity.getEventType());
        assertEquals(1, entity.getEventVersion());
        assertEquals("pix.payment.events", entity.getTopic());
        assertEquals(AGGREGATE_ID.toString(), entity.getPartitionKey());
        assertEquals(PAYLOAD, entity.getPayload());
        // Enum vira nome (String).
        assertEquals("PUBLISHED", entity.getStatus());
        assertEquals(2, entity.getAttempts());
        assertEquals("previous error", entity.getLastError());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(PUBLISHED_AT, entity.getPublishedAt());
        assertEquals(AVAILABLE_AT, entity.getAvailableAt());
    }

    @Test
    @DisplayName("Deve converter Entity para OutboxEvent (aplicacao), preservando status, payload, attempts e timestamps")
    void shouldMapEntityToDomain() {
        OutboxEventEntity entity = new OutboxEventEntity(
                ID, "PAYMENT", AGGREGATE_ID, "PAYMENT_REJECTED", 1,
                "pix.payment.events", AGGREGATE_ID.toString(), PAYLOAD,
                "FAILED", 5, "kafka down",
                CREATED_AT, null, AVAILABLE_AT);

        OutboxEvent event = OutboxEventJpaMapper.toDomain(entity);

        assertEquals(ID, event.getId());
        assertEquals("PAYMENT", event.getAggregateType());
        assertEquals(AGGREGATE_ID, event.getAggregateId());
        assertEquals("PAYMENT_REJECTED", event.getEventType());
        assertEquals(1, event.getEventVersion());
        assertEquals("pix.payment.events", event.getTopic());
        assertEquals(AGGREGATE_ID.toString(), event.getPartitionKey());
        assertEquals(PAYLOAD, event.getPayload());
        // String vira enum.
        assertEquals(OutboxEventStatus.FAILED, event.getStatus());
        assertEquals(5, event.getAttempts());
        assertEquals("kafka down", event.getLastError());
        assertEquals(CREATED_AT, event.getCreatedAt());
        assertNull(event.getPublishedAt());
        assertEquals(AVAILABLE_AT, event.getAvailableAt());
    }

    @Test
    @DisplayName("Round-trip aplicacao -> entity -> aplicacao deve preservar todos os campos")
    void shouldPreserveFieldsOnRoundTrip() {
        OutboxEvent original = OutboxEvent.restore(
                ID, "PAYMENT", AGGREGATE_ID, "PAYMENT_CREATED", 1,
                "pix.payment.events", AGGREGATE_ID.toString(), PAYLOAD,
                OutboxEventStatus.PENDING, 0, null,
                CREATED_AT, null, CREATED_AT);

        OutboxEvent roundTrip = OutboxEventJpaMapper.toDomain(OutboxEventJpaMapper.toEntity(original));

        assertEquals(original.getId(), roundTrip.getId());
        assertEquals(original.getStatus(), roundTrip.getStatus());
        assertEquals(original.getPayload(), roundTrip.getPayload());
        assertEquals(original.getAttempts(), roundTrip.getAttempts());
        assertEquals(original.getCreatedAt(), roundTrip.getCreatedAt());
        assertEquals(original.getAvailableAt(), roundTrip.getAvailableAt());
        assertNull(roundTrip.getPublishedAt());
    }
}
