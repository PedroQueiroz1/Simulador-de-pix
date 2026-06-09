package io.pixsimulator.payment.application.outbox.payload;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload explicito do evento {@code PAYMENT_APPROVED} (Lote 6).
 *
 * <p>Carrega o {@code ledgerTransactionId} porque o Ledger e criado na MESMA
 * transacao da aprovacao (spec 028): o consumidor sabe qual lancamento
 * financeiro corresponde ao pagamento aprovado, sem precisar de um evento
 * separado de Ledger neste lote. {@code eventId} e igual ao {@code id} do
 * {@code OutboxEvent}.
 */
public record PaymentApprovedEventPayload(
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
        UUID ledgerTransactionId) {
}
