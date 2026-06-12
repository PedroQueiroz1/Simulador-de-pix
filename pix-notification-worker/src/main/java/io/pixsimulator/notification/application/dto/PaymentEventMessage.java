package io.pixsimulator.notification.application.dto;

import io.pixsimulator.notification.domain.NotificationEventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de pagamento ja parseado e validado.
 *
 * <p>Representacao tipada do JSON recebido do Kafka, depois de o handler validar
 * os campos comuns, a versao ({@code = 1}), o tipo conhecido e os campos
 * obrigatorios especificos de cada tipo. Reune os campos comuns e a uniao dos
 * campos especificos:
 * <ul>
 *   <li>{@code PAYMENT_CREATED}: {@code description}, {@code createdAt};</li>
 *   <li>{@code PAYMENT_APPROVED}: {@code processedAt}, {@code ledgerTransactionId}
 *       (obrigatorio);</li>
 *   <li>{@code PAYMENT_REJECTED}: {@code processedAt}, {@code rejectionReason}
 *       (obrigatorio).</li>
 * </ul>
 * Campos nao aplicaveis ao tipo do evento chegam nulos.
 *
 * <p>Inclui o {@code correlationId} propagado pelo {@code pix-payment-api}
 * no payload Kafka. E opcional (pode chegar nulo), colocado no MDC durante o
 * processamento e gravado na auditoria do MongoDB.
 */
public record PaymentEventMessage(
        UUID eventId,
        NotificationEventType eventType,
        int eventVersion,
        LocalDateTime occurredAt,
        UUID paymentId,
        String status,
        String payerKey,
        String receiverKey,
        BigDecimal amount,
        String description,
        LocalDateTime createdAt,
        LocalDateTime processedAt,
        UUID ledgerTransactionId,
        String rejectionReason,
        String correlationId) {
}
