package io.pixsimulator.payment.application.dto;

import io.pixsimulator.payment.domain.model.PixPaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Resultado do caso de uso de criacao de pagamento.
 *
 * Carrega apenas o que o adapter web precisa para montar a resposta HTTP.
 * Note que a {@code idempotencyKey} nao e exposta no resultado.
 */
public record CreatePixPaymentResult(
        UUID paymentId,
        PixPaymentStatus status,
        String payerKey,
        String receiverKey,
        BigDecimal amount,
        String description
) {
}
