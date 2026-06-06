package io.pixsimulator.payment.adapter.in.web;

import io.pixsimulator.payment.domain.model.PixPaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Corpo da resposta da consulta de pagamento por id (Lote 4).
 *
 * Expoe o estado completo do ciclo de vida do pagamento. A {@code idempotencyKey}
 * nao e retornada.
 */
public record GetPixPaymentResponse(
        UUID paymentId,
        PixPaymentStatus status,
        String payerKey,
        String receiverKey,
        BigDecimal amount,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime processedAt,
        String rejectionReason
) {
}
