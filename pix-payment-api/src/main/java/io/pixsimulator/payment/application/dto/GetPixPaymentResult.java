package io.pixsimulator.payment.application.dto;

import io.pixsimulator.payment.domain.model.PixPaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resultado da consulta de pagamento por id (Lote 4).
 *
 * Diferente do resultado de criacao, expoe o estado completo do ciclo de vida
 * ({@code status}, {@code updatedAt}, {@code processedAt}, {@code rejectionReason}).
 * A {@code idempotencyKey} continua nao sendo exposta.
 */
public record GetPixPaymentResult(
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
