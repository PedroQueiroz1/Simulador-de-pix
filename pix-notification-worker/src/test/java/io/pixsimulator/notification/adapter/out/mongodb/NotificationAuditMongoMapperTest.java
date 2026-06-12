package io.pixsimulator.notification.adapter.out.mongodb;

import io.pixsimulator.notification.domain.NotificationAudit;
import io.pixsimulator.notification.domain.NotificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Testes do {@link NotificationAuditMongoMapper}.
 *
 * <p>Garante a conversao fiel dominio &harr; documento nos dois sentidos,
 * incluindo a traducao {@link NotificationStatus} &harr; {@code String} e a
 * preservacao de {@code eventId}, {@code eventType}, {@code paymentId},
 * {@code rawPayload} e {@code notificationStatus}.
 */
class NotificationAuditMongoMapperTest {

    private static final UUID EVENT_ID = UUID.fromString("0197a0e8-6e3f-7e2f-b3a5-bce754c21a19");
    private static final UUID PAYMENT_ID = UUID.fromString("01977a97-3c1f-7b48-a4e6-2b8d4e7b0b41");
    private static final String CORRELATION_ID = "11111111-2222-3333-4444-555555555555";
    private static final String RAW_PAYLOAD = "{\"eventId\":\"0197a0e8-6e3f-7e2f-b3a5-bce754c21a19\"}";
    private static final LocalDateTime RECEIVED_AT = LocalDateTime.of(2026, 6, 9, 10, 30, 0);
    private static final LocalDateTime PROCESSED_AT = LocalDateTime.of(2026, 6, 9, 10, 30, 1);

    @Test
    @DisplayName("Deve converter dominio (PROCESSED) para documento, preservando todos os campos")
    void shouldMapDomainToDocument() {
        NotificationAudit audit = NotificationAudit.processed(
                EVENT_ID, "PAYMENT_CREATED", 1, PAYMENT_ID, CORRELATION_ID,
                "Notificacao simulada", RAW_PAYLOAD, RECEIVED_AT, PROCESSED_AT);

        NotificationAuditDocument document = NotificationAuditMongoMapper.toDocument(audit);

        assertNull(document.getId());
        assertEquals(EVENT_ID, document.getEventId());
        assertEquals("PAYMENT_CREATED", document.getEventType());
        assertEquals(1, document.getEventVersion());
        assertEquals(PAYMENT_ID, document.getPaymentId());
        assertEquals(CORRELATION_ID, document.getCorrelationId());
        assertEquals("PROCESSED", document.getNotificationStatus());
        assertEquals("Notificacao simulada", document.getNotificationMessage());
        assertEquals(RAW_PAYLOAD, document.getRawPayload());
        assertNull(document.getErrorMessage());
        assertEquals(RECEIVED_AT, document.getReceivedAt());
        assertEquals(PROCESSED_AT, document.getProcessedAt());
    }

    @Test
    @DisplayName("Deve converter documento (FAILED) para dominio, preservando status e errorMessage")
    void shouldMapDocumentToDomain() {
        NotificationAuditDocument document = new NotificationAuditDocument(
                "mongo-generated-id", EVENT_ID, "PAYMENT_APPROVED", 1, PAYMENT_ID, CORRELATION_ID,
                "FAILED", null, RAW_PAYLOAD, "Unsupported eventVersion: 2",
                RECEIVED_AT, PROCESSED_AT);

        NotificationAudit audit = NotificationAuditMongoMapper.toDomain(document);

        assertEquals("mongo-generated-id", audit.getId());
        assertEquals(EVENT_ID, audit.getEventId());
        assertEquals("PAYMENT_APPROVED", audit.getEventType());
        assertEquals(PAYMENT_ID, audit.getPaymentId());
        assertEquals(CORRELATION_ID, audit.getCorrelationId());
        assertEquals(NotificationStatus.FAILED, audit.getNotificationStatus());
        assertEquals(RAW_PAYLOAD, audit.getRawPayload());
        assertEquals("Unsupported eventVersion: 2", audit.getErrorMessage());
    }

    @Test
    @DisplayName("Round-trip dominio -> documento -> dominio deve preservar eventId, eventType, paymentId, correlationId, rawPayload e status")
    void shouldPreserveFieldsOnRoundTrip() {
        NotificationAudit original = NotificationAudit.processed(
                EVENT_ID, "PAYMENT_REJECTED", 1, PAYMENT_ID, CORRELATION_ID,
                "Notificacao simulada", RAW_PAYLOAD, RECEIVED_AT, PROCESSED_AT);

        NotificationAudit roundTrip =
                NotificationAuditMongoMapper.toDomain(NotificationAuditMongoMapper.toDocument(original));

        assertEquals(original.getEventId(), roundTrip.getEventId());
        assertEquals(original.getEventType(), roundTrip.getEventType());
        assertEquals(original.getPaymentId(), roundTrip.getPaymentId());
        assertEquals(original.getCorrelationId(), roundTrip.getCorrelationId());
        assertEquals(original.getRawPayload(), roundTrip.getRawPayload());
        assertEquals(original.getNotificationStatus(), roundTrip.getNotificationStatus());
    }
}
