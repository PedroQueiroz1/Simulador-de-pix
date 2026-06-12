package io.pixsimulator.payment.application.dto;

import io.pixsimulator.payment.domain.model.PixPaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resultado do processamento simulado de um pagamento.
 *
 * Carrega apenas o desfecho da operacao: o {@code status} final
 * ({@code APPROVED} ou {@code REJECTED}), o {@code processedAt} e, em caso de
 * rejeicao, o {@code rejectionReason}.
 */
public record ProcessPixPaymentResult(
        UUID paymentId,
        PixPaymentStatus status,
        LocalDateTime processedAt,
        String rejectionReason
) {
}
