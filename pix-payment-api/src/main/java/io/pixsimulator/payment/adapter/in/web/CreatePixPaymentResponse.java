package io.pixsimulator.payment.adapter.in.web;

import io.pixsimulator.payment.domain.model.PixPaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Corpo da resposta de criacao de pagamento Pix.
 *
 * Expoe apenas os dados publicos do pagamento. A {@code idempotencyKey} nao
 * e retornada.
 */
public record CreatePixPaymentResponse(
        UUID paymentId,
        PixPaymentStatus status,
        String payerKey,
        String receiverKey,
        BigDecimal amount,
        String description
) {
}
