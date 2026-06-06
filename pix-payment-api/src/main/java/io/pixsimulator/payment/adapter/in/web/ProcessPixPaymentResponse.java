package io.pixsimulator.payment.adapter.in.web;

import io.pixsimulator.payment.domain.model.PixPaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Corpo da resposta do processamento simulado de pagamento (Lote 4).
 *
 * Expoe apenas o desfecho: status final, {@code processedAt} e, em caso de
 * rejeicao, o {@code rejectionReason}.
 */
public record ProcessPixPaymentResponse(
        UUID paymentId,
        PixPaymentStatus status,
        LocalDateTime processedAt,
        String rejectionReason
) {
}
