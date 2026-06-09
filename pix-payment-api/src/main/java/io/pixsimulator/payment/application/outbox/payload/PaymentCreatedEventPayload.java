package io.pixsimulator.payment.application.outbox.payload;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload explicito do evento {@code PAYMENT_CREATED} (Lote 6).
 *
 * <p>Contrato estavel publicado no Kafka (ADR-028): nao serializamos a entidade
 * de dominio nem a Entity JPA diretamente. {@code eventId} e sempre igual ao
 * {@code id} do {@code OutboxEvent}, permitindo idempotencia por evento no
 * consumidor (Lote 7).
 */
public record PaymentCreatedEventPayload(
        UUID eventId,
        String eventType,
        int eventVersion,
        LocalDateTime occurredAt,
        UUID paymentId,
        String status,
        String payerKey,
        String receiverKey,
        BigDecimal amount,
        String description,
        LocalDateTime createdAt) {
}
