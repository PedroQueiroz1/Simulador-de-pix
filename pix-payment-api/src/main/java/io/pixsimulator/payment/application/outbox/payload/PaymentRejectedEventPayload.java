package io.pixsimulator.payment.application.outbox.payload;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload explicito do evento {@code PAYMENT_REJECTED} (Lote 6).
 *
 * <p>Carrega o {@code rejectionReason} (motivo simulado da recusa). Pagamento
 * rejeitado nao movimenta valor: nao ha Ledger nem {@code ledgerTransactionId}.
 * {@code eventId} e igual ao {@code id} do {@code OutboxEvent}.
 */
public record PaymentRejectedEventPayload(
        UUID eventId,
        String eventType,
        int eventVersion,
        LocalDateTime occurredAt,
        UUID paymentId,
        String status,
        String payerKey,
        String receiverKey,
        BigDecimal amount,
        LocalDateTime processedAt,
        String rejectionReason) {
}
